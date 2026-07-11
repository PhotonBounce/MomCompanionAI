package com.friendai

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists VIP status and daily free-tier AI reply usage across app restarts.
 *
 * Free tier: 20 AI replies per day (generous enough for casual use, enough to
 * showcase the companion, but shows value of VIP for daily active dementia care).
 * VIP: unlimited replies, unlocked via Google Play Billing in [VipActivity].
 *
 * All state is persisted to SharedPreferences so:
 * - VIP users stay VIP after the app restarts (not just until the next kill)
 * - Free daily limit actually enforces itself across sessions
 * - "Restore Purchase" correctly re-reads the persisted VIP flag
 *
 * IMPORTANT: After any Google Play purchase acknowledgement, call [setVip] with the
 * context so the flag is saved to disk. See [VipActivity].
 */
class MonetizationManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── VIP status ──────────────────────────────────────────────────────────────

    fun isVipUser(): Boolean = prefs.getBoolean(KEY_IS_VIP, false)

    fun setVip(status: Boolean) {
        prefs.edit().putBoolean(KEY_IS_VIP, status).apply()
    }

    // ── Daily free-tier reply counter ────────────────────────────────────────────

    /** Call once per AI reply generated (before or after, doesn't matter). */
    fun incrementAiReply() {
        rolloverDayIfNeeded()
        prefs.edit().putInt(KEY_REPLY_COUNT, prefs.getInt(KEY_REPLY_COUNT, 0) + 1).apply()
    }

    /** Returns true if the user can receive another AI reply right now. */
    fun canUseAi(): Boolean {
        if (isVipUser()) return true
        rolloverDayIfNeeded()
        return prefs.getInt(KEY_REPLY_COUNT, 0) < FREE_DAILY_LIMIT
    }

    /** Replies left today (Int.MAX_VALUE for VIP). */
    fun getRemainingAiReplies(): Int {
        if (isVipUser()) return Int.MAX_VALUE
        rolloverDayIfNeeded()
        return (FREE_DAILY_LIMIT - prefs.getInt(KEY_REPLY_COUNT, 0)).coerceAtLeast(0)
    }

    /**
     * Resets the daily counter when a new calendar day begins (checked lazily on
     * every access — no background job or alarm needed).
     */
    private fun rolloverDayIfNeeded() {
        val today = dayOfYear()
        val savedDay = prefs.getInt(KEY_LAST_RESET_DAY, -1)
        if (savedDay != today) {
            prefs.edit()
                .putInt(KEY_LAST_RESET_DAY, today)
                .putInt(KEY_REPLY_COUNT, 0)
                .apply()
        }
    }

    private fun dayOfYear(): Int =
        java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)

    companion object {
        private const val PREFS_NAME = "monetization"
        private const val KEY_IS_VIP = "is_vip"
        private const val KEY_REPLY_COUNT = "daily_reply_count"
        private const val KEY_LAST_RESET_DAY = "last_reset_day"

        /**
         * Free daily AI replies before the app nudges toward VIP.
         * 20 is generous for a demo day and still shows the value of unlocking.
         */
        const val FREE_DAILY_LIMIT = 20
    }
}
