package com.devenlucaz.doscom.character

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CompanionRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Keeping your state objects
    var state = AnimationState()
    var zzzParticles = listOf<com.devenlucaz.doscom.animation.ZzzParticle>()
    var antennaColor: Int = Color.WHITE

    // Paints
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG) // We will apply a shader to this later
    
    private val rimLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4000E5FF") // Faint cyan rim light
        style = Paint.Style.STROKE
        strokeWidth = 8f
        // maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
    }

    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60FFFFFF") // Stronger, softer white for gloss
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        // maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
    }

    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FFFF") // Brighter Cyan
        style = Paint.Style.FILL
        // Layered glow: The shadow layer handles the outer glow
        setShadowLayer(25f, 0f, 0f, Color.parseColor("#00D4FF"))
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FFFF")
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        setShadowLayer(20f, 0f, 0f, Color.parseColor("#00D4FF"))
    }

    private val galaxyCorePaint = Paint(Paint.ANTI_ALIAS_FLAG) // Shader applied later
    
    private val galaxySwirlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80B8D8FF") // Soft blue-white
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        // maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        setShadowLayer(8f, 0f, 0f, Color.parseColor("#00E5FF"))
    }

    private val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A0E1A")
    }

    private val limbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A0E1A")
    }

    private var lastW = 0f
    private var lastH = 0f

    init {
        // LAYER_TYPE_SOFTWARE causes low-res blurring on scaled canvas views.
        // Modern Android (API 28+) supports BlurMaskFilter and shadows in hardware.
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val density = context.resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        val charW = 90f * density
        val charH = 90f * density

        // 1. Setup 3D-like Gradients (Only update when size changes)
        if (w != lastW || h != lastH) {
            lastW = w
            lastH = h
            
            // Simulates a 3D sphere light from the top-left
            bodyPaint.shader = RadialGradient(
                cx - charW * 0.3f, cy - charH * 0.4f,
                charW * 1.8f,
                intArrayOf(Color.parseColor("#1B223D"), Color.parseColor("#0A0E1A"), Color.parseColor("#020408")),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )

            // Simulates the glowing core of the galaxy on the belly
            galaxyCorePaint.shader = RadialGradient(
                cx, cy + charH * 0.15f,
                charW * 0.6f,
                intArrayOf(Color.parseColor("#4000E5FF"), Color.parseColor("#10006699"), Color.TRANSPARENT),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP
            )
        }

        canvas.save()
        canvas.translate(state.bodyOffsetX, state.bodyOffsetY)
        canvas.scale(state.scaleX * state.scale, state.scale, cx, cy)
        canvas.rotate(state.bodyRotation, cx, cy)

        val bodyRadiusX = charW * 0.45f
        val bodyRadiusY = charH * 0.5f

        // 2. Draw Limbs Behind Body
        val armLen = charH * 0.45f
        val shoulderLeftX = cx - bodyRadiusX * 0.7f
        val shoulderRightX = cx + bodyRadiusX * 0.7f
        val shoulderY = cy - charH * 0.1f

        val legLen = charH * 0.4f
        val hipLeftX = cx - bodyRadiusX * 0.35f
        val hipRightX = cx + bodyRadiusX * 0.35f
        val hipY = cy + bodyRadiusY * 0.7f

        drawExtrudedStub(canvas, hipLeftX, hipY, state.leftLegAngle, legLen, charW * 0.22f)
        drawExtrudedStub(canvas, hipRightX, hipY, state.rightLegAngle, legLen, charW * 0.22f)
        drawExtrudedStub(canvas, shoulderLeftX, shoulderY, state.leftArmAngle, armLen, charW * 0.2f)
        drawExtrudedStub(canvas, shoulderRightX, shoulderY, state.rightArmAngle, armLen, charW * 0.2f)

        // 3. Draw the Main Teardrop Body
        val headTopY = cy - charH * 0.85f // Made slightly taller for better teardrop
        val bodyPath = Path()
        bodyPath.moveTo(cx, headTopY)
        bodyPath.cubicTo(
            cx - bodyRadiusX * 1.4f, cy - charH * 0.2f,
            cx - bodyRadiusX * 1.1f, cy + bodyRadiusY,
            cx, cy + bodyRadiusY
        )
        bodyPath.cubicTo(
            cx + bodyRadiusX * 1.1f, cy + bodyRadiusY,
            cx + bodyRadiusX * 1.4f, cy - charH * 0.2f,
            cx, headTopY
        )
        // Draw base body
        canvas.drawPath(bodyPath, bodyPaint)
        // Draw cyan rim lighting around the edge to give it 3D pop
        canvas.drawPath(bodyPath, rimLightPaint)

        // 4. Draw 3D Glossy Specular Highlights
        val glossPath = Path()
        // Top head highlight
        glossPath.moveTo(cx - bodyRadiusX * 0.3f, headTopY + charH * 0.2f)
        glossPath.quadTo(cx - bodyRadiusX * 0.7f, cy - charH * 0.2f, cx - bodyRadiusX * 0.8f, cy)
        // Belly highlight
        glossPath.moveTo(cx + bodyRadiusX * 0.7f, cy)
        glossPath.quadTo(cx + bodyRadiusX * 0.8f, cy + bodyRadiusY * 0.4f, cx + bodyRadiusX * 0.4f, cy + bodyRadiusY * 0.8f)
        canvas.drawPath(glossPath, glossPaint)

        // 5. Draw the Galaxy 
        // Core glow
        canvas.drawCircle(cx, cy + charH * 0.15f, charW * 0.5f, galaxyCorePaint)
        
        // Soft swirls
        canvas.save()
        canvas.translate(cx, cy + charH * 0.15f)
        for (i in 0 until 3) { // 3 arms for the galaxy
            val swirlPath = Path()
            var radius = 0f
            var angle = 0f
            swirlPath.moveTo(0f, 0f)
            val steps = 30
            val maxRadius = bodyRadiusX * 0.5f
            for (j in 1..steps) {
                angle += 0.3f
                radius += maxRadius / steps
                val sx = (Math.cos(angle.toDouble()) * radius).toFloat()
                val sy = (Math.sin(angle.toDouble()) * radius).toFloat()
                swirlPath.lineTo(sx, sy)
            }
            canvas.drawPath(swirlPath, galaxySwirlPaint)
            canvas.rotate(120f)
        }
        canvas.restore()

        // 6. Draw the Face
        val eyeW = charW * 0.18f
        val eyeH = charH * 0.20f
        val eyeLeftX = cx - charW * 0.24f
        val eyeRightX = cx + charW * 0.24f
        val eyeY = cy - charH * 0.15f

        if (state.eyesClosed) {
            canvas.drawLine(eyeLeftX - eyeW / 1.2f, eyeY, eyeLeftX + eyeW / 1.2f, eyeY, linePaint)
            canvas.drawLine(eyeRightX - eyeW / 1.2f, eyeY, eyeRightX + eyeW / 1.2f, eyeY, linePaint)
        } else {
            canvas.drawOval(RectF(eyeLeftX - eyeW / 2f, eyeY - eyeH / 2f, eyeLeftX + eyeW / 2f, eyeY + eyeH / 2f), eyePaint)
            canvas.drawOval(RectF(eyeRightX - eyeW / 2f, eyeY - eyeH / 2f, eyeRightX + eyeW / 2f, eyeY + eyeH / 2f), eyePaint)

            if (state.eyesHalf) {
                canvas.drawRect(eyeLeftX - eyeW, eyeY - eyeH, eyeLeftX + eyeW, eyeY, lidPaint)
                canvas.drawRect(eyeRightX - eyeW, eyeY - eyeH, eyeRightX + eyeW, eyeY, lidPaint)
                canvas.drawLine(eyeLeftX - eyeW / 1.2f, eyeY, eyeLeftX + eyeW / 1.2f, eyeY, linePaint)
                canvas.drawLine(eyeRightX - eyeW / 1.2f, eyeY, eyeRightX + eyeW / 1.2f, eyeY, linePaint)
            }
        }

        // Mouth
        val mouthY = cy + charH * 0.08f
        val mouthW = charW * 0.1f

        if (state.mouthOpen) {
            canvas.drawOval(RectF(cx - mouthW, mouthY - mouthW / 2f, cx + mouthW, mouthY + mouthW / 2f), linePaint)
        } else {
            when (state.mouthExpression) {
                0 -> canvas.drawLine(cx - mouthW, mouthY, cx + mouthW, mouthY, linePaint)
                1 -> canvas.drawArc(RectF(cx - mouthW, mouthY - mouthW / 1.5f, cx + mouthW, mouthY + mouthW / 1.5f), 0f, 180f, false, linePaint)
                2 -> canvas.drawArc(RectF(cx - mouthW, mouthY - mouthW / 1.5f, cx + mouthW, mouthY + mouthW / 1.5f), 180f, 180f, false, linePaint)
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

        canvas.drawPath(path, limbPaint) // Use solid limb paint instead of body gradient

        // Glossy line for limbs
        canvas.drawLine(rootX - width * 0.25f, rootY + width * 0.5f, rootX - width * 0.25f, rootY + length - width * 0.6f, glossPaint)

        canvas.restore()
    }
}
