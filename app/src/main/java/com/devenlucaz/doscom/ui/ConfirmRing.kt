package com.devenlucaz.doscom.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator

class ConfirmRing(
    context: Context, 
    private val windowManager: WindowManager,
    private val onComplete: () -> Unit = {}
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4285F4")
        style = Paint.Style.STROKE
        strokeWidth = 6f * context.resources.displayMetrics.density
    }

    private var currentScale = 0.5f
    private var currentAlpha = 255
    private var animator: ValueAnimator? = null

    init {
        startAnimation()
    }

    private fun startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            repeatCount = 1
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                currentScale = 0.5f + (1.5f - 0.5f) * progress
                currentAlpha = (255 * (1f - progress)).toInt()
                
                paint.alpha = currentAlpha
                scaleX = currentScale
                scaleY = currentScale
                invalidate()
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    try {
                        windowManager.removeView(this@ConfirmRing)
                    } catch (e: Exception) {
                        // Ignore
                    }
                    onComplete()
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) - paint.strokeWidth) / 2f
        if (radius > 0) {
            canvas.drawCircle(cx, cy, radius, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
