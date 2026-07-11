package com.friendai
import com.friendai.CaregiverRulesStore
import com.friendai.ResponsiveLayout
import com.friendai.PinActivity

import android.Manifest
import android.app.Activity
import android.os.PowerManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class TroubleshootingActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var statusText: TextView
    private lateinit var backendButton: Button
    private lateinit var rulesStore: CaregiverRulesStore
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsChecked = false
    private var latestNote = "Device check ready."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rulesStore = CaregiverRulesStore(this)
        val isTablet = ResponsiveLayout.isTablet(this)
        val horizontalPadding = if (isTablet) 40 else 20
        val contentWidth = ResponsiveLayout.constrainedWidth(this, 720, horizontalPadding)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(horizontalPadding), dp(28), dp(horizontalPadding), dp(24))
        }

        val titleText = TextView(this).apply {
            text = "Device Check"
            textSize = if (isTablet) 34f else 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(UiStyle.ACCENT)
        }

        statusText = TextView(this).apply {
            textSize = if (isTablet) 22f else 19f
            setLineSpacing(0f, 1.15f)
            setPadding(0, dp(18), 0, dp(18))
            setTextColor(UiStyle.TEXT_PRI)
        }

        val micButton = Button(this).apply {
            text = "Request Microphone"
            textSize = if (isTablet) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtn(this, UiStyle.ACCENT)
            setOnClickListener { requestMicrophonePermission() }
        }

        val voiceButton = Button(this).apply {
            text = "Test Voice"
            textSize = if (isTablet) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtn(this, UiStyle.TALK)
            setOnClickListener { testVoice() }
        }

        backendButton = Button(this).apply {
            text = "Check Backend"
            textSize = if (isTablet) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtn(this, UiStyle.ACCENT)
            setOnClickListener { checkBackend() }
        }

        val settingsButton = Button(this).apply {
            text = "Caregiver Settings"
            textSize = if (isTablet) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtnSecondary(this)
            setOnClickListener {
                startActivity(Intent(this@TroubleshootingActivity, PinActivity::class.java))
            }
        }

        val doneButton = Button(this).apply {
            text = "Done"
            textSize = if (isTablet) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtnSecondary(this)
            setOnClickListener { finish() }
        }

        root.addView(titleText, fixedWidthWrap(contentWidth))
        root.addView(statusText, fixedWidthWrap(contentWidth))
        root.addView(micButton, buttonParams(contentWidth, isTablet))
        root.addView(voiceButton, buttonParams(contentWidth, isTablet))
        root.addView(backendButton, buttonParams(contentWidth, isTablet))
        root.addView(settingsButton, buttonParams(contentWidth, isTablet))
        root.addView(doneButton, buttonParams(contentWidth, isTablet))

        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                addView(root)
            }
        )

        refreshStatus()
        tts = TextToSpeech(this, this)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onInit(status: Int) {
        ttsChecked = true
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.language = Locale.getDefault()
        }
        latestNote = if (ttsReady) "Voice system is ready." else "Voice system is unavailable."
        refreshStatus()
    }

    private fun requestMicrophonePermission() {
        if (hasAudioPermission()) {
            latestNote = "Microphone permission is already granted."
            refreshStatus()
            return
        }

        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO_PERMISSION)
    }

    private fun testVoice() {
        if (!ttsReady) {
            latestNote = "Voice is not ready yet."
            refreshStatus()
            Toast.makeText(this, "Voice is not ready yet.", Toast.LENGTH_LONG).show()
            return
        }

        latestNote = "Playing test voice."
        refreshStatus()
        // Use the onboarding language preference so the test is in the right language.
        val prefs = getSharedPreferences("onboarding", MODE_PRIVATE)
        val testPhrase = if (prefs.getString("language", "English") == "Russian")
            "Голосовой тест Friendai. Я готова разговаривать."
        else
            "Friendai voice test. I am ready to talk."
        tts?.speak(testPhrase, TextToSpeech.QUEUE_FLUSH, null, "device_check_voice")
    }

    private fun checkBackend() {
        val settings = rulesStore.load()
        if (settings.backendUrl.isBlank()) {
            latestNote = "Backend URL is not set. Open Caregiver Settings to add it."
            refreshStatus()
            return
        }

        backendButton.isEnabled = false
        latestNote = "Checking backend health..."
        refreshStatus()

        Thread {
            val result = runCatching {
                val connection = (URL(healthUrl(settings.backendUrl)).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/json")
                }
                val code = connection.responseCode
                connection.disconnect()

                if (code in 200..299) {
                    "Backend health check passed."
                } else {
                    "Backend responded with HTTP $code."
                }
            }.getOrElse { error ->
                "Backend check failed: ${error.message ?: "unable to connect"}"
            }

            runOnUiThread {
                latestNote = result
                backendButton.isEnabled = true
                refreshStatus()
            }
        }.start()
    }

    private fun refreshStatus() {
        val settings = rulesStore.load()
        val layout = if (ResponsiveLayout.isTablet(this)) {
            if (ResponsiveLayout.isTabletLandscape(this)) "Tablet landscape" else "Tablet portrait"
        } else {
            "Phone"
        }

        val ttsStatus = when {
            !ttsChecked -> "Starting"
            ttsReady -> "Ready"
            else -> "Unavailable"
        }

        statusText.text = """
            $latestNote

            Microphone: ${if (hasAudioPermission()) "Granted" else "Needs permission"}
            Speech recognition: ${if (isSpeechRecognitionAvailable()) "Available" else "Not found"}
            Voice: $ttsStatus
            Caregiver contact: ${if (settings.contactPhone.isBlank()) "Not set" else "Set"}
            AI backend URL: ${if (settings.backendUrl.isBlank()) "Not set" else "Set"}
            AI backend token: ${if (settings.backendToken.isBlank()) "Not set" else "Set"}
            First setup: ${if (settings.setupComplete) "Complete" else "Not complete"}
            Always-listening: ${if (settings.timedListeningHours > 0) "${settings.timedListeningHours}h window enabled" else "Off (push-to-talk only)"}
            Lock screen visible: ${if (settings.timedListeningHours > 0) "Yes (replies shown over lock screen)" else "N/A (push-to-talk only)"}
            Battery optimization: ${batteryOptStatus()}
            Layout: $layout
            ${vipStatusLine()}
        """.trimIndent()
    }

    private fun batteryOptStatus(): String {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return if (pm.isIgnoringBatteryOptimizations(packageName)) {
            "Exempted ✓ (timed listening will run reliably)"
        } else {
            "Not exempted — timed listening may stop early. Open Settings to fix."
        }
    }

    private fun vipStatusLine(): String {
        val m = MonetizationManager(this)
        return if (m.isVipUser()) {
            "VIP: Active — unlimited AI replies"
        } else {
            val left = m.getRemainingAiReplies()
            "VIP: Free tier — $left ${if (left == 1) "AI reply" else "AI replies"} left today"
        }
    }

    private fun healthUrl(backendUrl: String): String {
        val trimmed = backendUrl.trim().trimEnd('/')
        val baseUrl = if (trimmed.endsWith("/companion/reply")) {
            trimmed.removeSuffix("/companion/reply")
        } else {
            trimmed
        }

        return "$baseUrl/health"
    }

    private fun isSpeechRecognitionAvailable(): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        return packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    }

    private fun hasAudioPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            latestNote = if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                "Microphone permission granted."
            } else {
                "Microphone permission was not granted."
            }
            refreshStatus()
        }
    }

    private fun fixedWidthWrap(width: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            width,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun buttonParams(width: Int, isTablet: Boolean): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            width,
            dp(if (isTablet) 72 else 60)
        ).apply {
            setMargins(0, dp(10), 0, 0)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 201
    }
}

