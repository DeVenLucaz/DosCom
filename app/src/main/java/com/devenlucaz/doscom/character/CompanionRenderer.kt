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

    var antennaColor: Int = Color.parseColor("#00E5FF")

    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#080812") // Deep dark blue/black
        setShadowLayer(20f, 0f, 0f, Color.parseColor("#4000E5FF")) // Cosmic glow
    }

    private val nebulaPaint = Paint(Paint.ANTI_ALIAS_FLAG) // We will set shader in onDraw

    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF") // Neon cyan
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#00E5FF")) // High luminosity
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        setShadowLayer(10f, 0f, 0f, Color.parseColor("#00E5FF"))
    }

    private val tipGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        setShadowLayer(8f, 0f, 0f, Color.parseColor("#FFFFFF"))
    }

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
        
        canvas.save()
        canvas.translate(state.bodyOffsetX, state.bodyOffsetY)
        canvas.scale(state.scaleX * state.scale, state.scale, cx, cy)
        canvas.rotate(state.bodyRotation, cx, cy)

        val bodyRadiusX = charW * 0.35f
        val bodyRadiusY = charH * 0.40f

        // Limbs
        val armLen = charH * 0.35f
        val shoulderLeftX = cx - bodyRadiusX * 0.6f
        val shoulderRightX = cx + bodyRadiusX * 0.6f
        val shoulderY = cy - bodyRadiusY * 0.2f

        val legLen = charH * 0.3f
        val hipLeftX = cx - bodyRadiusX * 0.4f
        val hipRightX = cx + bodyRadiusX * 0.4f
        val hipY = cy + bodyRadiusY * 0.5f
        
        // Draw Legs
        drawExtrudedLimb(canvas, hipLeftX, hipY, state.leftLegAngle, legLen, charW * 0.15f)
        drawExtrudedLimb(canvas, hipRightX, hipY, state.rightLegAngle, legLen, charW * 0.15f)
        
        // Draw Arms
        drawExtrudedLimb(canvas, shoulderLeftX, shoulderY, state.leftArmAngle, armLen, charW * 0.15f)
        drawExtrudedLimb(canvas, shoulderRightX, shoulderY, state.rightArmAngle, armLen, charW * 0.15f)

        // Draw Main Body (Blob)
        val bodyPath = Path()
        // Squishy egg shape
        bodyPath.moveTo(cx, cy - bodyRadiusY)
        bodyPath.cubicTo(
            cx + bodyRadiusX, cy - bodyRadiusY,
            cx + bodyRadiusX * 1.2f, cy + bodyRadiusY * 0.8f,
            cx, cy + bodyRadiusY
        )
        bodyPath.cubicTo(
            cx - bodyRadiusX * 1.2f, cy + bodyRadiusY * 0.8f,
            cx - bodyRadiusX, cy - bodyRadiusY,
            cx, cy - bodyRadiusY
        )
        canvas.drawPath(bodyPath, inkPaint)

        // Cosmic Nebula Core
        val nebulaRadius = bodyRadiusX * 0.8f
        nebulaPaint.shader = RadialGradient(
            cx, cy, nebulaRadius,
            intArrayOf(Color.parseColor("#408A2BE2"), Color.parseColor("#2000E5FF"), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, nebulaRadius, nebulaPaint)

        // Glossy Outer Layer Highlight
        val glossPath = Path()
        glossPath.moveTo(cx - bodyRadiusX * 0.5f, cy - bodyRadiusY * 0.7f)
        glossPath.quadTo(cx, cy - bodyRadiusY * 0.9f, cx + bodyRadiusX * 0.5f, cy - bodyRadiusY * 0.7f)
        canvas.drawPath(glossPath, glossPaint)

        // Face
        val eyeRadius = charW * 0.09f
        val eyeLeftX = cx - charW * 0.15f
        val eyeRightX = cx + charW * 0.15f
        val eyeY = cy - charH * 0.1f
        
        if (state.eyesClosed) {
            canvas.drawLine(eyeLeftX - eyeRadius, eyeY, eyeLeftX + eyeRadius, eyeY, linePaint)
            canvas.drawLine(eyeRightX - eyeRadius, eyeY, eyeRightX + eyeRadius, eyeY, linePaint)
        } else {
            canvas.drawCircle(eyeLeftX, eyeY, eyeRadius, eyePaint)
            canvas.drawCircle(eyeRightX, eyeY, eyeRadius, eyePaint)
            
            if (state.eyesHalf) {
                // Eyelids matching body color
                val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#080812") }
                canvas.drawRect(eyeLeftX - eyeRadius*1.5f, eyeY - eyeRadius*1.5f, eyeLeftX + eyeRadius*1.5f, eyeY, lidPaint)
                canvas.drawRect(eyeRightX - eyeRadius*1.5f, eyeY - eyeRadius*1.5f, eyeRightX + eyeRadius*1.5f, eyeY, lidPaint)
                canvas.drawLine(eyeLeftX - eyeRadius, eyeY, eyeLeftX + eyeRadius, eyeY, linePaint)
                canvas.drawLine(eyeRightX - eyeRadius, eyeY, eyeRightX + eyeRadius, eyeY, linePaint)
            }
        }
        
        // Mouth
        val mouthY = cy + charH * 0.05f
        val mouthW = charW * 0.08f
        
        if (state.mouthOpen) {
            canvas.drawOval(RectF(cx - mouthW/2f, mouthY - mouthW/3f, cx + mouthW/2f, mouthY + mouthW/3f), linePaint)
        } else {
            when (state.mouthExpression) {
                0 -> canvas.drawLine(cx - mouthW/2f, mouthY, cx + mouthW/2f, mouthY, linePaint) // Simple flat line
                1 -> canvas.drawArc(RectF(cx - mouthW/2f, mouthY - mouthW/4f, cx + mouthW/2f, mouthY + mouthW/4f), 0f, 180f, false, linePaint) // Smile
                2 -> canvas.drawArc(RectF(cx - mouthW/2f, mouthY - mouthW/4f, cx + mouthW/2f, mouthY + mouthW/4f), 180f, 180f, false, linePaint) // Sad
            }
        }

        // Zzz Particles
        if (zzzParticles.isNotEmpty()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = charH * 0.12f
                color = Color.parseColor("#00E5FF")
                setShadowLayer(8f, 0f, 0f, Color.parseColor("#00E5FF"))
            }
            for (p in zzzParticles) {
                textPaint.alpha = p.alpha
                canvas.drawText("Z", cx + charW * 0.2f + p.x, cy - charH * 0.4f + p.y, textPaint)
            }
        }

        canvas.restore()
    }
    
    private fun drawExtrudedLimb(canvas: Canvas, rootX: Float, rootY: Float, angle: Float, length: Float, rootWidth: Float) {
        canvas.save()
        canvas.rotate(angle, rootX, rootY)
        
        val path = Path()
        path.moveTo(rootX - rootWidth / 2f, rootY)
        
        // Curve down to a sharp tip
        path.quadTo(
            rootX - rootWidth / 4f, rootY + length * 0.5f,
            rootX, rootY + length
        )
        
        // Curve back up to the other side of the root
        path.quadTo(
            rootX + rootWidth / 4f, rootY + length * 0.5f,
            rootX + rootWidth / 2f, rootY
        )
        path.close()
        
        canvas.drawPath(path, inkPaint)
        
        // Glowing starry tip
        canvas.drawCircle(rootX, rootY + length, 3f, tipGlowPaint)
        
        canvas.restore()
    }
}
