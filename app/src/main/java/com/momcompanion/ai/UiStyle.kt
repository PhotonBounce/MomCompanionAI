package com.friendai

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

/**
 * Shared visual style constants and helpers for all programmatic-UI activities.
 * Activities extending android.app.Activity don't receive Material theme button
 * styling automatically — this object applies it explicitly.
 */
object UiStyle {

    // ── Palette ──────────────────────────────────────────────────────────────
    val ACCENT    = Color.parseColor("#7C4DFF")   // purple  — primary actions
    val TALK      = Color.parseColor("#00C896")   // teal    — Talk / positive
    val DANGER    = Color.parseColor("#FF4757")   // red     — emergency / caregiver call
    val VIP_GOLD  = Color.parseColor("#FF9500")   // gold    — VIP / upgrade
    val WHITE     = Color.WHITE
    val TEXT_PRI  = Color.parseColor("#1A1A2E")   // near-black body text
    val TEXT_SEC  = Color.parseColor("#666688")   // secondary / hint text
    val BG_TOP    = Color.parseColor("#F3EFFF")
    val BG_BOTTOM = Color.parseColor("#FFF0F7")
    private val SECONDARY = Color.parseColor("#EEEAF7")  // light-purple secondary button
    private val INPUT_BG  = Color.parseColor("#F0EBF8")  // light-purple input background

    // ── Background ───────────────────────────────────────────────────────────

    fun applyBg(view: View) {
        view.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(BG_TOP, BG_BOTTOM)
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    fun dp(view: View, dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, view.resources.displayMetrics)

    private fun roundedBg(color: Int, cornerDp: Float, view: View): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(view, cornerDp)
        }

    // ── Buttons ──────────────────────────────────────────────────────────────

    /** Filled button with ripple — use for primary / named actions. */
    fun styleBtn(btn: Button, color: Int, cornerDp: Float = 14f) {
        val shape = roundedBg(color, cornerDp, btn)
        btn.background = RippleDrawable(
            ColorStateList.valueOf(Color.argb(50, 255, 255, 255)),
            shape, shape
        )
        btn.setTextColor(WHITE)
    }

    /** Light-fill button — use for cancel / done / secondary actions. */
    fun styleBtnSecondary(btn: Button, cornerDp: Float = 14f) {
        val shape = roundedBg(SECONDARY, cornerDp, btn)
        btn.background = RippleDrawable(
            ColorStateList.valueOf(Color.argb(40, 100, 50, 200)),
            shape, shape
        )
        btn.setTextColor(ACCENT)
    }

    // ── Text inputs ──────────────────────────────────────────────────────────

    fun styleEditText(et: EditText) {
        et.background = roundedBg(INPUT_BG, 8f, et)
        et.setTextColor(TEXT_PRI)
        et.setHintTextColor(TEXT_SEC)
        val p = dp(et, 12f).toInt()
        et.setPadding(p, dp(et, 8f).toInt(), p, dp(et, 8f).toInt())
    }

    // ── Text views ───────────────────────────────────────────────────────────

    fun styleTitle(tv: TextView) {
        tv.setTextColor(ACCENT)
        tv.typeface = Typeface.DEFAULT_BOLD
    }

    fun styleDangerTitle(tv: TextView) {
        tv.setTextColor(DANGER)
        tv.typeface = Typeface.DEFAULT_BOLD
    }

    fun styleBody(tv: TextView) {
        tv.setTextColor(TEXT_PRI)
    }

    fun styleMuted(tv: TextView) {
        tv.setTextColor(TEXT_SEC)
    }
}
