package com.devenlucaz.doscom.character

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Choreographer
import android.view.View
import android.util.AttributeSet

class CompanionRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Choreographer.FrameCallback {

    // --- V1 COMPATIBILITY STUBS ---
    var currentState: CharacterState = CharacterState.IDLE_BOB
        private set
    var currentFrame: Int = 0
        private set
    fun setState(characterState: CharacterState) {
        currentState = characterState
        currentFrame = 0
    }
    fun nextFrame() {
        currentFrame = (currentFrame + 1) % 4
    }
    // ------------------------------

    // Phase 2b: Animation State
    var state = AnimationState()

    var antennaColor: Int = Color.WHITE

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C8C8C8")
        setShadowLayer(8f, 0f, 4f, Color.argb(51, 0, 0, 0)) 
    }
    
    private val shadowlessBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C8C8C8")
    }

    private val eyeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
    }

    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB3B3")
    }

    private val tonguePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6B6B")
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    private val propPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        
        // --- Proportions Base ---
        val headH = 0.38f * h
        val bodyH = 0.32f * h
        val groupH = headH + 2f + bodyH
        val startY = cy - groupH / 2f
        
        val headW = 0.55f * w
        val headY = startY + headH / 2f
        val bodyW = 0.45f * w
        val bodyY = startY + headH + 2f + bodyH / 2f
        
        // Transform for entire robot
        canvas.save()
        canvas.translate(state.bodyOffsetX, state.bodyOffsetY)
        canvas.scale(state.scaleX * state.scale, state.scale, cx, cy)
        canvas.rotate(state.bodyRotation, cx, cy)

        // Antenna
        linePaint.color = Color.parseColor("#1A1A1A")
        linePaint.strokeWidth = 3f
        val antennaLen = h * 0.08f
        canvas.drawLine(cx, headY - headH / 2f, cx, headY - headH / 2f - antennaLen, linePaint)
        
        propPaint.color = antennaColor
        propPaint.alpha = (255 * state.antennaGlow).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, headY - headH / 2f - antennaLen, w * 0.03f, propPaint)
        propPaint.alpha = 255
        
        // Legs
        val legW = 0.12f * w
        val legH = 0.22f * h
        val legLeftX = cx - 0.10f * w
        val legRightX = cx + 0.10f * w
        val legY = bodyY + bodyH / 2f
        
        // Left Leg
        canvas.save()
        canvas.rotate(state.leftLegAngle, legLeftX, legY)
        canvas.drawRoundRect(legLeftX - legW/2f, legY, legLeftX + legW/2f, legY + legH, h * 0.05f, h * 0.05f, shadowlessBodyPaint)
        canvas.restore()

        // Right Leg
        canvas.save()
        canvas.rotate(state.rightLegAngle, legRightX, legY)
        canvas.drawRoundRect(legRightX - legW/2f, legY, legRightX + legW/2f, legY + legH, h * 0.05f, h * 0.05f, shadowlessBodyPaint)
        canvas.restore()
        
        // Arms
        val armW = 0.10f * w
        val armH = 0.26f * h
        val armLeftX = cx - bodyW / 2f - armW / 2f
        val armRightX = cx + bodyW / 2f + armW / 2f
        val armY = bodyY - bodyH / 4f
        
        // Left Arm
        canvas.save()
        canvas.rotate(state.leftArmAngle, armLeftX, armY)
        canvas.drawRoundRect(armLeftX - armW/2f, armY, armLeftX + armW/2f, armY + armH, h * 0.04f, h * 0.04f, shadowlessBodyPaint)
        val handRadius = 0.07f * w
        canvas.drawCircle(armLeftX, armY + armH, handRadius, shadowlessBodyPaint)
        canvas.restore()

        // Right Arm
        canvas.save()
        canvas.rotate(state.rightArmAngle, armRightX, armY)
        canvas.drawRoundRect(armRightX - armW/2f, armY, armRightX + armW/2f, armY + armH, h * 0.04f, h * 0.04f, shadowlessBodyPaint)
        canvas.drawCircle(armRightX, armY + armH, handRadius, shadowlessBodyPaint)
        canvas.restore()

        // Body
        canvas.drawRoundRect(cx - bodyW/2f, bodyY - bodyH/2f, cx + bodyW/2f, bodyY + bodyH/2f, h * 0.08f, h * 0.08f, bodyPaint)
        
        // Head
        canvas.drawRoundRect(cx - headW/2f, headY - headH/2f, cx + headW/2f, headY + headH/2f, h * 0.15f, h * 0.15f, bodyPaint)
        
        // Eyes
        val eyeRadius = 0.08f * w
        val eyeLeftX = cx - 0.12f * w
        val eyeRightX = cx + 0.12f * w
        val eyeY = headY - 0.02f * h
        
        if (state.eyesClosed) {
            linePaint.color = Color.parseColor("#1A1A1A")
            canvas.drawLine(eyeLeftX - eyeRadius, eyeY, eyeLeftX + eyeRadius, eyeY, linePaint)
            canvas.drawLine(eyeRightX - eyeRadius, eyeY, eyeRightX + eyeRadius, eyeY, linePaint)
        } else {
            canvas.drawCircle(eyeLeftX, eyeY, eyeRadius, eyeWhitePaint)
            canvas.drawCircle(eyeRightX, eyeY, eyeRadius, eyeWhitePaint)
            
            if (state.eyesHalf) {
                propPaint.color = Color.parseColor("#C8C8C8")
                canvas.drawRect(eyeLeftX - eyeRadius, eyeY - eyeRadius, eyeLeftX + eyeRadius, eyeY, propPaint)
                canvas.drawRect(eyeRightX - eyeRadius, eyeY - eyeRadius, eyeRightX + eyeRadius, eyeY, propPaint)
                canvas.drawLine(eyeLeftX - eyeRadius, eyeY, eyeLeftX + eyeRadius, eyeY, linePaint)
                canvas.drawLine(eyeRightX - eyeRadius, eyeY, eyeRightX + eyeRadius, eyeY, linePaint)
            }
            
            val pupilRadius = if (state.eyesWide) 0.06f * w else 0.04f * w
            canvas.drawCircle(eyeLeftX + state.pupilOffsetX, eyeY + state.pupilOffsetY, pupilRadius, pupilPaint)
            canvas.drawCircle(eyeRightX + state.pupilOffsetX, eyeY + state.pupilOffsetY, pupilRadius, pupilPaint)
        }
        
        // Blush
        if (state.blushVisible) {
            val blushW = 0.08f * w
            val blushH = 0.04f * h
            val blushY = eyeY + 0.08f * h
            canvas.drawOval(RectF(cx - 0.20f * w - blushW/2f, blushY - blushH/2f, cx - 0.20f * w + blushW/2f, blushY + blushH/2f), blushPaint)
            canvas.drawOval(RectF(cx + 0.20f * w - blushW/2f, blushY - blushH/2f, cx + 0.20f * w + blushW/2f, blushY + blushH/2f), blushPaint)
        }
        
        // Mouth
        val mouthY = headY + 0.10f * h
        linePaint.color = Color.parseColor("#1A1A1A")
        linePaint.strokeWidth = 3f
        
        if (state.mouthOpen) {
            propPaint.color = Color.parseColor("#1A1A1A")
            canvas.drawOval(RectF(cx - 0.04f * w, mouthY, cx + 0.04f * w, mouthY + 0.06f * h), propPaint)
            if (state.tongueOut) {
                canvas.drawRoundRect(cx - 0.02f * w, mouthY + 0.04f * h, cx + 0.02f * w, mouthY + 0.08f * h, 4f, 4f, tonguePaint)
            }
        } else {
            val mouthW = 0.06f * w
            when (state.mouthExpression) {
                0 -> canvas.drawLine(cx - mouthW/2f, mouthY, cx + mouthW/2f, mouthY, linePaint)
                1 -> canvas.drawArc(RectF(cx - mouthW/2f, mouthY - 0.02f * h, cx + mouthW/2f, mouthY + 0.02f * h), 0f, 180f, false, linePaint)
                2 -> canvas.drawArc(RectF(cx - mouthW/2f, mouthY - 0.02f * h, cx + mouthW/2f, mouthY + 0.02f * h), 180f, 180f, false, linePaint)
            }
            if (state.tongueOut) {
                canvas.drawRoundRect(cx - 0.02f * w, mouthY, cx + 0.02f * w, mouthY + 0.04f * h, 4f, 4f, tonguePaint)
            }
        }
        
        // Props
        if (state.activeProp != PropType.NONE) {
            drawProp(canvas, cx, headY, headW, headH, armLeftX, armY, w, h)
        }

        // Restore global transform
        canvas.restore()
    }
    
    private fun drawProp(canvas: Canvas, cx: Float, headY: Float, headW: Float, headH: Float, armX: Float, armY: Float, w: Float, h: Float) {
        when (state.activeProp) {
            PropType.PARTY_HAT -> {
                propPaint.color = Color.parseColor("#FF5252")
                val path = android.graphics.Path()
                path.moveTo(cx, headY - headH/2f - 0.2f * h)
                path.lineTo(cx - 0.15f * w, headY - headH/2f)
                path.lineTo(cx + 0.15f * w, headY - headH/2f)
                path.close()
                canvas.drawPath(path, propPaint)
                propPaint.color = Color.parseColor("#FFEB3B")
                canvas.drawCircle(cx, headY - headH/2f - 0.2f * h, 0.04f * w, propPaint)
            }
            PropType.PILOT_HAT -> {
                propPaint.color = Color.parseColor("#795548")
                canvas.drawRoundRect(cx - 0.25f * w, headY - headH/2f - 0.1f * h, cx + 0.25f * w, headY - headH/2f + 0.05f * h, 8f, 8f, propPaint)
            }
            PropType.DETECTIVE_HAT -> {
                propPaint.color = Color.parseColor("#8D6E63")
                canvas.drawRoundRect(cx - 0.2f * w, headY - headH/2f - 0.15f * h, cx + 0.2f * w, headY - headH/2f, 12f, 12f, propPaint)
                canvas.drawRect(cx - 0.3f * w, headY - headH/2f, cx + 0.3f * w, headY - headH/2f + 0.02f * h, propPaint)
            }
            PropType.OVERSIZED_GLASSES -> {
                linePaint.color = Color.parseColor("#1A1A1A")
                linePaint.strokeWidth = 6f
                val glassRadius = 0.15f * w
                canvas.drawCircle(cx - 0.15f * w, headY, glassRadius, linePaint)
                canvas.drawCircle(cx + 0.15f * w, headY, glassRadius, linePaint)
                canvas.drawLine(cx - 0.05f * w, headY, cx + 0.05f * w, headY, linePaint)
                linePaint.strokeWidth = 3f
            }
            PropType.BOOMBOX -> {
                propPaint.color = Color.parseColor("#424242")
                val bbY = armY + 0.1f * h
                canvas.drawRoundRect(cx - 0.2f * w, bbY, cx + 0.2f * w, bbY + 0.2f * h, 8f, 8f, propPaint)
                propPaint.color = Color.parseColor("#9E9E9E")
                canvas.drawCircle(cx - 0.1f * w, bbY + 0.1f * h, 0.06f * w, propPaint)
                canvas.drawCircle(cx + 0.1f * w, bbY + 0.1f * h, 0.06f * w, propPaint)
            }
            else -> {}
        }
    }
}
