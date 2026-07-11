package com.friendai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Automatically restarts [TimedListeningService] after the device reboots, if the caregiver
 * had timed-listening enabled when the device shut down.
 *
 * Without this, any phone restart silently ends the listening window, and Mom — who cannot
 * reliably operate the app herself — would be left without a companion until the caregiver
 * notices and manually restarts the service.
 *
 * Registered for both BOOT_COMPLETED (standard) and QUICKBOOT_POWERON (some HTC/custom ROMs).
 *
 * Note: if the user manually force-stops the app via Settings → Apps, Android places it in
 * "stopped state" and broadcast delivery is suppressed until the next manual launch. This is
 * an Android platform restriction; we document it in the Play Store description.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON") return

        val settings = CaregiverRulesStore(context).load()
        if (settings.timedListeningHours > 0) {
            // Use goAsync so we have a bit more time on the main thread before the
            // receiver is recycled, then fire and forget.
            val pendingResult = goAsync()
            try {
                TimedListeningService.start(context, settings.timedListeningHours)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
