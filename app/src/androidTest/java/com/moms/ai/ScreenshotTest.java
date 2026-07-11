package com.friendai;
import com.friendai.MainActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Environment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class ScreenshotTest {
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    );

    private void takeScreenshotAndCopy(String name) throws IOException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        // Use Pictures/screenshots/playstore/phone for Play Store compliance
        String dirPath = "/sdcard/Pictures/screenshots/playstore/phone/";
        device.executeShellCommand("mkdir -p " + dirPath);
        String filename = name + "_" + System.currentTimeMillis() + ".png";
        File outFile = new File(dirPath, filename);
        boolean success = device.takeScreenshot(outFile);
        System.out.println("[TEST] Screenshot saved: " + outFile.getAbsolutePath() + " success=" + success);
    }

    @Test
    public void screenshot_onboardingDialog() throws IOException {
        ActivityScenario.launch(MainActivity.class);
        takeScreenshotAndCopy("onboarding_dialog");
    }

    @Test
    public void screenshot_mainScreen() throws IOException {
        ActivityScenario.launch(MainActivity.class);
        // Assume onboarding is completed
        takeScreenshotAndCopy("main_screen");
    }

    // Add more tests for each interaction as needed
}
