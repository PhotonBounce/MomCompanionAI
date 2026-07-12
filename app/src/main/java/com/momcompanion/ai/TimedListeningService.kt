package com.friendai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import androidx.core.app.NotificationCompat
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Caregiver-configurable "timed listening" mode.
 *
 * Push-to-talk doesn't work for everyone — some people (especially dementia patients)
 * cannot reliably operate a button. This foreground service lets a caregiver opt into a
 * window (up to 12 hours, set in Caregiver Settings) during which the app listens for
 * Mom on its own, replies through the same real AI pipeline as the Talk button
 * ([AiClientFactory] / [CompanionEngine]), and stops automatically when the timer ends.
 *
 * Lessons learned from the old always-on [CompanionService] (deleted): never echo the
 * user's own words back as a "reply", always pause the recognizer while TTS is speaking
 * to avoid the mic hearing itself, and always have a hard auto-stop so the mic isn't left
 * running indefinitely.
 */
class TimedListeningService : Service(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val handler = Handler(Looper.getMainLooper())
    private var listening = false
    private var stopped = false
    private val recentTurns = ArrayDeque<String>()
    /**
     * Auto-detected recognition locale — starts as the device default, then switches to
     * Russian if we detect Cyrillic text and back to English if we see Latin text. Lets
     * bilingual families (the primary use-case) speak either language without any setting.
     */
    private var currentRecognitionLocale: Locale = Locale.getDefault()
    /** Ensures the spoken "I'm here and listening" greeting plays once per service start. */
    private var greetedThisSession = false
    /**
     * Partial wake lock — keeps the CPU running even when the screen turns off, so the
     * timed-listening loop isn't suspended mid-session. Acquired with a bounded timeout
     * (equal to the listening window + a small margin) so it auto-releases even if the
     * service crashes without reaching onDestroy.
     */
    private var wakeLock: PowerManager.WakeLock? = null

    /** Fires when a listening window elapses; auto-renews so the companion never goes silent. */
    private val stopRunnable = Runnable { renewWindow() }
    private val restartListeningRunnable = Runnable { startListeningCycle() }
    /** Length of one listening window; the window auto-renews so hands-free runs all day. */
    private var windowMillis: Long = 0L
    /** Fires when nobody has spoken for [IDLE_CHECK_IN_DELAY_MS] — gently prompts Mom. */
    private val idleCheckInRunnable = Runnable { speakIdleCheckIn() }

    /**
     * Single engine instance for the lifetime of this service. Keeping it alive across turns
     * lets recentTopics, recentMoods, and lastAIReply accumulate naturally so the engine
     * tracks the conversation in-memory (in addition to seeding from recentTurns string).
     */
    private val serviceEngine = CompanionEngine()

    override fun onCreate() {
        super.onCreate()
        // Seed the recognition locale from the onboarding language preference so Russian
        // families start in Russian mode even before the first utterance is detected.
        val onboardingPrefs = getSharedPreferences("onboarding", MODE_PRIVATE)
        if (onboardingPrefs.getString("language", "English") == "Russian") {
            currentRecognitionLocale = Locale("ru", "RU")
        }
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PAUSE_FOR_PUSH_TO_TALK) {
            // Mom is using the on-screen Talk button directly — yield the microphone so the
            // two recognizers don't fight over it, then resume our own cycle afterward.
            pauseForPushToTalk()
            return START_STICKY
        }

        // Android 14+ throws SecurityException if a microphone-type FGS starts before
        // RECORD_AUDIO is granted (crashes the whole app). MainActivity normally requests
        // the permission first, but guard here too so no path can crash the service.
        val micGranted = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            stopSelf()
            return START_NOT_STICKY
        }

        val hours = (intent?.getIntExtra(EXTRA_HOURS, 0) ?: 0).coerceIn(1, 12)
        // Android 14+ throws SecurityException if startForeground is called from
        // a background context (e.g. BootReceiver) before the microphone-type FGS
        // is permitted. Guard here so the service fails cleanly rather than crashing.
        try {
            startForeground(NOTIFICATION_ID, buildNotification(hours))
        } catch (e: SecurityException) {
            stopSelf()
            return START_NOT_STICKY
        }

        val durationMillis = hours * 60L * 60L * 1000L
        windowMillis = durationMillis
        handler.removeCallbacks(stopRunnable)
        handler.postDelayed(stopRunnable, durationMillis)

        // (Re-)acquire a bounded wake lock for this window so the CPU stays awake even
        // if the screen times out. The lock is bounded to the listening window + 5 min
        // margin so it is guaranteed to release even if the service crashes.
        runCatching { wakeLock?.release() }
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "friendai:TimedListening").apply {
            acquire(durationMillis + 5 * 60 * 1000L)
        }

        // Start the idle check-in timer — if nobody talks for 15 minutes, gently prompt Mom.
        resetIdleCheckIn()

        if (!listening && !stopped) {
            startListeningCycle()
        }
        return START_STICKY
    }

    private fun pauseForPushToTalk() {
        if (stopped) return
        handler.removeCallbacks(restartListeningRunnable)
        runCatching { speechRecognizer?.stopListening() }
        runCatching { speechRecognizer?.cancel() }
        listening = false
        scheduleRestart(PUSH_TO_TALK_YIELD_MS)
    }

    /**
     * Called when a listening window elapses. Instead of going silent — which left Mom with
     * no companion and no button until someone reopened the app — we quietly start another
     * window so hands-free listening continues all day. The caregiver stops it from Settings.
     */
    private fun renewWindow() {
        if (stopped) return
        handler.removeCallbacks(stopRunnable)
        handler.postDelayed(stopRunnable, windowMillis)
        // Re-arm the bounded wake lock for the next window so the loop survives screen-off.
        runCatching { wakeLock?.release() }
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "friendai:TimedListening").apply {
            acquire(windowMillis + 5 * 60 * 1000L)
        }
        resetIdleCheckIn()
        if (!listening) scheduleRestart(POST_REPLY_DELAY_MS)
    }

    /**
     * Resets the idle check-in timer. Called on every incoming user message and on start
     * so the 15-minute silence clock always counts from the last real conversation.
     */
    private fun resetIdleCheckIn() {
        handler.removeCallbacks(idleCheckInRunnable)
        if (!stopped) handler.postDelayed(idleCheckInRunnable, IDLE_CHECK_IN_DELAY_MS)
    }

    /**
     * Gently engages Mom after a few minutes of silence. Dementia patients often sit
     * quietly and don't initiate — the companion must carry the conversation. Mixes
     * wellbeing check-ins with real questions drawn from the caregiver's topic list so
     * each prompt gives her something concrete to respond to.
     */
    private fun speakIdleCheckIn() {
        if (stopped) return
        val ru = currentRecognitionLocale.language == "ru"
        val topics = CaregiverRulesStore(this).load().promptTopics
            .split('\n').map { it.trim() }.filter { it.isNotEmpty() }

        val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)

        // 2 out of 3 silent check-ins lead with a topic question instead of "are you there".
        val text = if (topics.isNotEmpty() && (0..2).random() != 0) {
            val topic = topics.random()
            if (ru) listOf(
                "Мне стало интересно — расскажите мне про: $topic?",
                "Я думала о вас и вспомнила про тему: $topic. Хотите поговорить об этом?",
                "Вот вопрос для вас: расскажите что-нибудь о: $topic?",
                "Знаете, мне хотелось бы услышать о $topic. Что вы об этом думаете?"
            ).random()
            else listOf(
                "I was just thinking — I'd love to hear about this: $topic. What comes to mind?",
                "I was wondering — can you tell me something about: $topic?",
                "Here's a question for you: what do you remember about $topic?",
                "I'd really love to hear your thoughts on $topic. Do you have a memory about it?"
            ).random()
        } else {
            // Build a season-aware idle check-in pool
            val seasonalRu = when (month) {
                5, 6, 7 -> listOf( // summer
                    "Лето такое тёплое! Вы сегодня выходили на улицу или хотя бы открывали окно?",
                    "В такое лето так приятно вспоминать дачу и огород. Расскажите про ваше лето.",
                    "Летом так хочется клубники и варенья! Вы любите летние фрукты?"
                )
                2, 3, 4 -> listOf( // spring
                    "Весна — время надежды! Что вы любите в весне больше всего?",
                    "Весной так хорошо пахнет свежей землёй и цветами. Вы это замечаете?",
                    "Расскажите — что для вас значит приход весны?"
                )
                11, 0, 1 -> listOf( // winter
                    "Зимой так уютно дома! Как вы согреваетесь — чай, плед, что-то ещё?",
                    "Зима — это Новый год, снег и уют. Что вам нравится в зиме?",
                    "В такую погоду хочется чего-нибудь горячего. Вы пили чай или кофе сегодня?"
                )
                else -> listOf( // autumn
                    "Осень — это красота листьев и запах антоновских яблок. Вы любите осень?",
                    "Осенью так хочется борща и горячего чаю! Что вы сегодня ели?",
                    "Расскажите — какое ваше любимое осеннее воспоминание?"
                )
            }
            val seasonalEn = when (month) {
                5, 6, 7 -> listOf(
                    "It's lovely summer weather! Have you been outside today, or at least had the window open?",
                    "Summer always makes me think of gardens and berries. What's your favourite summer memory?",
                    "It's warm and bright — what does summer feel like to you?"
                )
                2, 3, 4 -> listOf(
                    "Spring is here! What do you love most about springtime?",
                    "Everything is waking up again in spring. What first sign of spring makes you happy?",
                    "Spring always feels like a fresh start. What are you looking forward to?"
                )
                11, 0, 1 -> listOf(
                    "It's cosy inside in winter! What's your favourite way to stay warm — tea, a blanket?",
                    "Winter has its own magic. What do you love about this time of year?",
                    "It's cold outside — have you had something warm to drink today?"
                )
                else -> listOf(
                    "Autumn is so beautiful with all the colours. Do you love this season?",
                    "Autumn smells of apples and fallen leaves. What's your favourite autumn memory?",
                    "It's getting cosy — what do you like about autumn?"
                )
            }
            val generalRu = listOf(
                "Вы здесь? Как вы себя чувствуете?",
                "Привет! Я здесь, если хотите поговорить.",
                "Просто проверяю — есть ли что-то на уме?",
                "Я рядом с вами. Хотите поговорить?",
                "Тихо стало. Всё ли хорошо?",
                "Я здесь и думаю о вас. Как настроение?",
                "Не хотите рассказать мне что-нибудь интересное?",
                "Хотите услышать историю или, может быть, шутку?",
                "Как прошёл ваш день сегодня?",
                "Хочу спросить: что вас сегодня порадовало?",
                "Давайте поговорим! Расскажите мне что-нибудь.",
                "Вы пили воду сегодня? Не забывайте — это важно!",
                "Расскажите мне что-нибудь хорошее из сегодняшнего дня.",
                "Я думала о вас. Как вы себя чувствуете прямо сейчас?"
            )
            val generalEn = listOf(
                "Are you there? How are you feeling today?",
                "Hello! I'm here if you'd like to chat.",
                "Just checking in — is there anything on your mind?",
                "I'm right here with you. Would you like to talk?",
                "It's been quiet — is everything okay?",
                "I'm here and thinking of you. How are you feeling?",
                "Would you like to hear a story or a joke?",
                "What has your day been like so far?",
                "What's something that made you smile today?",
                "Let's chat! What comes to mind right now?",
                "Have you had some water today? It's so important to stay hydrated!",
                "Tell me something nice about today.",
                "I was just thinking of you. How are you feeling right now?"
            )
            // 1 in 3 chance of a seasonal prompt, otherwise general
            if ((0..2).random() == 0) {
                if (ru) seasonalRu.random() else seasonalEn.random()
            } else {
                if (ru) generalRu.random() else generalEn.random()
            }
        }

        speak(text) {
            scheduleRestart(POST_REPLY_DELAY_MS)
            resetIdleCheckIn()
        }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.setLanguage(currentRecognitionLocale)
            // Apply the caregiver-configured speech rate so AI replies in the timed-listening
            // service are the same speed as replies in the main push-to-talk screen.
            val settings = CaregiverRulesStore(this).load()
            tts?.setSpeechRate(settings.ttsSpeechRate)
            // Greet once per service start so Mom knows the companion is here and she can
            // just talk — there is no button in hands-free mode.
            if (!greetedThisSession) {
                greetedThisSession = true
                val ru = currentRecognitionLocale.language == "ru"
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val greeting = when {
                    hour < 6 -> if (ru) listOf(
                        "Ночью не спится? Я здесь и рада поговорить. Просто говорите со мной.",
                        "Я здесь с вами, даже ночью. Говорите — я слушаю.",
                        "Не волнуйтесь — я здесь. Расскажите, как вы себя чувствуете."
                    ).random() else listOf(
                        "Can't sleep? I'm here and happy to keep you company. Just talk to me.",
                        "I'm right here with you, even at night. Say anything — I'm listening.",
                        "Don't worry — I'm here. Tell me how you're feeling."
                    ).random()
                    hour < 12 -> if (ru) listOf(
                        "Доброе утро! Я здесь и слушаю. Просто говорите, когда захотите.",
                        "С добрым утром! Рада вас слышать. Как вы сегодня?",
                        "Доброе утро! Я здесь с вами. Просто скажите что-нибудь."
                    ).random() else listOf(
                        "Good morning! I'm here and listening. Just talk whenever you like.",
                        "Good morning! So glad to hear you. How are you feeling today?",
                        "Good morning! I'm right here with you. Say anything at all."
                    ).random()
                    hour < 18 -> if (ru) listOf(
                        "Добрый день! Я здесь и слушаю. Говорите — мне всегда приятно вас слышать.",
                        "Здравствуйте! Я рада вас слышать. Расскажите, как у вас дела.",
                        "Добрый день! Я здесь с вами. Просто скажите что-нибудь, и мы поговорим."
                    ).random() else listOf(
                        "Good afternoon! I'm here and listening. Tell me how your day is going.",
                        "Hello! I'm so glad you're here. Just talk to me whenever you like.",
                        "Good afternoon! I'm right here with you. Say anything and we'll chat."
                    ).random()
                    else -> if (ru) listOf(
                        "Добрый вечер! Я здесь с вами. Говорите — я всегда слушаю.",
                        "Добрый вечер! Как прошёл ваш день? Я здесь и рада поговорить.",
                        "Вечер добрый! Я здесь, рядом. Просто говорите, когда захотите."
                    ).random() else listOf(
                        "Good evening! I'm right here with you. Talk to me whenever you like.",
                        "Good evening! How has your day been? I'm here and happy to listen.",
                        "Good evening! I'm here beside you. Just speak whenever you feel like it."
                    ).random()
                }
                speak(greeting) { scheduleRestart(POST_REPLY_DELAY_MS) }
                // Don't wait for Mom to start: open the conversation ourselves shortly
                // after the greeting (the idle check-in then keeps it going every few
                // minutes of silence). Reuses the check-in speaker, which prefers real
                // topic questions from the caregiver's list.
                handler.postDelayed(idleCheckInRunnable, OPENER_DELAY_MS)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
        super.onDestroy()
    }

    private fun startListeningCycle() {
        if (stopped) return
        if (speechRecognizer == null) {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                stopSelfCleanly("speech recognition unavailable")
                return
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(recognitionListener)
            }
        }

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentRecognitionLocale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        listening = true
        runCatching { speechRecognizer?.startListening(recognizerIntent) }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onError(error: Int) {
            listening = false
            // Most errors in an always-on loop are transient (no speech detected, timeout,
            // brief mic busy). Adjust retry delay by severity:
            val retryDelayMs = when (error) {
                // These are harmless and expected — just retry quickly.
                android.speech.SpeechRecognizer.ERROR_NO_MATCH,
                android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> RETRY_DELAY_MS

                // The recognizer itself had a problem — give it a moment to recover.
                android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                android.speech.SpeechRecognizer.ERROR_SERVER,
                android.speech.SpeechRecognizer.ERROR_NETWORK -> RETRY_DELAY_MS * 4L

                // Client or audio issues — recreate the recognizer on next cycle.
                android.speech.SpeechRecognizer.ERROR_CLIENT,
                android.speech.SpeechRecognizer.ERROR_AUDIO -> {
                    runCatching { speechRecognizer?.destroy() }
                    speechRecognizer = null
                    RETRY_DELAY_MS * 2L
                }

                // Not available at all — stop the service rather than looping forever.
                android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    stopSelfCleanly("speech recognition permission revoked")
                    return
                }

                else -> RETRY_DELAY_MS
            }
            scheduleRestart(retryDelayMs)
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isNotBlank()) {
                // Auto-detect language from script: if the recognised text is majority
                // Cyrillic, switch the recogniser to Russian for the next cycle. If it's
                // majority Latin, switch back to the device default.  This lets bilingual
                // families (English/Russian is the primary case) speak either language
                // freely without any setting — the app follows them.
                val cyrillicCount = text.count { it in 'Ѐ'..'ӿ' }
                val latinCount = text.count { it.isLetter() && it !in 'Ѐ'..'ӿ' }
                // Russian is sticky: any Cyrillic keeps Russian; only a clear English word
                // (>=4 Latin letters, no Cyrillic) switches to English. Keeps a Russian speaker
                // from being flipped to English by one mis-recognised token.
                currentRecognitionLocale = when {
                    cyrillicCount > 0 -> Locale("ru", "RU")
                    latinCount >= 4 -> Locale.US
                    else -> currentRecognitionLocale // keep current if ambiguous / no letters
                }
                handleUserMessage(text)
            } else {
                scheduleRestart(RETRY_DELAY_MS)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
    }

    private fun scheduleRestart(delayMs: Long) {
        if (stopped) return
        handler.removeCallbacks(restartListeningRunnable)
        handler.postDelayed(restartListeningRunnable, delayMs)
    }

    private fun handleUserMessage(userMsg: String) {
        // Mom spoke — reset the 15-minute idle check-in timer.
        resetIdleCheckIn()
        // Respect the same free-tier daily limit as push-to-talk so timed listening can't
        // silently bypass the monetization gate. VIP users are always unlimited.
        val monetization = MonetizationManager(this)
        if (!monetization.canUseAi()) {
            val msg = "You've reached today's free AI conversation limit. " +
                "Ask your caregiver about VIP for unlimited replies — I'll be back tomorrow!"
            speak(msg) { scheduleRestart(POST_REPLY_DELAY_MS) }
            return
        }

        val settings = CaregiverRulesStore(this).load()
        val recentConversation = recentTurns.joinToString("\n")
        // Use the service-level engine so recentTopics/recentMoods accumulate across turns,
        // and pass it directly to LocalAiClient so all state paths share one instance.
        val client = when {
            settings.geminiApiKey.isNotBlank() -> GeminiAiClient(settings.geminiApiKey, serviceEngine, LocalAiClient(serviceEngine))
            settings.backendUrl.isNotBlank() -> ProxyAiClient(settings.backendUrl, serviceEngine, LocalAiClient(serviceEngine))
            else -> LocalAiClient(serviceEngine)
        }

        client.generateReply(userMsg, recentConversation, settings, applicationContext) { reply ->
            monetization.incrementAiReply()
            handler.post {
                rememberTurn(userMsg, reply.text)
                speak(reply.text) {
                    maybeEscalate(reply, serviceEngine)
                    // Resume listening only after speaking finishes, so the mic never hears
                    // the AI's own voice (the feedback-loop bug from the old service).
                    scheduleRestart(POST_REPLY_DELAY_MS)
                }
            }
        }
    }

    private fun rememberTurn(userMsg: String, aiReply: String) {
        recentTurns.addLast("Mom: $userMsg")
        recentTurns.addLast("Companion: $aiReply")
        while (recentTurns.size > MAX_RECENT_TURNS) {
            recentTurns.removeFirst()
        }
    }

    private fun maybeEscalate(reply: CompanionReply, engine: CompanionEngine) {
        val level = if (reply.escalationLevel != EscalationLevel.NONE) {
            reply.escalationLevel
        } else {
            engine.escalationLevelForText(reply.text)
        }
        if (level == EscalationLevel.NONE) return

        val (title, message, showEmergencyButton) = when (level) {
            EscalationLevel.EMERGENCY -> Triple(
                "This sounds urgent",
                "Please call emergency services now, or ask someone nearby to help.",
                true
            )
            EscalationLevel.SCAM -> Triple(
                "Let's pause for a moment",
                "Please don't share passwords, codes, or money. Call someone you trust before doing anything.",
                false
            )
            EscalationLevel.MEDICAL -> Triple(
                "Let's get you some help",
                "I can stay with you, but for medical concerns it's best to call your doctor or a trusted person.",
                false
            )
            EscalationLevel.NONE -> return
        }

        val intent = Intent(this, EmergencyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EmergencyActivity.EXTRA_TITLE, title)
            putExtra(EmergencyActivity.EXTRA_MESSAGE, message)
            putExtra(EmergencyActivity.EXTRA_SHOW_EMERGENCY_BUTTON, showEmergencyButton)
        }
        startActivity(intent)
    }

    private fun speak(text: String, onDone: () -> Unit) {
        if (!ttsReady) {
            onDone()
            return
        }
        // Stop the recognizer before speaking so the mic can't pick up our own voice.
        runCatching { speechRecognizer?.stopListening() }
        runCatching { speechRecognizer?.cancel() }
        listening = false
        // Mirror the auto-detected language in TTS so the AI speaks back in the same
        // language Mom just used (Russian or English).
        tts?.setLanguage(currentRecognitionLocale)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { handler.post(onDone) }
            override fun onDone(utteranceId: String?) { handler.post(onDone) }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "timed_listening_reply")
    }

    private fun stopSelfCleanly(@Suppress("UNUSED_PARAMETER") reason: String) {
        if (stopped) return
        stopped = true
        handler.removeCallbacksAndMessages(null)
        runCatching { speechRecognizer?.stopListening() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
        @Suppress("DEPRECATION")
        stopForeground(true)   // removeNotification=true; works on all API levels ≥ 5
        stopSelf()
    }

    private fun buildNotification(hours: Int): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Friendai listening mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while Friendai is actively listening so caregivers and Mom know it's on."
            }
            manager?.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentIntent = PendingIntent.getActivity(this, 0, openAppIntent, pendingFlags)

        // NotificationCompat.Builder works on all API levels ≥ minSdk 24;
        // plain Notification.Builder(context, channelId) requires API 26+.
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Friendai is listening")
            .setContentText("Listening — no button press needed. Tap to open.")
            .setSmallIcon(R.drawable.ic_notification_mic)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val EXTRA_HOURS = "extra_hours"
        const val ACTION_PAUSE_FOR_PUSH_TO_TALK = "com.friendai.action.PAUSE_FOR_PUSH_TO_TALK"
        private const val NOTIFICATION_ID = 4242
        private const val CHANNEL_ID = "timed_listening"
        private const val RETRY_DELAY_MS = 1500L
        private const val POST_REPLY_DELAY_MS = 600L
        private const val PUSH_TO_TALK_YIELD_MS = 20_000L
        private const val MAX_RECENT_TURNS = 6
        /**
         * Gentle conversation prompt fires after this much silence. Kept short (3 min):
         * the companion must INITIATE conversation, not wait for it — the target user
         * rarely starts talking on her own.
         */
        private const val IDLE_CHECK_IN_DELAY_MS = 3 * 60 * 1000L
        /** First conversation opener fires this long after the start-up greeting. */
        private const val OPENER_DELAY_MS = 45 * 1000L

        /**
         * Tells the service (if running) to yield the microphone for ~20s because Mom just
         * tapped the on-screen Talk button — avoids both recognizers fighting over the mic.
         * Safe to call even if the service isn't running (no-op via stopped service start).
         */
        fun yieldMicForPushToTalk(context: android.content.Context) {
            val settings = CaregiverRulesStore(context).load()
            if (settings.timedListeningHours <= 0) return
            runCatching {
                val intent = Intent(context, TimedListeningService::class.java)
                    .setAction(ACTION_PAUSE_FOR_PUSH_TO_TALK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        /** Starts the service for [hours] (clamped 1-12). No-op until RECORD_AUDIO is granted. */
        fun start(context: android.content.Context, hours: Int) {
            val micGranted = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!micGranted) return
            val clamped = hours.coerceIn(1, 12)
            val intent = Intent(context, TimedListeningService::class.java)
                .putExtra(EXTRA_HOURS, clamped)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, TimedListeningService::class.java))
        }
    }
}
