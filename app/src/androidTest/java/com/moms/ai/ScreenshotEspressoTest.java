
package com.friendai;
import com.friendai.MainActivity;

import android.Manifest;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
public class ScreenshotEspressoTest {
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    );

    private void takeScreenshotAndCopy(String name) throws IOException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        String dirPath = "/sdcard/Pictures/screenshots/playstore/phone/";
        device.executeShellCommand("mkdir -p " + dirPath);
        String filename = name + "_" + System.currentTimeMillis() + ".png";
        File outFile = new File(dirPath, filename);
        boolean success = device.takeScreenshot(outFile);
        System.out.println("[TEST] Screenshot saved: " + outFile.getAbsolutePath() + " success=" + success);
    }

    @Test
    public void screenshot_talkToMeConversation() throws IOException {
        ActivityScenario.launch(MainActivity.class);
        // Handle onboarding dialogs if present
        try {
            // Step 1: Welcome dialog
            Espresso.onView(withText("Get Started")).check(matches(isDisplayed()));
            Espresso.onView(withText("Get Started")).perform(click());
        } catch (Exception ignored) {}

        try {
            // Step 2: Language selection
            Espresso.onView(withText("Choose your language")).check(matches(isDisplayed()));
            // Accept default (first) and click Next
            Espresso.onView(withText("Next")).perform(click());
        } catch (Exception ignored) {}

        try {
            // Step 3: Voice selection
            Espresso.onView(withText("Choose voice style")).check(matches(isDisplayed()));
            // Accept default (first) and click Next
            Espresso.onView(withText("Next")).perform(click());
        } catch (Exception ignored) {}

        try {
            // Step 4: Family phrase entry
            Espresso.onView(withText("Add a family phrase")).check(matches(isDisplayed()));
            // Skip phrase entry
            Espresso.onView(withText("Skip")).perform(click());
        } catch (Exception ignored) {}

        // Wait for conversationText to appear (retry for up to 5 seconds)
        boolean found = false;
        for (int i = 0; i < 10; i++) {
            try {
                Espresso.onView(ViewMatchers.withId(R.id.conversationText)).check(matches(isDisplayed()));
                found = true;
                break;
            } catch (Exception e) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        if (!found) throw new AssertionError("conversationText not found after waiting");
        takeScreenshotAndCopy("main_screen_full_auto");
    }
}
