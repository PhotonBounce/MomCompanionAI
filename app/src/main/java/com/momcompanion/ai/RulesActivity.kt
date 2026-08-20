package com.friendai

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class RulesActivity : Activity() {
    private fun privacyNotice(): TextView {
        return TextView(this).apply {
            text = "Privacy Notice: This app uses the microphone for speech recognition — either when Mom taps Talk, or during a caregiver-set listening window (shown by an ongoing notification). Conversation text is sent to Google Gemini AI (if you add a key below) or your custom backend — only if configured. Nothing is stored. No ads, no tracking."
            textSize = if (isTabletLayout) 18f else 16f
            setPadding(0, dp(10), 0, dp(10))
        }
    }

    private lateinit var rulesInput: EditText
    private lateinit var profileInput: EditText
    private lateinit var vocabularyInput: EditText
    private lateinit var promptTopicsInput: EditText
    private lateinit var pinInput: EditText
    private lateinit var contactNameInput: EditText
    private lateinit var contactPhoneInput: EditText
    private lateinit var backendUrlInput: EditText
    private lateinit var backendTokenInput: EditText
    private lateinit var geminiApiKeyInput: EditText
    private lateinit var testMessageInput: EditText
    private lateinit var testResultText: TextView
    private lateinit var localTestButton: Button
    private lateinit var backendTestButton: Button
    private lateinit var statusText: TextView
    private lateinit var rulesStore: CaregiverRulesStore
    private lateinit var textSizeSpinner: android.widget.Spinner
    private lateinit var listeningHoursSpinner: android.widget.Spinner
    private lateinit var ttsSpeedSpinner: android.widget.Spinner
    private lateinit var premiumVoiceCheck: android.widget.CheckBox
    private val listeningHoursOptions = listOf(0, 1, 2, 3, 4, 6, 8, 10, 12)
    /** Maps spinner position → TTS speech rate multiplier. Slow = 0.7, Normal = 1.0, Fast = 1.3. */
    private val ttsSpeedOptions = listOf(0.7f, 1.0f, 1.3f)
    private var isFirstRunSetup = false
    private var isTabletLayout = false
    private var userTextSize = 18f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rulesStore = CaregiverRulesStore(this)
        val savedSettings = rulesStore.load()
        isFirstRunSetup = intent.getBooleanExtra(EXTRA_FIRST_RUN_SETUP, false)
        isTabletLayout = ResponsiveLayout.isTablet(this)
        val horizontalPadding = if (isTabletLayout) 40 else 20
        val verticalPadding = if (isTabletLayout) 32 else 24
        val contentWidth = ResponsiveLayout.constrainedWidth(this, 920, horizontalPadding)
        // Load text size from onboarding prefs if available
        val prefs = getSharedPreferences("onboarding", MODE_PRIVATE)
        userTextSize = prefs.getFloat("textSize", if (isTabletLayout) 22f else 18f)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(horizontalPadding),
                dp(verticalPadding),
                dp(horizontalPadding),
                dp(verticalPadding)
            )
        }

        val titleText = TextView(this).apply {
            text = if (isFirstRunSetup) "First Setup" else "Caregiver Settings"
            textSize = userTextSize + 10f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(UiStyle.ACCENT)
            contentDescription = text
        }

        // Text size option
        val textSizeLabel = TextView(this).apply {
            text = "Text Size:"
            textSize = userTextSize
            setPadding(0, dp(10), 0, dp(4))
        }
        textSizeSpinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, listOf("Normal", "Large", "Extra Large"))
            setSelection(when {
                userTextSize >= 32f -> 2
                userTextSize >= 26f -> 1
                else -> 0
            })
            contentDescription = "Text size selector"
        }

        // TTS speed: lets the caregiver slow down AI replies for users who need more time.
        val ttsSpeedLabel = TextView(this).apply {
            text = "AI Voice Speed:"
            textSize = userTextSize
            setPadding(0, dp(10), 0, dp(4))
        }
        ttsSpeedSpinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Slow (easier to follow)", "Normal", "Fast")
            )
            val savedRate = savedSettings.ttsSpeechRate
            val closestIndex = ttsSpeedOptions.indexOfFirst { Math.abs(it - savedRate) < 0.2f }.let { if (it >= 0) it else 1 }
            setSelection(closestIndex)
            contentDescription = "AI voice speed selector"
        }

        // Premium cloud voice: a warm, human-quality voice for every reply. Off by default
        // because it consumes cloud credits; the free on-device voice is used when unchecked.
        // (The welcome greeting is always premium — it's pre-recorded and costs nothing.)
        val premiumVoiceLabel = TextView(this).apply {
            text = "Premium AI Voice:"
            textSize = userTextSize
            setPadding(0, dp(10), 0, dp(4))
        }
        premiumVoiceCheck = android.widget.CheckBox(this).apply {
            text = "Use the warm premium voice for every reply (uses cloud credits). " +
                "Leave off to use the free built-in phone voice."
            textSize = userTextSize - 2f
            isChecked = getSharedPreferences("friendai_prefs", MODE_PRIVATE)
                .getBoolean("premium_voice", false)
            contentDescription = "Premium AI voice toggle"
        }

        // Timed listening: lets the app actively listen for/converse with Mom WITHOUT
        // requiring her to press Talk — important for people who can't reliably operate
        // a button (e.g. dementia patients with limited motor/cognitive ability).
        val listeningHoursLabel = TextView(this).apply {
            text = "Always-listening window (no button press needed):"
            textSize = userTextSize
            setPadding(0, dp(10), 0, dp(4))
        }
        val listeningHoursDescription = TextView(this).apply {
            text = "If Mom can't press Talk, choose how many hours the app should actively " +
                "listen and respond on its own, starting when she opens the app. Choose " +
                "'Off' to keep push-to-talk only. Maximum 12 hours; the app stops listening " +
                "automatically when the timer ends."
            textSize = userTextSize - 2f
            setPadding(0, 0, 0, dp(6))
        }
        listeningHoursSpinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                listeningHoursOptions.map { if (it == 0) "Off (push-to-talk only)" else "$it hour${if (it == 1) "" else "s"}" }
            )
            val savedHours = savedSettings.timedListeningHours.coerceIn(0, 12)
            val closestIndex = listeningHoursOptions.indexOf(savedHours).let { if (it >= 0) it else 0 }
            setSelection(closestIndex)
            contentDescription = "Always-listening duration selector, up to 12 hours"
        }

        statusText = TextView(this).apply {
            text = if (isFirstRunSetup) {
                "Set a private PIN and caregiver contact before Mom uses the app."
            } else {
                ""
            }
            textSize = userTextSize + 2f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
            contentDescription = "Status: $text"
        }

        rulesInput = multilineInput(savedSettings.rules, minLines = 7).apply {
            contentDescription = "Rules input"
            textSize = userTextSize
        }
        profileInput = multilineInput(savedSettings.profileNotes, minLines = 4).apply {
            contentDescription = "Profile notes input"
            textSize = userTextSize
        }
        vocabularyInput = multilineInput(savedSettings.vocabularyNotes, minLines = 12).apply {
            contentDescription = "Vocabulary input"
            textSize = userTextSize
        }
        promptTopicsInput = multilineInput(savedSettings.promptTopics, minLines = 8).apply {
            contentDescription = "Prompt topics input"
            textSize = userTextSize
        }
        pinInput = singleLineInput(savedSettings.pin, InputType.TYPE_CLASS_NUMBER).apply {
            contentDescription = "PIN input"
            textSize = userTextSize
        }
        contactNameInput = singleLineInput(savedSettings.contactName, InputType.TYPE_CLASS_TEXT).apply {
            contentDescription = "Caregiver name input"
            textSize = userTextSize
        }
        contactPhoneInput = singleLineInput(savedSettings.contactPhone, InputType.TYPE_CLASS_PHONE).apply {
            contentDescription = "Caregiver phone input"
            textSize = userTextSize
        }
        backendUrlInput = singleLineInput(savedSettings.backendUrl, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI).apply {
            contentDescription = "AI backend URL input"
            textSize = userTextSize
        }
        backendTokenInput = singleLineInput(
            savedSettings.backendToken,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        ).apply {
            contentDescription = "AI backend token input"
            textSize = userTextSize
        }
        geminiApiKeyInput = singleLineInput(
            savedSettings.geminiApiKey,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        ).apply {
            contentDescription = "Gemini API key input"
            textSize = userTextSize
            hint = "Paste free key from aistudio.google.com/apikey"
        }
        testMessageInput = multilineInput(DEFAULT_TEST_MESSAGE, minLines = 3).apply {
            contentDescription = "Test message input"
            textSize = userTextSize
        }
        testResultText = TextView(this).apply {
            text = "No test run yet."
            textSize = userTextSize + 2f
            setPadding(0, dp(10), 0, dp(10))
            contentDescription = "Test result"
        }

        val saveButton = Button(this).apply {
            text = "Save"
            textSize = if (isTabletLayout) 24f else 22f
            setAllCaps(false)
            UiStyle.styleBtn(this, UiStyle.ACCENT)
            setOnClickListener { saveSettings() }
            contentDescription = "Save settings button"
        }

        val resetButton = Button(this).apply {
            text = "Reset Rules"
            textSize = if (isTabletLayout) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtnSecondary(this)
            setOnClickListener {
                rulesStore.resetRulesAndProfile()
                val defaults = rulesStore.load()
                rulesInput.setText(defaults.rules)
                profileInput.setText(defaults.profileNotes)
                vocabularyInput.setText(defaults.vocabularyNotes)
                statusText.text = "Default rules and vocabulary restored"
            }
            contentDescription = "Reset rules button"
        }

        localTestButton = Button(this).apply {
            text = "Test Local Rules"
            textSize = if (isTabletLayout) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtn(this, UiStyle.TALK)
            setOnClickListener { runLocalReplyTest() }
            contentDescription = "Test local rules button"
        }

        backendTestButton = Button(this).apply {
            text = "Test AI Backend"
            textSize = if (isTabletLayout) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtn(this, UiStyle.TALK)
            setOnClickListener { runBackendReplyTest() }
            contentDescription = "Test AI backend button"
        }

        val closeButton = Button(this).apply {
            text = "Done"
            textSize = if (isTabletLayout) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtnSecondary(this)
            setOnClickListener { finish() }
            contentDescription = "Done button"
        }

        val privacyPolicyButton = Button(this).apply {
            text = "Privacy Policy"
            textSize = if (isTabletLayout) 20f else 18f
            setAllCaps(false)
            UiStyle.styleBtnSecondary(this)
            setOnClickListener {
                runCatching {
                    startActivity(
                        Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse(PRIVACY_POLICY_URL))
                    )
                }
            }
            contentDescription = "Open privacy policy in browser"
        }

        val deviceCheckButton = Button(this).apply {
            text = "Device Check"
            textSize = if (isTabletLayout) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtnSecondary(this)
            setOnClickListener {
                startActivity(Intent(this@RulesActivity, TroubleshootingActivity::class.java))
            }
            contentDescription = "Open Device Check screen"
        }

        val vipButton = Button(this).apply {
            text = "VIP / Upgrade"
            textSize = if (isTabletLayout) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtn(this, UiStyle.VIP_GOLD)
            setOnClickListener {
                startActivity(Intent(this@RulesActivity, VipActivity::class.java))
            }
            contentDescription = "Open VIP upgrade screen"
        }

        root.addView(titleText, fullWidthWrap())
        root.addView(textSizeLabel, fullWidthWrap())
        root.addView(textSizeSpinner, fullWidthWrap())
        root.addView(ttsSpeedLabel, fullWidthWrap())
        root.addView(ttsSpeedSpinner, fullWidthWrap())
        root.addView(premiumVoiceLabel, fullWidthWrap())
        root.addView(premiumVoiceCheck, fullWidthWrap())
        root.addView(listeningHoursLabel, fullWidthWrap())
        root.addView(listeningHoursDescription, fullWidthWrap())
        root.addView(listeningHoursSpinner, fullWidthWrap())
        root.addView(statusText, fullWidthWrap())
        root.addView(label("Rules Prompt"))
        root.addView(rulesInput, fullWidthWrap())
        root.addView(label("Mom Profile Notes"))
        root.addView(profileInput, fullWidthWrap())
        root.addView(label("Basic Vocabulary (English / Russian)"))
        root.addView(vocabularyInput, fullWidthWrap())
        root.addView(label("Prompt Topics (one per line, e.g. 'the garden', 'cats', 'flowers')"))
        root.addView(promptTopicsInput, fullWidthWrap())
        root.addView(privacyNotice())
        root.addView(label("PIN"))
        root.addView(pinInput, fixedHeight(64))
        root.addView(label("Caregiver Name"))
        root.addView(contactNameInput, fixedHeight(64))
        root.addView(label("Caregiver Phone"))
        root.addView(contactPhoneInput, fixedHeight(64))
        root.addView(label("🤖 Free AI Key (Gemini) — makes the app smart!"))
        root.addView(geminiKeyHint(), fullWidthWrap())
        root.addView(geminiApiKeyInput, fixedHeight(64))
        root.addView(label("AI Backend URL (optional, advanced)"))
        root.addView(backendUrlInput, fixedHeight(64))
        root.addView(label("AI Backend Token"))
        root.addView(backendTokenInput, fixedHeight(64))
        root.addView(label("Test Message"))
        root.addView(testMessageInput, fullWidthWrap())
        root.addView(localTestButton, fixedHeight(56).apply { setMargins(0, dp(10), 0, 0) })
        root.addView(backendTestButton, fixedHeight(56).apply { setMargins(0, dp(10), 0, 0) })
        root.addView(testResultText, fullWidthWrap())
        root.addView(
            saveButton,
            fixedHeight(64).apply { setMargins(0, dp(18), 0, 0) }
        )
        root.addView(
            resetButton,
            fixedHeight(56).apply { setMargins(0, dp(10), 0, 0) }
        )
        root.addView(
            deviceCheckButton,
            fixedHeight(56).apply { setMargins(0, dp(10), 0, 0) }
        )
        root.addView(
            vipButton,
            fixedHeight(56).apply { setMargins(0, dp(10), 0, 0) }
        )
        root.addView(
            closeButton,
            fixedHeight(56).apply { setMargins(0, dp(10), 0, 0) }
        )
        root.addView(
            privacyPolicyButton,
            fixedHeight(48).apply { setMargins(0, dp(16), 0, 0) }
        )

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
        }

        if (isTabletLayout) {
            val outer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            outer.addView(
                root,
                LinearLayout.LayoutParams(
                    contentWidth,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            scrollView.addView(
                outer,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        } else {
            scrollView.addView(root)
        }

        setContentView(scrollView)
    }

    private fun saveSettings() {
        val pin = pinInput.text.toString().trim()
        if (pin.length < 4) {
            statusText.text = "PIN must be at least 4 digits."
            return
        }

        // Save text size preference
        val textSizeIdx = textSizeSpinner.selectedItemPosition
        val textSizePref = when (textSizeIdx) {
            2 -> 32f // Extra Large
            1 -> 26f // Large
            else -> 18f // Normal
        }
        val prefs = getSharedPreferences("onboarding", MODE_PRIVATE).edit()
        prefs.putFloat("textSize", textSizePref)
        prefs.apply()

        // Persist the premium-voice choice (read by MainActivity.speakReply).
        getSharedPreferences("friendai_prefs", MODE_PRIVATE).edit()
            .putBoolean("premium_voice", premiumVoiceCheck.isChecked)
            .apply()

        rulesStore.save(
            CaregiverSettings(
                rules = rulesInput.text.toString(),
                profileNotes = profileInput.text.toString(),
                vocabularyNotes = vocabularyInput.text.toString(),
                promptTopics = promptTopicsInput.text.toString(),
                pin = pin,
                contactName = contactNameInput.text.toString(),
                contactPhone = contactPhoneInput.text.toString(),
                backendUrl = backendUrlInput.text.toString(),
                backendToken = backendTokenInput.text.toString(),
                setupComplete = true,
                timedListeningHours = listeningHoursOptions.getOrElse(listeningHoursSpinner.selectedItemPosition) { 0 },
                ttsSpeechRate = ttsSpeedOptions.getOrElse(ttsSpeedSpinner.selectedItemPosition) { 1.0f },
                geminiApiKey = geminiApiKeyInput.text.toString()
            )
        )
        if (isFirstRunSetup) {
            statusText.text = "Setup complete! Taking you back to the app…"
            // Brief delay so the caregiver can read the confirmation, then return
            // to MainActivity automatically — no "Done" tap needed on first run.
            statusText.postDelayed({ finish() }, 1200)
        } else {
            statusText.text = "Saved"
        }
    }

    private fun runLocalReplyTest() {
        val message = testMessage()
        if (message == null) {
            return
        }

        setTestingState("Testing local rules...", enabled = false)
        LocalAiClient().generateReply(
            message = message,
            recentConversation = "",
            settings = readSettingsFromInputs()
        ) { reply ->
            runOnUiThread {
                showTestResult("Local rules reply", reply)
                setTestingState("Local test finished", enabled = true)
            }
        }
    }

    private fun runBackendReplyTest() {
        val message = testMessage()
        if (message == null) {
            return
        }

        val settings = readSettingsFromInputs()
        if (settings.backendUrl.isBlank()) {
            testResultText.text = "Add an AI Backend URL before testing the backend."
            return
        }

        setTestingState("Testing AI backend...", enabled = false)
        ProxyAiClient(
            endpointUrl = settings.backendUrl,
            fallbackEnabled = false
        ).generateReply(
            message = message,
            recentConversation = "Caregiver is testing settings before Mom uses the app.",
            settings = settings
        ) { reply ->
            runOnUiThread {
                showTestResult("AI backend reply", reply)
                setTestingState("Backend test finished", enabled = true)
            }
        }
    }

    private fun testMessage(): String? {
        val message = testMessageInput.text.toString().trim()
        if (message.isBlank()) {
            testResultText.text = "Type a test message first."
            return null
        }

        return message
    }

    private fun readSettingsFromInputs(): CaregiverSettings {
        return CaregiverSettings(
            rules = rulesInput.text.toString(),
            profileNotes = profileInput.text.toString(),
            vocabularyNotes = vocabularyInput.text.toString(),
            promptTopics = promptTopicsInput.text.toString(),
            pin = pinInput.text.toString(),
            contactName = contactNameInput.text.toString(),
            contactPhone = contactPhoneInput.text.toString(),
            backendUrl = backendUrlInput.text.toString(),
            backendToken = backendTokenInput.text.toString(),
            setupComplete = rulesStore.load().setupComplete,
            timedListeningHours = listeningHoursOptions.getOrElse(listeningHoursSpinner.selectedItemPosition) { 0 },
            ttsSpeechRate = ttsSpeedOptions.getOrElse(ttsSpeedSpinner.selectedItemPosition) { 1.0f },
            geminiApiKey = geminiApiKeyInput.text.toString()
        )
    }

    private fun showTestResult(title: String, reply: CompanionReply) {
        val escalation = when (reply.escalationLevel) {
            EscalationLevel.NONE -> "None"
            EscalationLevel.EMERGENCY -> "Emergency help screen"
            EscalationLevel.SCAM -> "Scam warning screen"
            EscalationLevel.MEDICAL -> "Medical/contact help screen"
        }

        testResultText.text = "$title\n\n${reply.text}\n\nEscalation: $escalation"
    }

    private fun setTestingState(message: String, enabled: Boolean) {
        statusText.text = message
        localTestButton.isEnabled = enabled
        backendTestButton.isEnabled = enabled
    }

    private fun geminiKeyHint(): TextView {
        return TextView(this).apply {
            text = "Get a FREE key (no credit card): go to aistudio.google.com/apikey → Create API key → paste it here. Once saved, the app uses real Gemini AI instead of the basic offline engine."
            textSize = if (isTabletLayout) 17f else 15f
            setPadding(0, 0, 0, dp(6))
        }
    }

    private fun multilineInput(text: String, minLines: Int): EditText {
        return EditText(this).apply {
            setText(text)
            textSize = if (isTabletLayout) 20f else 18f
            this.minLines = minLines
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
    }

    private fun singleLineInput(text: String, type: Int): EditText {
        return EditText(this).apply {
            setText(text)
            textSize = if (isTabletLayout) 20f else 18f
            inputType = type
            setSingleLine(true)
        }
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = if (isTabletLayout) 20f else 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(14), 0, dp(6))
        }
    }

    private fun fullWidthWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun fixedHeight(height: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(height)
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_FIRST_RUN_SETUP = "first_run_setup"
        private const val DEFAULT_TEST_MESSAGE = "Hello, I feel a little lonely today."
        /**
         * Update this to your live URL once the microsite/privacy page is deployed.
         * Currently set to the Friendai app domain — change to the hostupon.com address
         * or wherever the privacy.html from friendai-microsite.zip is published.
         */
        const val PRIVACY_POLICY_URL = "https://friendai.app/privacy"
    }
}
