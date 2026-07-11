package com.friendai;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import com.friendai.RulesActivity;
import com.friendai.PinActivity;
import com.friendai.EmergencyActivity;
import com.friendai.TroubleshootingActivity;
import com.friendai.VipActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class ScreenshotAllScreensTest {
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    );

    private void takeScreenshot(String name) throws IOException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        String dirPath = "/sdcard/Pictures/screenshots/playstore/phone/";
        device.executeShellCommand("mkdir -p " + dirPath);
        String filename = name + "_" + System.currentTimeMillis() + ".png";
        File outFile = new File(dirPath, filename);
        boolean success = device.takeScreenshot(outFile);
        System.out.println("[TEST] Screenshot saved: " + outFile.getAbsolutePath() + " success=" + success);
    }

    @Test
    public void screenshot_rulesActivity() throws IOException {
        ActivityScenario.launch(RulesActivity.class);
        takeScreenshot("rules_screen");
    }

    @Test
    public void screenshot_pinActivity() throws IOException {
        ActivityScenario.launch(PinActivity.class);
        takeScreenshot("pin_screen");
    }

    @Test
    public void screenshot_emergencyActivity() throws IOException {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
            EmergencyActivity.class);
        intent.putExtra("title", "Emergency");
        intent.putExtra("message", "Test emergency message");
        ActivityScenario.launch(intent);
        takeScreenshot("emergency_screen");
    }

    @Test
    public void screenshot_troubleshootingActivity() throws IOException {
        ActivityScenario.launch(TroubleshootingActivity.class);
        takeScreenshot("troubleshooting_screen");
    }

    @Test
    public void screenshot_vipActivity() throws IOException {
        ActivityScenario.launch(VipActivity.class);
        takeScreenshot("vip_screen");
    }
}
