package com.devenlucaz.doscom.character

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.util.AttributeSet

class CompanionRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var state = AnimationState()
    var zzzParticles = listOf<com.devenlucaz.doscom.animation.ZzzParticle>()

    var antennaColor: Int = Color.parseColor("#00E5FF")

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E")
        setShadowLayer(16f, 0f, 0f, Color.parseColor("#6000E5FF")) 
    }
    
    private val shadowlessBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E")
    }

    private val eyeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        setShadowLayer(10f, 0f, 0f, Color.parseColor("#00E5FF"))
    }

    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66FF007F") 
    }

    private val tonguePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF007F")
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val density = context.resources.displayMetrics.density
        val w = 80f * density
        val h = 80f * density
        val cx = width.toFloat() / 2f
        val cy = height.toFloat() / 2f
        
        val headH = 0.40f * h
        val bodyH = 0.35f * h
        val groupH = headH + 2f + bodyH
        val startY = cy - groupH / 2f
        
        val headW = 0.55f * w
        val headY = startY + headH / 2f
        val bodyW = 0.45f * w
        val bodyY = startY + headH + 2f + bodyH / 2f
        
        canvas.save()
        canvas.translate(state.bodyOffsetX, state.bodyOffsetY)
        canvas.scale(state.scaleX * state.scale, state.scale, cx, cy)
        canvas.rotate(state.bodyRotation, cx, cy)

        linePaint.color = Color.parseColor("#1A1A2E")
        linePaint.strokeWidth = 6f
        val antennaLen = h * 0.08f
        val wispPath = android.graphics.Path()
        wispPath.moveTo(cx, headY - headH / 2f)
        wispPath.quadTo(cx - w * 0.05f, headY - headH / 2f - antennaLen / 2f, cx + w * 0.02f, headY - headH / 2f - antennaLen)
        canvas.drawPath(wispPath, linePaint)
        
        propPaint.color = antennaColor
        propPaint.alpha = (255 * state.antennaGlow).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx + w * 0.02f, headY - headH / 2f - antennaLen, w * 0.04f, propPaint)
        propPaint.alpha = 255
        
        val legW = 0.14f * w
        val legH = 0.20f * h
        val legLeftX = cx - 0.12f * w
        val legRightX = cx + 0.12f * w
        val legY = bodyY + bodyH / 2f
        
        canvas.save()
        canvas.rotate(state.leftLegAngle, legLeftX, legY)
        canvas.drawRoundRect(legLeftX - legW/2f, legY, legLeftX + legW/2f, legY + legH, legW, legW, shadowlessBodyPaint)
        canvas.restore()

        canvas.save()
        canvas.rotate(state.rightLegAngle, legRightX, legY)
        canvas.drawRoundRect(legRightX - legW/2f, legY, legRightX + legW/2f, legY + legH, legW, legW, shadowlessBodyPaint)
        canvas.restore()
        
        val armW = 0.12f * w
        val armH = 0.26f * h
        val armLeftX = cx - bodyW / 2f - armW / 3f
        val armRightX = cx + bodyW / 2f + armW / 3f
        val armY = bodyY - bodyH / 4f
        
        canvas.save()
        canvas.rotate(state.leftArmAngle, armLeftX, armY)
        canvas.drawRoundRect(armLeftX - armW/2f, armY, armLeftX + armW/2f, armY + armH, armW, armW, shadowlessBodyPaint)
        canvas.restore()

        canvas.save()
        canvas.rotate(state.rightArmAngle, armRightX, armY)
        canvas.drawRoundRect(armRightX - armW/2f, armY, armRightX + armW/2f, armY + armH, armW, armW, shadowlessBodyPaint)
        canvas.restore()

        canvas.drawOval(RectF(cx - bodyW/2f, bodyY - bodyH/2f, cx + bodyW/2f, bodyY + bodyH/2f), bodyPaint)
        canvas.drawRect(cx - bodyW/2.5f, headY, cx + bodyW/2.5f, bodyY, bodyPaint)
        canvas.drawOval(RectF(cx - headW/2f, headY - headH/2f, cx + headW/2f, headY + headH/2f), bodyPaint)
        
        val eyeRadius = 0.08f * w
        val eyeLeftX = cx - 0.12f * w
        val eyeRightX = cx + 0.12f * w
        val eyeY = headY - 0.02f * h
        
        if (state.eyesClosed) {
            linePaint.color = Color.parseColor("#00E5FF")
            linePaint.strokeWidth = 3f
            canvas.drawLine(eyeLeftX - eyeRadius, eyeY, eyeLeftX + eyeRadius, eyeY, linePaint)
            canvas.drawLine(eyeRightX - eyeRadius, eyeY, eyeRightX + eyeRadius, eyeY, linePaint)
        } else {
            canvas.drawCircle(eyeLeftX, eyeY, eyeRadius, eyeWhitePaint)
            canvas.drawCircle(eyeRightX, eyeY, eyeRadius, eyeWhitePaint)
            
            if (state.eyesHalf) {
                propPaint.color = Color.parseColor("#1A1A2E")
                canvas.drawRect(eyeLeftX - eyeRadius, eyeY - eyeRadius, eyeLeftX + eyeRadius, eyeY, propPaint)
                canvas.drawRect(eyeRightX - eyeRadius, eyeY - eyeRadius, eyeRightX + eyeRadius, eyeY, propPaint)
                linePaint.color = Color.parseColor("#00E5FF")
                linePaint.strokeWidth = 3f
                canvas.drawLine(eyeLeftX - eyeRadius, eyeY, eyeLeftX + eyeRadius, eyeY, linePaint)
                canvas.drawLine(eyeRightX - eyeRadius, eyeY, eyeRightX + eyeRadius, eyeY, linePaint)
            }
            
            val pupilRadius = if (state.eyesWide) 0.05f * w else 0.03f * w
            canvas.drawCircle(eyeLeftX + state.pupilOffsetX, eyeY + state.pupilOffsetY, pupilRadius, pupilPaint)
            canvas.drawCircle(eyeRightX + state.pupilOffsetX, eyeY + state.pupilOffsetY, pupilRadius, pupilPaint)
        }
        
        if (state.blushVisible) {
            val blushW = 0.08f * w
            val blushH = 0.04f * h
            val blushY = eyeY + 0.08f * h
            canvas.drawOval(RectF(cx - 0.20f * w - blushW/2f, blushY - blushH/2f, cx - 0.20f * w + blushW/2f, blushY + blushH/2f), blushPaint)
            canvas.drawOval(RectF(cx + 0.20f * w - blushW/2f, blushY - blushH/2f, cx + 0.20f * w + blushW/2f, blushY + blushH/2f), blushPaint)
        }
        
        val mouthY = headY + 0.10f * h
        linePaint.color = Color.parseColor("#00E5FF")
        linePaint.strokeWidth = 3f
        
        if (state.mouthOpen) {
            propPaint.color = Color.parseColor("#0F0F15")
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
        
        if (state.activeProp != PropType.NONE) {
            drawProp(canvas, cx, headY, headW, headH, armLeftX, armY, w, h)
        }

        if (zzzParticles.isNotEmpty()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 0.1f * h
                color = Color.parseColor("#00E5FF")
                setShadowLayer(4f, 0f, 0f, Color.parseColor("#00E5FF"))
            }
            for (p in zzzParticles) {
                textPaint.alpha = p.alpha
                canvas.drawText("Z", cx + 0.2f * w + p.x, headY - headH/2f + p.y, textPaint)
            }
        }

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
                linePaint.color = Color.parseColor("#00E5FF")
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
                propPaint.color = Color.parseColor("#00E5FF")
                canvas.drawCircle(cx - 0.1f * w, bbY + 0.1f * h, 0.06f * w, propPaint)
                canvas.drawCircle(cx + 0.1f * w, bbY + 0.1f * h, 0.06f * w, propPaint)
            }
            else -> {}
        }
    }
}
