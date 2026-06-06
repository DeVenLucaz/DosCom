package com.devenlucaz.doscom.character

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

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

        // Antenna line
        canvas.drawLine(w * 0.5f, h * 0.1f, w * 0.5f, h * 0.3f, linePaint)
        // Antenna ball
        canvas.drawCircle(w * 0.5f, h * 0.1f, w * 0.05f, bodyPaint)

        // Body
        val bodyRect = RectF(w * 0.2f, h * 0.3f, w * 0.8f, h * 0.8f)
        canvas.drawRoundRect(bodyRect, w * 0.1f, w * 0.1f, bodyPaint)

        // Left Eye
        canvas.drawCircle(w * 0.35f, h * 0.5f, w * 0.1f, eyePaint)
        canvas.drawCircle(w * 0.35f, h * 0.5f, w * 0.05f, pupilPaint)

        // Right Eye
        canvas.drawCircle(w * 0.65f, h * 0.5f, w * 0.1f, eyePaint)
        canvas.drawCircle(w * 0.65f, h * 0.5f, w * 0.05f, pupilPaint)

        // Legs
        val legWidth = w * 0.15f
        canvas.drawRect(w * 0.3f, h * 0.8f, w * 0.3f + legWidth, h * 0.95f, bodyPaint)
        canvas.drawRect(w * 0.55f, h * 0.8f, w * 0.55f + legWidth, h * 0.95f, bodyPaint)
    }
}
