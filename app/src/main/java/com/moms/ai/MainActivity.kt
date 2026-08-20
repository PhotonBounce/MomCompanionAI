
package com.friendai

import android.Manifest
import android.app.Activity

import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import android.content.Intent
import java.util.Locale

import com.friendai.CompanionEngine
import com.friendai.CaregiverRulesStore
import com.friendai.GeminiAiClient
import com.friendai.AnthropicAiClient
import com.friendai.ProxyAiClient
import com.friendai.LocalAiClient
import com.friendai.BuildConfig
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.friendai.EqualizerView
import android.content.Context
import android.app.AlertDialog
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.speech.RecognizerIntent

/**
 * Main activity for Friendai app — "Mom Mode".
 * Handles onboarding, push-to-talk voice input/output, conversation UI, and accessibility features.
 *
 * - Shows onboarding dialog on first launch
 * - Big "Talk" button drives speech recognition (push-to-talk, not always-on, per product plan)
 * - Routes every reply through [AiClientFactory] so the configured backend (OpenAI proxy) is used
 *   when available, with the local rule-based [CompanionEngine] as an offline/error fallback
 * - Displays closed captions for all speech
 * - Escalates to [EmergencyActivity] when a reply is flagged as urgent/medical/scam
 */
class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var conversationText: TextView
    private lateinit var closedCaptionText: TextView
    private lateinit var equalizerView: EqualizerView
    private lateinit var soundEffectIcon: ImageView
    private lateinit var animationIcon: ImageView
    private lateinit var talkButton: Button
    private lateinit var listeningModeBanner: TextView
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsInitTried = false
    /** Prevents the welcome greeting from replaying every time TTS re-initializes. */
    private var hasGreetedOnThisLaunch = false
    /**
     * Guards against an infinite POST_NOTIFICATIONS request loop: the permission callback
     * re-runs [applyTimedListeningPreference], which would otherwise request again forever
     * if the permission isn't granted (ANR via main-thread livelock — seen on device).
     */
    private var notificationsPermissionRequested = false
    /**
     * Auto-detected input locale for push-to-talk speech recognition. Starts as device
     * default, then mirrors the auto-bilingual logic from [TimedListeningService]: if the
     * recognised text is majority Cyrillic, switch to ru-RU for the next recognition so
     * the prompt matches the language Mom is actually speaking.
     */
    private var currentInputLocale: Locale = Locale.getDefault()
    private val REQ_CODE_SPEECH_INPUT = 1001
    private val REQ_CODE_RECORD_AUDIO_PERMISSION = 2001
    private val REQ_CODE_RECORD_AUDIO_FOR_TIMED_LISTENING = 2002
    private val REQ_CODE_NOTIFICATIONS_PERMISSION = 2003

    /** Rolling text summary of recent turns, sent to the backend for conversational memory.
     *  Kept generous so the AI can hold a long, hours-long conversation with continuity. */
    private val recentTurns = ArrayDeque<String>()
    private val MAX_RECENT_TURNS = 30

    /** Persistent engine so recentTopics/recentMoods/lastAIReply accumulate across push-to-talk turns. */
    private val activityEngine = CompanionEngine()

    /** The full on-screen conversation transcript (You / Friendai turns), shown in the card. */
    private val transcript = StringBuilder()

    /**
     * When true, the app keeps listening after each reply so a conversation flows hands-free:
     * tap Talk once, then just speak back and forth. Tap Talk again (now "Stop") to end it.
     */
    private var conversationActive = false

    /**
     * Initializes the main UI, onboarding, and voice interaction.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_stylish)

        conversationText = findViewById(R.id.conversationText)
        // Allow long AI replies to be scrolled with a finger — without this the text is
        // clipped at the card boundary and there's no way to read the rest.
        conversationText.movementMethod = android.text.method.ScrollingMovementMethod.getInstance()
        closedCaptionText = findViewById(R.id.closedCaptionText)
        equalizerView = findViewById(R.id.equalizerView)
        soundEffectIcon = findViewById(R.id.soundEffectIcon)
        animationIcon = findViewById(R.id.animationIcon)
        talkButton = findViewById(R.id.talkButton)
        listeningModeBanner = findViewById(R.id.listeningModeBanner)

        // The single most important control on this screen: tap to talk to the AI companion.
        // Vibration gives Mom tactile confirmation the button registered — especially
        // helpful for users with vision impairment or tremors who may not be sure they hit it.
        talkButton.setOnClickListener {
            runCatching {
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    (getSystemService(android.os.VibratorManager::class.java))?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(android.os.Vibrator::class.java)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        android.os.VibrationEffect.createOneShot(60, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(60)
                }
            }
            // Tap once to start a flowing conversation; tap again to stop it.
            if (conversationActive) stopConversation() else startConversation()
        }

        // Wire up Call Caregiver button for accessibility
        findViewById<android.widget.Button>(R.id.callCaregiverButton)?.setOnClickListener {
            val intent = Intent(this, EmergencyActivity::class.java)
            startActivity(intent)
        }

        // Tucked-away caregiver entry point — PIN-gated so Mom can't wander into settings.
        findViewById<android.widget.ImageButton>(R.id.caregiverSettingsButton)?.setOnClickListener {
            startActivity(Intent(this, PinActivity::class.java))
        }

        // Animate header and conversation card fade-in
        findViewById<TextView>(R.id.headerTitle)?.apply {
            alpha = 0f
            animate().alpha(1f).setDuration(900).start()
        }
        findViewById<CardView?>(R.id.conversationCard)?.apply {
            alpha = 0f
            animate().alpha(1f).setDuration(1200).setStartDelay(300).start()
        }

        showOnboardingIfNeeded(this)
        openFirstRunCaregiverSetupIfNeeded()
        showAiKeyHintIfNeeded()

        tts = TextToSpeech(this, this)
        ttsInitTried = true

        // Push-to-talk works for many people, but some — especially dementia patients —
        // can't reliably press a button. If a caregiver has opted into "timed listening"
        // (Caregiver Settings, up to 12 hours), start that background service here so the
        // app actively listens and converses on its own for the configured window.
        applyTimedListeningPreference()
    }

    override fun onResume() {
        super.onResume()
        // Re-check in case the caregiver just changed the setting in RulesActivity.
        applyTimedListeningPreference()
    }

    /**
     * Starts or stops [TimedListeningService] to match the caregiver's saved preference.
     * Requests RECORD_AUDIO (and, on Android 13+, POST_NOTIFICATIONS for the ongoing
     * "listening" notification) before starting, since the service can't request runtime
     * permissions itself.
     */
    private fun applyTimedListeningPreference() {
        if (isRunningInTest()) return
        val hours = CaregiverRulesStore(this).load().timedListeningHours
        if (hours <= 0) {
            // Caregiver explicitly chose push-to-talk fallback mode — show the Talk button.
            talkButton.visibility = android.view.View.VISIBLE
            TimedListeningService.stop(this)
            listeningModeBanner.visibility = TextView.GONE
            // Release screen-on and lock-screen-show flags when timed listening stops.
            window.clearFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            @Suppress("DEPRECATION")
            window.clearFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false)
                setTurnScreenOn(false)
            }
            return
        }
        // Hands-free mode is THE primary interaction: Mom cannot press buttons, she just
        // talks. Hide the Talk button entirely — the app listens on its own.
        talkButton.visibility = android.view.View.GONE
        // Keep the screen on so Mom can read AI replies without unlocking the device.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Show the app on the lock screen and turn the screen on when timed listening is
        // active — the whole point of the feature is that Mom doesn't need to do anything,
        // including unlocking her phone to read or hear the AI response.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQ_CODE_RECORD_AUDIO_FOR_TIMED_LISTENING
            )
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= 33 && !notificationsPermissionRequested) {
            val notifGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                notificationsPermissionRequested = true
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_CODE_NOTIFICATIONS_PERMISSION
                )
                // Don't block starting the service on the notification permission — the
                // service still works without it, the ongoing notice just won't show.
            }
        }

        requestBatteryOptimizationExemptionIfNeeded()
        TimedListeningService.start(this, hours)
        updateListeningBanner(hours)
    }

    /**
     * Prompts the caregiver (once) to exempt this app from battery optimization so Android's
     * Doze mode doesn't kill the timed-listening foreground service mid-session. Without this,
     * the service can be suspended after ~1 hour on many devices even though it's a foreground
     * service with a visible notification.
     *
     * This is permitted under Google Play policy for health/safety apps.
     */
    private fun requestBatteryOptimizationExemptionIfNeeded() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return  // already exempt

        val prefs = getSharedPreferences("onboarding", MODE_PRIVATE)
        if (prefs.getBoolean("battery_opt_asked", false)) return  // asked before, don't nag

        AlertDialog.Builder(this)
            .setTitle("Keep listening while you rest?")
            .setMessage(
                "To let Friendai keep listening during the full window the caregiver set, " +
                "please disable battery optimization for this app when prompted.\n\n" +
                "This only affects Friendai — your battery will not be drained faster during " +
                "normal use."
            )
            .setPositiveButton("Allow") { _, _ ->
                prefs.edit().putBoolean("battery_opt_asked", true).apply()
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                }
            }
            .setNegativeButton("Not now") { _, _ ->
                prefs.edit().putBoolean("battery_opt_asked", true).apply()
            }
            .show()
    }

    private fun updateListeningBanner(hours: Int) {
        if (hours > 0) {
            listeningModeBanner.visibility = TextView.VISIBLE
        } else {
            listeningModeBanner.visibility = TextView.GONE
        }
    }

    /**
     * On first launch (before a caregiver has ever saved settings), opens [RulesActivity]
     * directly in first-run mode — no PIN required yet, since the caregiver hasn't set one.
     * This is the "First launch opens setup so the caregiver can replace the default PIN and
     * contact info" flow described in the README, which previously had no code path to reach it.
     */
    private fun openFirstRunCaregiverSetupIfNeeded() {
        if (isRunningInTest()) return
        if (!CaregiverRulesStore(this).isSetupComplete()) {
            startActivity(
                Intent(this, RulesActivity::class.java)
                    .putExtra(RulesActivity.EXTRA_FIRST_RUN_SETUP, true)
            )
        }
    }

    /**
     * Shows a one-time caregiver hint when no AI key or backend URL is configured.
     * Disappears the moment the user speaks (conversationText is overwritten).
     * Meant for the caregiver reading the screen during setup, not for Mom.
     */
    private fun showAiKeyHintIfNeeded() {
        if (isRunningInTest()) return
        val settings = CaregiverRulesStore(this).load()
        if (!settings.setupComplete) return
        if (settings.geminiApiKey.isNotBlank() || settings.backendUrl.isNotBlank()) return
        val prefs = getSharedPreferences("onboarding", MODE_PRIVATE)
        if (prefs.getBoolean("ai_key_hint_shown", false)) return
        prefs.edit().putBoolean("ai_key_hint_shown", true).apply()
        // Intentionally do NOT dump setup instructions onto Mom's start screen — that turned
        // the home page into a wall of text. Caregivers add the AI key in Settings (gear icon)
        // instead; the start screen stays clean with just the conversation.
    }

    private fun isRunningInTest(): Boolean {
        return try {
            Class.forName("androidx.test.platform.app.InstrumentationRegistry")
            true  // class found → we're in a test environment
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Intercepts the back button so Mom doesn't accidentally exit the app.
     * Instead of closing, we move it to the background — the home screen appears,
     * but the app stays alive and the timed-listening service continues running.
     * A caregiver can still close the app normally from the recents screen.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Move to background rather than exiting — timed listening must keep running.
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            // Apply onboarding language preference — if the user selected Russian during
            // first-run setup, honour it as the starting locale instead of device default.
            val onboardingPrefs = getSharedPreferences("onboarding", MODE_PRIVATE)
            val onboardingLanguage = onboardingPrefs.getString("language", "English")
            if (onboardingLanguage == "Russian") {
                currentInputLocale = Locale("ru", "RU")
            }

            val result = tts?.setLanguage(currentInputLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = false
                Toast.makeText(this, "TTS language not supported. Please install TTS data.", Toast.LENGTH_LONG).show()
                conversationText.text = "TTS language not supported. Please install TTS data."
            } else {
                // Apply the caregiver-configured speech rate (slow / normal / fast).
                val settings = CaregiverRulesStore(this).load()
                tts?.setSpeechRate(settings.ttsSpeechRate)
                // Speak a gentle welcome greeting so Mom knows the app is ready —
                // even without touching the screen. Skip in timed-listening mode because
                // the service will greet her on its own after the first voice detection.
                if (!hasGreetedOnThisLaunch && settings.timedListeningHours <= 0) {
                    hasGreetedOnThisLaunch = true
                    val greeting = if (currentInputLocale.language == "ru")
                        "Привет! Я здесь. Нажмите кнопку «Говорить», когда будете готовы."
                    else
                        "Hello! I'm here. Tap the Talk button when you're ready to chat."
                    speakReply(greeting)
                }
            }
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed. Please enable TTS in device settings.", Toast.LENGTH_LONG).show()
            conversationText.text = "TTS initialization failed. Please enable TTS in device settings."
        }
    }

    /**
     * Confirms RECORD_AUDIO is granted before starting speech recognition, requesting it at
     * runtime if needed (required on Android 6.0+; speech recognition silently fails without it).
     */
    private fun isRu() = currentInputLocale.language == "ru"

    /** Start a flowing conversation: listen, reply, then listen again until stopped. */
    private fun startConversation() {
        conversationActive = true
        updateTalkButtonLabel()
        ensureMicPermissionThenTalk()
    }

    /** End the conversation loop (back to idle). */
    private fun stopConversation() {
        conversationActive = false
        updateTalkButtonLabel()
    }

    private fun updateTalkButtonLabel() {
        talkButton.text = when {
            conversationActive && isRu() -> "⏹  Стоп"
            conversationActive          -> "⏹  Stop"
            isRu()                      -> "🎤  Говорить"
            else                        -> "🎤  Talk"
        }
    }

    /** Append one turn to the on-screen transcript and scroll to the newest line. */
    private fun appendTranscript(speaker: String, text: String) {
        transcript.append(speaker).append(": ").append(text).append("\n\n")
        conversationText.text = transcript.toString()
        conversationText.post {
            val layout = conversationText.layout ?: return@post
            val delta = layout.getLineBottom(conversationText.lineCount - 1) -
                conversationText.height - conversationText.scrollY
            if (delta > 0) conversationText.scrollBy(0, delta)
        }
    }

    private fun ensureMicPermissionThenTalk() {
        if (isRunningInTest()) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            startVoiceInput()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQ_CODE_RECORD_AUDIO_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQ_CODE_RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startVoiceInput()
                } else {
                    Toast.makeText(
                        this,
                        "Microphone permission is needed so your AI companion can hear you. Tap Talk again to allow it.",
                        Toast.LENGTH_LONG
                    ).show()
                    conversationText.text = "Microphone permission is needed to talk with your AI companion."
                }
            }
            REQ_CODE_RECORD_AUDIO_FOR_TIMED_LISTENING -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    applyTimedListeningPreference()
                } else {
                    Toast.makeText(
                        this,
                        "Microphone permission is needed for the always-listening mode the caregiver enabled. " +
                            "Push-to-talk will still work.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            REQ_CODE_NOTIFICATIONS_PERMISSION -> {
                // Optional — service runs either way; this just controls whether the
                // ongoing "Friendai is listening" notice is shown.
                applyTimedListeningPreference()
            }
        }
    }

    /**
     * Launches Android speech recognition for user input.
     */
    private fun startVoiceInput() {
        // If timed listening is enabled, tell it to step aside for ~20s so its recognizer
        // doesn't fight this screen's recognizer over the microphone.
        TimedListeningService.yieldMicForPushToTalk(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        // Use the auto-detected locale (mirrors TimedListeningService bilingual logic).
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentInputLocale.toLanguageTag())
        val promptText = if (currentInputLocale.language == "ru") "Говорите..." else "Say something to your AI"
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not supported. Please install the Google app or enable voice input in device settings.", Toast.LENGTH_LONG).show()
            conversationText.text = "Speech recognition not supported. Please install the Google app or enable voice input."
        }
    }

    /**
     * Handles results from speech recognition and updates conversation.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val userMsg = result?.getOrNull(0) ?: ""
                if (userMsg.isNotBlank()) {
                    // Auto-detect language from the recognised text — same Cyrillic/Latin
                    // heuristic as TimedListeningService so push-to-talk adapts to Russian too.
                    val cyrillicCount = userMsg.count { it in 'Ѐ'..'ӿ' }
                    val latinCount = userMsg.count { it.isLetter() && it !in 'Ѐ'..'ӿ' }
                    currentInputLocale = when {
                        cyrillicCount > latinCount -> Locale("ru", "RU")
                        latinCount > 0 && cyrillicCount == 0 -> Locale.US
                        else -> currentInputLocale
                    }
                    // Mirror the detected language in TTS so the AI speaks back in the
                    // same language Mom just used.
                    if (ttsReady) tts?.setLanguage(currentInputLocale)
                    handleUserMessage(userMsg)
                } else {
                    // Nothing heard — end the loop so we don't re-open the mic on silence.
                    stopConversation()
                    Toast.makeText(this, "I didn't catch that. Tap Talk to try again.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Cancelled or error — stop the loop; keep the transcript on screen.
                stopConversation()
                Toast.makeText(this, "Tap Talk when you'd like to speak again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Sends Mom's message through the configured AI backend (falling back to the local
     * rule-based companion when the backend is unavailable or unconfigured), then speaks
     * and displays the reply, and escalates to the help screen when needed.
     */
    private fun handleUserMessage(userMsg: String) {
        val youLabel = if (isRu()) "Вы" else "You"

        val monetization = MonetizationManager(this)
        if (!monetization.canUseAi()) {
            val msg = "You've had ${MonetizationManager.FREE_DAILY_LIMIT} AI conversations today — your free daily limit. " +
                "Ask your caregiver about upgrading to VIP for unlimited replies, or I'll be back tomorrow!"
            stopConversation()
            appendTranscript("Friendai", msg)
            speakReply(msg)
            return
        }

        // Show Mom's line in the running transcript right away.
        appendTranscript(youLabel, userMsg)
        playSoundEffect()
        animateEqualizer(true)

        val settings = CaregiverRulesStore(this).load()
        val recentConversation = recentTurns.joinToString("\n")
        // Route by the key the caregiver entered (Claude "sk-ant-" vs Gemini), then a proxy
        // backend, then any key bundled at build time; otherwise the offline engine.
        val key = settings.geminiApiKey
        val client = when {
            key.startsWith("sk-ant-") -> AnthropicAiClient(key, activityEngine, LocalAiClient(activityEngine))
            key.isNotBlank() -> GeminiAiClient(key, activityEngine, LocalAiClient(activityEngine))
            settings.backendUrl.isNotBlank() -> ProxyAiClient(settings.backendUrl, activityEngine, LocalAiClient(activityEngine))
            BuildConfig.DEFAULT_ANTHROPIC_KEY.isNotBlank() ->
                AnthropicAiClient(BuildConfig.DEFAULT_ANTHROPIC_KEY, activityEngine, LocalAiClient(activityEngine))
            BuildConfig.DEFAULT_GEMINI_KEY.isNotBlank() ->
                GeminiAiClient(BuildConfig.DEFAULT_GEMINI_KEY, activityEngine, LocalAiClient(activityEngine))
            else -> LocalAiClient(activityEngine)
        }

        client.generateReply(userMsg, recentConversation, settings, applicationContext) { reply ->
            monetization.incrementAiReply()
            runOnUiThread {
                rememberTurn(userMsg, reply.text)
                appendTranscript("Friendai", reply.text)
                speakReply(reply.text) {
                    animateEqualizer(false)
                    maybeEscalate(reply, activityEngine)
                    // Keep the conversation flowing hands-free: listen again for Mom's next turn.
                    if (conversationActive) {
                        conversationText.postDelayed({
                            if (conversationActive) ensureMicPermissionThenTalk()
                        }, 800)
                    }
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

    /**
     * Opens the Help/Emergency screen when a reply was flagged urgent, scam-related, or a
     * direct request to contact someone — so Mom is routed to a real person, not just told to be.
     */
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
            putExtra(EmergencyActivity.EXTRA_TITLE, title)
            putExtra(EmergencyActivity.EXTRA_MESSAGE, message)
            putExtra(EmergencyActivity.EXTRA_SHOW_EMERGENCY_BUTTON, showEmergencyButton)
        }
        startActivity(intent)
    }

    /**
     * Speaks the AI reply using TTS and calls onDone when finished.
     */
    private fun speakReply(text: String, onDone: (() -> Unit)? = null) {
        // Nudge media volume to at least 60% if it's very low — elderly users often have
        // phones on near-silent and miss AI replies entirely. We only raise, never lower.
        runCatching {
            val am = getSystemService(AudioManager::class.java)
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val minDesired = (maxVol * 0.55f).toInt()
            if (curVol < minDesired) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, minDesired, 0)
            }
        }
        if (ttsReady) {
            if (onDone != null) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { runOnUiThread { onDone() } }
                    override fun onDone(utteranceId: String?) { runOnUiThread { onDone() } }
                })
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ai_reply")
            } else {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ai_reply")
            }
        } else {
            Toast.makeText(this, "TTS not ready. Please enable TTS in device settings.", Toast.LENGTH_LONG).show()
            conversationText.text = "TTS not ready. Please enable TTS in device settings."
            onDone?.invoke()
        }
    }

    /**
     * Plays a short, gentle chime when the AI is about to speak, giving Mom an audible
     * cue that a response is coming. Uses ToneGenerator so no audio file is needed.
     */
    private fun playSoundEffect() {
        runCatching {
            // TONE_PROP_ACK is a short pleasant acknowledgement tone; volume 40% to stay subtle.
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 40)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 180)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching { toneGen.release() }
            }, 250)
        }
        // Sound is purely cosmetic — ignore any failure silently.
    }

    /**
     * Animates the equalizer view to indicate voice activity.
     */
    private fun animateEqualizer(active: Boolean) {
        equalizerView.isAnimating = active
    }

    // Show closed caption overlay for speech (user or AI)
    /**
     * Shows a closed caption overlay for speech (user or AI).
     */
    private fun showClosedCaption(text: String, durationMs: Long = 4000L) {
        runOnUiThread {
            closedCaptionText.removeCallbacks(null) // cancel any pending hide
            closedCaptionText.text = text
            closedCaptionText.visibility = TextView.VISIBLE
            closedCaptionText.postDelayed({
                closedCaptionText.visibility = TextView.GONE
            }, durationMs)
        }
    }

    /**
     * Shows the onboarding dialog on first launch.
     */
    private fun showOnboardingIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
        val shown = prefs.getBoolean("shown", false)
        if (!shown) {
            AlertDialog.Builder(context)
                .setTitle("Welcome to Friendai!")
                .setMessage("This app helps you stay safe and connected.\n\n• Tap 'Talk' to speak with your AI companion.\n• The app can understand English and Russian.\n• Caregivers can set up rules, contacts, and PIN protection.\n• All conversations are private and secure.\n\nPress 'Get Started' to begin.")
                .setPositiveButton("Get Started") { dialog, _ ->
                    prefs.edit().putBoolean("shown", true).apply()
                    dialog.dismiss()

                    // Step 2: Language selection
                    val languages = arrayOf("English", "Russian")
                    AlertDialog.Builder(context)
                        .setTitle("Choose your language")
                        .setSingleChoiceItems(languages, 0) { _, which ->
                            prefs.edit().putString("language", languages[which]).apply()
                        }
                        .setPositiveButton("Next") { d, _ ->
                            d.dismiss()

                            // Step 3: TTS Voice selection (simple demo: default or high contrast)
                            val voices = arrayOf("Default", "High Contrast")
                            AlertDialog.Builder(context)
                                .setTitle("Choose voice style")
                                .setSingleChoiceItems(voices, 0) { _, which ->
                                    prefs.edit().putString("tts_voice", voices[which]).apply()
                                }
                                .setPositiveButton("Next") { dlg, _ ->
                                    dlg.dismiss()

                                    // Step 4: Family phrase entry
                                    val input = android.widget.EditText(context)
                                    input.hint = "e.g. 'I love you, Mom!'"
                                    AlertDialog.Builder(context)
                                        .setTitle("Add a family phrase")
                                        .setMessage("Enter a phrase your AI companion can use to sound more familiar.")
                                        .setView(input)
                                        .setPositiveButton("Save") { phraseDialog, _ ->
                                            val phrase = input.text.toString()
                                            prefs.edit().putString("family_phrase", phrase).apply()
                                            phraseDialog.dismiss()
                                        }
                                        .setNegativeButton("Skip") { phraseDialog, _ ->
                                            phraseDialog.dismiss()
                                        }
                                        .show()
                                }
                                .setNegativeButton("Skip") { voiceDialog, _ ->
                                    voiceDialog.dismiss()
                                }
                                .show()
                        }
                        .setNegativeButton("Skip") { langDialog, _ ->
                            langDialog.dismiss()
                        }
                        .show()
                }
                .setCancelable(false)
                .show()
        }
    }
}
