package com.devenlucaz.doscom.character

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.util.AttributeSet

class CompanionRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var state = AnimationState()
    var zzzParticles = listOf<com.devenlucaz.doscom.animation.ZzzParticle>()

    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#080818") // Dark navy
    }

    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#20FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF") // Cyan
        style = Paint.Style.FILL
        setShadowLayer(20f, 0f, 0f, Color.parseColor("#00E5FF"))
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#00E5FF"))
    }

    private val galaxyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        alpha = (255 * 0.15f).toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        setShadowLayer(8f, 0f, 0f, Color.parseColor("#00E5FF"))
    }

    private var lastW = 0f
    private var lastH = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val density = context.resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        
        val charW = 80f * density
        val charH = 80f * density

        if (w != lastW || h != lastH) {
            lastW = w
            lastH = h
            inkPaint.shader = RadialGradient(
                cx - charW * 0.2f, cy - charH * 0.2f,
                charW * 1.5f,
                intArrayOf(Color.parseColor("#2A2A4A"), Color.parseColor("#080818")),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        
        canvas.save()
        canvas.translate(state.bodyOffsetX, state.bodyOffsetY)
        canvas.scale(state.scaleX * state.scale, state.scale, cx, cy)
        canvas.rotate(state.bodyRotation, cx, cy)

        val bodyRadiusX = charW * 0.45f
        val bodyRadiusY = charH * 0.5f

        // Limbs
        val armLen = charH * 0.45f
        val shoulderLeftX = cx - bodyRadiusX * 0.7f
        val shoulderRightX = cx + bodyRadiusX * 0.7f
        val shoulderY = cy - charH * 0.1f

        val legLen = charH * 0.4f
        val hipLeftX = cx - bodyRadiusX * 0.35f
        val hipRightX = cx + bodyRadiusX * 0.35f
        val hipY = cy + bodyRadiusY * 0.7f
        
        // Draw Legs
        drawExtrudedStub(canvas, hipLeftX, hipY, state.leftLegAngle, legLen, charW * 0.22f)
        drawExtrudedStub(canvas, hipRightX, hipY, state.rightLegAngle, legLen, charW * 0.22f)
        
        // Draw Arms
        drawExtrudedStub(canvas, shoulderLeftX, shoulderY, state.leftArmAngle, armLen, charW * 0.2f)
        drawExtrudedStub(canvas, shoulderRightX, shoulderY, state.rightArmAngle, armLen, charW * 0.2f)

        // Draw Teardrop Body
        val headTopY = cy - charH * 0.75f
        val bodyPath = Path()
        bodyPath.moveTo(cx, headTopY)
        bodyPath.cubicTo(
            cx - bodyRadiusX * 1.3f, cy - charH * 0.2f, 
            cx - bodyRadiusX * 1.1f, cy + bodyRadiusY,
            cx, cy + bodyRadiusY
        )
        bodyPath.cubicTo(
            cx + bodyRadiusX * 1.1f, cy + bodyRadiusY,
            cx + bodyRadiusX * 1.3f, cy - charH * 0.2f,
            cx, headTopY
        )
        canvas.drawPath(bodyPath, inkPaint)

        // Glossy Sheen Highlight on body
        val glossPath = Path()
        glossPath.moveTo(cx - bodyRadiusX * 0.5f, cy - charH * 0.4f)
        glossPath.quadTo(cx - bodyRadiusX * 0.8f, cy, cx - bodyRadiusX * 0.3f, cy + bodyRadiusY * 0.6f)
        canvas.drawPath(glossPath, glossPaint)

        // Swirling Galaxy Pattern on Torso
        canvas.save()
        canvas.translate(cx, cy + charH * 0.15f)
        for (i in 0 until 4) {
            val swirlPath = Path()
            var radius = 2f
            var angle = 0f
            swirlPath.moveTo(radius, 0f)
            val steps = 25
            val maxRadius = bodyRadiusX * 0.55f
            for (j in 1..steps) {
                angle += 0.4f
                radius += (maxRadius - 2f) / steps
                val sx = (Math.cos(angle.toDouble()) * radius).toFloat()
                val sy = (Math.sin(angle.toDouble()) * radius).toFloat()
                swirlPath.lineTo(sx, sy)
            }
            canvas.drawPath(swirlPath, galaxyPaint)
            canvas.rotate(90f)
        }
        canvas.restore()

        // Face
        val eyeW = charW * 0.16f
        val eyeH = charH * 0.18f
        val eyeLeftX = cx - charW * 0.22f
        val eyeRightX = cx + charW * 0.22f
        val eyeY = cy - charH * 0.15f
        
        if (state.eyesClosed) {
            canvas.drawLine(eyeLeftX - eyeW/1.5f, eyeY, eyeLeftX + eyeW/1.5f, eyeY, linePaint)
            canvas.drawLine(eyeRightX - eyeW/1.5f, eyeY, eyeRightX + eyeW/1.5f, eyeY, linePaint)
        } else {
            canvas.drawOval(RectF(eyeLeftX - eyeW/2f, eyeY - eyeH/2f, eyeLeftX + eyeW/2f, eyeY + eyeH/2f), eyePaint)
            canvas.drawOval(RectF(eyeRightX - eyeW/2f, eyeY - eyeH/2f, eyeRightX + eyeW/2f, eyeY + eyeH/2f), eyePaint)
            
            if (state.eyesHalf) {
                val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#080818") }
                canvas.drawRect(eyeLeftX - eyeW, eyeY - eyeH, eyeLeftX + eyeW, eyeY, lidPaint)
                canvas.drawRect(eyeRightX - eyeW, eyeY - eyeH, eyeRightX + eyeW, eyeY, lidPaint)
                canvas.drawLine(eyeLeftX - eyeW/1.5f, eyeY, eyeLeftX + eyeW/1.5f, eyeY, linePaint)
                canvas.drawLine(eyeRightX - eyeW/1.5f, eyeY, eyeRightX + eyeW/1.5f, eyeY, linePaint)
            }
        }
        
        // Mouth
        val mouthY = cy + charH * 0.08f
        val mouthW = charW * 0.08f
        
        if (state.mouthOpen) {
            canvas.drawOval(RectF(cx - mouthW, mouthY - mouthW/2f, cx + mouthW, mouthY + mouthW/2f), linePaint)
        } else {
            when (state.mouthExpression) {
                0 -> canvas.drawLine(cx - mouthW, mouthY, cx + mouthW, mouthY, linePaint)
                1 -> canvas.drawArc(RectF(cx - mouthW, mouthY - mouthW/1.5f, cx + mouthW, mouthY + mouthW/1.5f), 0f, 180f, false, linePaint)
                2 -> canvas.drawArc(RectF(cx - mouthW, mouthY - mouthW/1.5f, cx + mouthW, mouthY + mouthW/1.5f), 180f, 180f, false, linePaint)
            }
        }

        // Zzz Particles
        if (zzzParticles.isNotEmpty()) {
            textPaint.textSize = charH * 0.15f
            for (p in zzzParticles) {
                textPaint.alpha = p.alpha
                canvas.drawText("Z", cx + charW * 0.3f + p.x, cy - charH * 0.5f + p.y, textPaint)
            }
        }

        canvas.restore()
    }
    
    private fun drawExtrudedStub(canvas: Canvas, rootX: Float, rootY: Float, angle: Float, length: Float, width: Float) {
        canvas.save()
        canvas.rotate(angle, rootX, rootY)
        
        val path = Path()
        path.moveTo(rootX - width / 2f, rootY)
        path.lineTo(rootX - width / 2f, rootY + length - width / 2f)
        path.arcTo(RectF(rootX - width / 2f, rootY + length - width, rootX + width / 2f, rootY + length), 180f, -180f, false)
        path.lineTo(rootX + width / 2f, rootY)
        path.close()
        
        canvas.drawPath(path, inkPaint)
        
        // Glossy line
        canvas.drawLine(rootX - width * 0.15f, rootY + width * 0.5f, rootX - width * 0.15f, rootY + length - width * 0.6f, glossPaint)
        
        canvas.restore()
    }
}
