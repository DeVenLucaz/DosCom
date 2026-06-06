package com.devenlucaz.doscom.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

class SpeechBubble @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val textView: TextView

    init {
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f * context.resources.displayMetrics.density
            setColor(Color.WHITE)
            setStroke((2 * context.resources.displayMetrics.density).toInt(), Color.BLACK)
        }
        background = backgroundDrawable

        val padding = (12 * context.resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)

        textView = TextView(context).apply {
            setTextColor(Color.BLACK)
            textSize = 14f
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        addView(textView)
    }

    fun setText(text: String) {
        textView.text = text
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        alpha = 0f
        ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
            duration = 200
            start()
        }

        postDelayed({
            ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
                duration = 200
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        try {
                            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                            windowManager.removeView(this@SpeechBubble)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
                start()
            }
        }, 4000)
    }
}
