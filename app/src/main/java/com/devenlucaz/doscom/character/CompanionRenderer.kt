package com.devenlucaz.doscom.character

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.os.Handler
import android.os.Looper

class CompanionRenderer(context: Context) : View(context) {

    var currentState: CharacterState = CharacterState.IDLE_BOB
        private set
    
    var currentFrame: Int = 0
        private set

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        color = Color.DKGRAY 
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    private val handler = Handler(Looper.getMainLooper())
    private val animationRunnable = object : Runnable {
        override fun run() {
            nextFrame()
            invalidate()
            handler.postDelayed(this, 150L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(animationRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(animationRunnable)
    }

    fun setState(state: CharacterState) {
        currentState = state
        currentFrame = 0
    }

    fun nextFrame() {
        currentFrame++
        if (currentFrame > 3) {
            currentFrame = 0
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        var bodyOffsetY = 0f
        if (currentState == CharacterState.IDLE_BOB) {
            bodyOffsetY = (Math.sin(System.currentTimeMillis() / 200.0) * (h * 0.05f)).toFloat()
        }

        canvas.save()
        canvas.translate(0f, bodyOffsetY)

        // Antenna line
        canvas.drawLine(w * 0.5f, h * 0.1f, w * 0.5f, h * 0.3f, linePaint)
        // Antenna ball
        if (currentState == CharacterState.LISTEN) {
            val blinkColor = if (System.currentTimeMillis() % 400 < 200) Color.RED else Color.LTGRAY
            val antennaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = blinkColor }
            canvas.drawCircle(w * 0.5f, h * 0.1f, w * 0.05f, antennaPaint)
        } else {
            canvas.drawCircle(w * 0.5f, h * 0.1f, w * 0.05f, bodyPaint)
        }

        // Body
        val bodyRect = RectF(w * 0.2f, h * 0.3f, w * 0.8f, h * 0.8f)
        canvas.drawRoundRect(bodyRect, w * 0.1f, w * 0.1f, bodyPaint)

        val eyeCy = h * 0.5f

        if (currentState == CharacterState.IDLE_BLINK && currentFrame == 3) {
            canvas.drawLine(w * 0.25f, eyeCy, w * 0.45f, eyeCy, linePaint)
            canvas.drawLine(w * 0.55f, eyeCy, w * 0.75f, eyeCy, linePaint)
        } else {
            var leftPupilX = w * 0.35f
            var rightPupilX = w * 0.65f

            when (currentState) {
                CharacterState.IDLE_LOOK_LEFT -> {
                    leftPupilX -= w * 0.03f
                    rightPupilX -= w * 0.03f
                }
                CharacterState.IDLE_LOOK_RIGHT -> {
                    leftPupilX += w * 0.03f
                    rightPupilX += w * 0.03f
                }
                else -> {}
            }

            // Left Eye
            canvas.drawCircle(w * 0.35f, eyeCy, w * 0.1f, eyePaint)
            canvas.drawCircle(leftPupilX, eyeCy, w * 0.05f, pupilPaint)

            // Right Eye
            canvas.drawCircle(w * 0.65f, eyeCy, w * 0.1f, eyePaint)
            canvas.drawCircle(rightPupilX, eyeCy, w * 0.05f, pupilPaint)
        }

        canvas.restore()

        // Legs
        val legWidth = w * 0.15f
        canvas.drawRect(w * 0.3f, h * 0.8f, w * 0.3f + legWidth, h * 0.95f, bodyPaint)
        canvas.drawRect(w * 0.55f, h * 0.8f, w * 0.55f + legWidth, h * 0.95f, bodyPaint)
    }
}
