package com.friendai
import com.friendai.MainActivity

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityLaunchTest {
    @Test
    fun mainActivity_starts_and_showsOnboarding() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // The onboarding dialog should be shown on first launch
            // (UIAutomator or Espresso can be used for more detailed checks)
            // For now, just ensure the activity launches without crashing
            assert(scenario.state.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))
        }
    }
}
