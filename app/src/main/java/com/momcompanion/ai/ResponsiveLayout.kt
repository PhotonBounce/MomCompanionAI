package com.friendai

import android.app.Activity
import android.content.res.Configuration
import android.view.ViewGroup
import kotlin.math.min

object ResponsiveLayout {

    fun isTablet(activity: Activity): Boolean {
        return activity.resources.configuration.smallestScreenWidthDp >= 600
    }

    fun isTabletLandscape(activity: Activity): Boolean {
        return isTablet(activity) &&
            activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun constrainedWidth(
        activity: Activity,
        maxDp: Int,
        horizontalPaddingDp: Int
    ): Int {
        if (!isTablet(activity)) {
            return ViewGroup.LayoutParams.MATCH_PARENT
        }

        val horizontalPaddingPx = dp(activity, horizontalPaddingDp) * 2
        val availableWidth = activity.resources.displayMetrics.widthPixels - horizontalPaddingPx
        return min(dp(activity, maxDp), availableWidth.coerceAtLeast(dp(activity, 320)))
    }

    fun dp(activity: Activity, value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}
