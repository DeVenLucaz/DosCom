package com.devenlucaz.doscom.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
    private var isAboveRobot = true

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC0D0D1A") // 80% opacity
        style = Paint.Style.FILL
    }
    
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1AFFFFFF") // 10% opacity
        style = Paint.Style.STROKE
        strokeWidth = 1f * context.resources.displayMetrics.density
    }

    private val path = Path()
    private val tailSize = 12f * context.resources.displayMetrics.density
    private val cornerRadius = 16f * context.resources.displayMetrics.density

    init {
        setWillNotDraw(false)
        val density = context.resources.displayMetrics.density
        val padH = (14 * density).toInt()
        val padV = (10 * density).toInt()
        
        // Add extra padding on top or bottom for the tail to draw
        val extraPad = tailSize.toInt()
        setPadding(padH, padV + extraPad, padH, padV + extraPad) // padding handles both cases safely

        textView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            maxWidth = (200 * density).toInt()
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

    fun setDirection(isAbove: Boolean) {
        isAboveRobot = isAbove
        val padH = (14 * context.resources.displayMetrics.density).toInt()
        val padV = (10 * context.resources.displayMetrics.density).toInt()
        val extraPad = tailSize.toInt()
        if (isAbove) {
            setPadding(padH, padV, padH, padV + extraPad)
        } else {
            setPadding(padH, padV + extraPad, padH, padV)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val w = width.toFloat()
        val h = height.toFloat()
        val strokeHalf = borderPaint.strokeWidth / 2f
        
        path.reset()
        
        val contentTop = if (isAboveRobot) strokeHalf else tailSize + strokeHalf
        val contentBottom = if (isAboveRobot) h - tailSize - strokeHalf else h - strokeHalf
        val contentLeft = strokeHalf
        val contentRight = w - strokeHalf
        
        // Draw rounded rectangle
        path.addRoundRect(contentLeft, contentTop, contentRight, contentBottom, cornerRadius, cornerRadius, Path.Direction.CW)
        
        // Draw tail
        val tailBaseX = w / 2f
        if (isAboveRobot) {
            // Tail points down
            path.moveTo(tailBaseX - tailSize, contentBottom)
            path.lineTo(tailBaseX, contentBottom + tailSize)
            path.lineTo(tailBaseX + tailSize, contentBottom)
        } else {
            // Tail points up
            path.moveTo(tailBaseX - tailSize, contentTop)
            path.lineTo(tailBaseX, contentTop - tailSize)
            path.lineTo(tailBaseX + tailSize, contentTop)
        }
        
        canvas.drawPath(path, bgPaint)
        canvas.drawPath(path, borderPaint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        val slideOffset = if (isAboveRobot) 50f else -50f
        translationY = slideOffset
        alpha = 0f
        
        animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(200)
            .start()

        postDelayed({
            animate()
                .alpha(0f)
                .setDuration(150)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        try {
                            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                            windowManager.removeView(this@SpeechBubble)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
                .start()
        }, 4000)
        
        setOnClickListener {
            animate().cancel()
            try {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.removeView(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
