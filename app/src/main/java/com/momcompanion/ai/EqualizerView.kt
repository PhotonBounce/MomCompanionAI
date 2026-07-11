package com.friendai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class EqualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val barPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val barCount = 12
    private val barHeights = IntArray(barCount) { 0 }
    private val maxBarHeight get() = height * 0.8f

    var isAnimating = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                // Only start the animation loop if the view is attached to a window.
                if (isAttachedToWindow) scheduleAnimationFrame()
            } else {
                removeCallbacks(animationRunnable)
                for (i in barHeights.indices) barHeights[i] = 0
                invalidate()
            }
        }

    private val animationRunnable = Runnable {
        if (isAnimating && isAttachedToWindow) {
            // Guard against zero-height during measurement phase
            val minH = (height * 0.2f).toInt()
            val maxH = maxBarHeight.toInt()
            if (maxH > minH) {
                for (i in barHeights.indices) {
                    barHeights[i] = Random.nextInt(minH, maxH)
                }
            }
            invalidate()
            scheduleAnimationFrame()
        }
    }

    private fun scheduleAnimationFrame() {
        postDelayed(animationRunnable, 80)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Resume animation if it was set before we were attached.
        if (isAnimating) scheduleAnimationFrame()
    }

    override fun onDetachedFromWindow() {
        // Cancel any pending frames to avoid the view leaking via the message queue.
        removeCallbacks(animationRunnable)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val barWidth = width / (barCount * 1.5f)
        for (i in 0 until barCount) {
            val left = i * barWidth * 1.5f
            val top = height.toFloat() - barHeights[i].toFloat()
            val right = left + barWidth
            val bottom = height.toFloat()
            canvas.drawRoundRect(left, top, right, bottom, barWidth / 2f, barWidth / 2f, barPaint)
        }
    }
}
