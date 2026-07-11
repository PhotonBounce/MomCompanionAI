package com.momcompanion.ai

import android.Manifest
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
// Espresso imports removed
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.friendai.MainActivity

@RunWith(AndroidJUnit4::class)
class FullAutoQATest {
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET
    )

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private fun takeScreenshot(name: String) {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "$name.png")
        device.takeScreenshot(file)
    }

    @Test
    fun runFullQASuite() {
        ActivityScenario.launch(MainActivity::class.java)
        takeScreenshot("main_screen")

        // Try to trigger onboarding if present
        try {
            val getStartedBtn = device.findObject(UiSelector().textContains("Get Started").enabled(true))
            if (getStartedBtn.exists()) {
                getStartedBtn.click()
                Thread.sleep(1000)
                takeScreenshot("onboarding")
            }
        } catch (e: Exception) {}

        // Simulate all main flows
        val testInputs = listOf(
            "How are you?",
            "Tell me a story",
            "joke",
            "music",
            "question",
            "sad",
            "family loves",
            "health",
            "emergency",
            "scam",
            "repeat",
            "topic: flowers"
        )
        for (input in testInputs) {
            try {
                // Find input field and enter text if present
                val inputField = device.findObject(UiSelector().className("android.widget.EditText"))
                if (inputField.exists()) {
                    inputField.setText("")
                    inputField.legacySetText(input)
                } else {
                    // If no input field, try to trigger voice input button
                    val talkBtn = device.findObject(UiSelector().textContains("Talk").enabled(true))
                    if (talkBtn.exists()) talkBtn.click()
                }
                Thread.sleep(2000)
                takeScreenshot("reply_${input.replace(" ", "_")}")
            } catch (e: Exception) {
                // fallback: try to tap screen and capture
                takeScreenshot("reply_${input.replace(" ", "_")}_fallback")
            }
        }
    }
}

// Extension for UiObject to set text (for older APIs)
fun androidx.test.uiautomator.UiObject.legacySetText(text: String) {
    setText(text)
}
