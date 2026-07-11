package com.friendai

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.friendai.CaregiverRulesStore
import java.util.Locale

class EmergencyActivity : Activity() {

    private lateinit var rulesStore: CaregiverRulesStore
    /**
     * TTS instance used to speak the emergency title and message aloud as soon as this
     * screen appears. Critical for hands-free / dementia users who may not read the screen.
     */
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rulesStore = CaregiverRulesStore(this)
        val settings = rulesStore.load()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Need Help?"
        val message = intent.getStringExtra(EXTRA_MESSAGE)
            ?: "If something feels urgent or unsafe, contact someone you trust now."
        val showEmergencyButton = intent.getBooleanExtra(EXTRA_SHOW_EMERGENCY_BUTTON, false)
        val isTablet = ResponsiveLayout.isTablet(this)
        val horizontalPadding = if (isTablet) 40 else 24
        val contentWidth = ResponsiveLayout.constrainedWidth(this, 620, horizontalPadding)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (isTablet) Gravity.CENTER else Gravity.CENTER_HORIZONTAL
            setPadding(dp(horizontalPadding), dp(32), dp(horizontalPadding), dp(24))
        }

        val titleText = TextView(this).apply {
            text = title
            textSize = if (isTablet) 34f else 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(UiStyle.DANGER)
        }

        val messageText = TextView(this).apply {
            text = message
            textSize = if (isTablet) 26f else 24f
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(24))
            setTextColor(UiStyle.TEXT_PRI)
            isFocusable = true
            isFocusableInTouchMode = true
            contentDescription = message
        }

        val caregiverButton = Button(this).apply {
            text = if (settings.contactName.isBlank()) {
                "Call Caregiver"
            } else {
                "Call ${settings.contactName}"
            }
            textSize = if (isTablet) 28f else 24f
            setAllCaps(false)
            isEnabled = settings.contactPhone.isNotBlank()
            UiStyle.styleBtn(this, UiStyle.DANGER, 14f)
            setOnClickListener { dial(settings.contactPhone) }
            contentDescription = "Call caregiver button"
        }

        val emergencyButton = Button(this).apply {
            text = "Call Emergency Services"
            textSize = if (isTablet) 24f else 22f
            setAllCaps(false)
            visibility = if (showEmergencyButton) android.view.View.VISIBLE else android.view.View.GONE
            UiStyle.styleBtn(this, UiStyle.DANGER, 14f)
            contentDescription = "Call Emergency Services — 911"
            setOnClickListener { dial("911") }
        }

        val doneButton = Button(this).apply {
            text = "Done"
            textSize = if (isTablet) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtnSecondary(this)
            setOnClickListener { finish() }
        }

        root.addView(
            titleText,
            LinearLayout.LayoutParams(
                contentWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            messageText,
            LinearLayout.LayoutParams(
                contentWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            caregiverButton,
            LinearLayout.LayoutParams(
                contentWidth,
                dp(if (isTablet) 80 else 72)
            )
        )
        root.addView(
            emergencyButton,
            LinearLayout.LayoutParams(
                contentWidth,
                dp(if (isTablet) 80 else 72)
            ).apply {
                setMargins(0, dp(12), 0, 0)
            }
        )
        root.addView(
            doneButton,
            LinearLayout.LayoutParams(
                contentWidth,
                dp(if (isTablet) 64 else 56)
            ).apply {
                setMargins(0, dp(20), 0, 0)
            }
        )

        setContentView(root)

        // Speak the title and message aloud immediately so Mom knows what's happening
        // even without looking at the screen. Essential for hands-free / dementia use.
        val speechText = "$title. $message"
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.getDefault())
                tts?.setSpeechRate(settings.ttsSpeechRate)
                tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "emergency_message")
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun dial(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            return
        }

        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")))
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_SHOW_EMERGENCY_BUTTON = "show_emergency_button"
    }
}
