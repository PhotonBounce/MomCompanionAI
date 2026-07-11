package com.friendai

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.friendai.CaregiverRulesStore

class PinActivity : Activity() {

    private lateinit var pinInput: EditText
    private lateinit var statusText: TextView
    private lateinit var rulesStore: CaregiverRulesStore
    private lateinit var unlockButton: Button
    private var failedAttempts = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rulesStore = CaregiverRulesStore(this)
        val isTablet = ResponsiveLayout.isTablet(this)
        val horizontalPadding = if (isTablet) 40 else 24
        val contentWidth = ResponsiveLayout.constrainedWidth(this, 460, horizontalPadding)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (isTablet) Gravity.CENTER else Gravity.CENTER_HORIZONTAL
            setPadding(dp(horizontalPadding), dp(40), dp(horizontalPadding), dp(24))
        }

        val titleText = TextView(this).apply {
            text = "Caregiver PIN"
            textSize = if (isTablet) 32f else 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(UiStyle.ACCENT)
        }

        statusText = TextView(this).apply {
            text = "Enter the caregiver PIN."
            textSize = if (isTablet) 20f else 18f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(16))
            setTextColor(UiStyle.TEXT_PRI)
        }

        pinInput = EditText(this).apply {
            textSize = if (isTablet) 28f else 24f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "••••"
            imeOptions = EditorInfo.IME_ACTION_DONE
            contentDescription = "Caregiver PIN input"
            UiStyle.styleEditText(this)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) { unlock(); true } else false
            }
        }

        unlockButton = Button(this).apply {
            text = "Unlock"
            textSize = if (isTablet) 24f else 22f
            setAllCaps(false)
            UiStyle.styleBtn(this, UiStyle.ACCENT)
            contentDescription = "Unlock button"
            setOnClickListener { unlock() }
        }

        val cancelButton = Button(this).apply {
            text = "Cancel"
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
            statusText,
            LinearLayout.LayoutParams(
                contentWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            pinInput,
            LinearLayout.LayoutParams(
                contentWidth,
                dp(if (isTablet) 80 else 72)
            )
        )
        root.addView(
            unlockButton,
            LinearLayout.LayoutParams(
                contentWidth,
                dp(if (isTablet) 72 else 64)
            ).apply {
                setMargins(0, dp(18), 0, 0)
            }
        )
        root.addView(
            cancelButton,
            LinearLayout.LayoutParams(
                contentWidth,
                dp(if (isTablet) 64 else 56)
            ).apply {
                setMargins(0, dp(10), 0, 0)
            }
        )

        setContentView(root)
    }

    private fun unlock() {
        if (!unlockButton.isEnabled) return  // locked out
        if (rulesStore.isPinValid(pinInput.text.toString().trim())) {
            failedAttempts = 0
            startActivity(Intent(this, RulesActivity::class.java))
            finish()
        } else {
            failedAttempts++
            if (failedAttempts >= MAX_ATTEMPTS) {
                // Lockout: disable input for 30 seconds to deter brute-force
                unlockButton.isEnabled = false
                pinInput.isEnabled = false
                statusText.text = "Too many wrong attempts. Please wait 30 seconds."
                handler.postDelayed({
                    failedAttempts = 0
                    unlockButton.isEnabled = true
                    pinInput.isEnabled = true
                    pinInput.text.clear()
                    statusText.text = "Enter the caregiver PIN."
                }, LOCKOUT_MS)
            } else {
                val remaining = MAX_ATTEMPTS - failedAttempts
                statusText.text = "PIN did not match. $remaining attempt${if (remaining == 1) "" else "s"} remaining."
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_MS = 30_000L
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
