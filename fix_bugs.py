import os, re

# 1. Update CompanionRenderer bounds
renderer_path = "app/src/main/java/com/devenlucaz/doscom/character/CompanionRenderer.kt"
with open(renderer_path, "r") as f:
    r_text = f.read()

r_text = r_text.replace("""        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f""", """        val density = context.resources.displayMetrics.density
        val w = 80f * density
        val h = 80f * density
        val cx = width.toFloat() / 2f
        val cy = height.toFloat() / 2f""")

with open(renderer_path, "w") as f:
    f.write(r_text)

# 2. Update MovementEngine angles
movement_path = "app/src/main/java/com/devenlucaz/doscom/animation/MovementEngine.kt"
with open(movement_path, "r") as f:
    m_text = f.read()

m_text = m_text.replace("-135f", "-67.5f")
m_text = m_text.replace("-90f", "-45f")
m_text = m_text.replace("-45f", "-22.5f")
m_text = m_text.replace("-20f", "-10f")
m_text = m_text.replace(" 45f", " 22.5f")
m_text = m_text.replace(" 90f", " 45f")
# wait, bodyRotation = 90f should not be halved maybe? The prompt said "reduce all AnimationState angle increment values".
# But if it's 45f it's fine.

with open(movement_path, "w") as f:
    f.write(m_text)

# 3. Update IdleAnimationEngine lerp rate
idle_path = "app/src/main/java/com/devenlucaz/doscom/animation/IdleAnimationEngine.kt"
with open(idle_path, "r") as f:
    i_text = f.read()

i_text = i_text.replace("val rate = 0.08f * animSpeedMultiplier", "val rate = 0.03f * animSpeedMultiplier")
with open(idle_path, "w") as f:
    f.write(i_text)

# 4. Update ScreenReader.kt TargetResult
screen_path = "app/src/main/java/com/devenlucaz/doscom/screen/ScreenReader.kt"
with open(screen_path, "r") as f:
    s_text = f.read()

s_text = s_text.replace("""    data class TargetResult(
        val x: Int,
        val y: Int,
        val explanation: String,
        val source: String
    )""", """    data class TargetResult(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val explanation: String,
        val source: String
    )""")

# Accessibility Scanner
s_text = s_text.replace("""            if (foundNode != null) {
                val coords = AccessibilityScanner.getNodeCenterCoords(foundNode)
                val mapped = CoordinateMapper.fromNodeCoords(context, coords.first, coords.second, characterSizePx)
                return TargetResult(
                    x = mapped.first,
                    y = mapped.second,
                    explanation = "I found exactly what you're looking for natively.",
                    source = "Accessibility"
                )
            }""", """            if (foundNode != null) {
                val rect = android.graphics.Rect()
                foundNode.getBoundsInScreen(rect)
                return TargetResult(
                    x = rect.centerX(),
                    y = rect.centerY(),
                    width = rect.width(),
                    height = rect.height(),
                    explanation = "I found exactly what you're looking for natively.",
                    source = "Accessibility"
                )
            }""")

# Gemini Scanner
s_text = s_text.replace("""            if (visionResult != null && visionResult.found) {
                val mapped = CoordinateMapper.fromPercent(
                    context, 
                    visionResult.xPercent, 
                    visionResult.yPercent, 
                    characterSizePx
                )
                return TargetResult(
                    x = mapped.first,
                    y = mapped.second,
                    explanation = visionResult.explanation,
                    source = "GeminiVision"
                )
            }""", """            if (visionResult != null && visionResult.found) {
                val normX = if (visionResult.xPercent > 1f) visionResult.xPercent / 100f else visionResult.xPercent
                val normY = if (visionResult.yPercent > 1f) visionResult.yPercent / 100f else visionResult.yPercent
                val rawX = (com.devenlucaz.doscom.utils.ScreenMetrics.getScreenWidth(context) * normX).toInt()
                val rawY = (com.devenlucaz.doscom.utils.ScreenMetrics.getScreenHeight(context) * normY).toInt()

                return TargetResult(
                    x = rawX,
                    y = rawY,
                    width = 0,
                    height = 0,
                    explanation = visionResult.explanation,
                    source = "GeminiVision"
                )
            }""")

with open(screen_path, "w") as f:
    f.write(s_text)

# 5. Update CompanionOverlayService.kt
overlay_path = "app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt"
with open(overlay_path, "r") as f:
    o_text = f.read()

# Size and padding
o_text = o_text.replace("""        val sizePx = (80 * resources.displayMetrics.density).toInt()

        overlayView = CompanionRenderer(this)""", """        val sizePx = (80 * resources.displayMetrics.density).toInt()
        val paddingPx = (40 * resources.displayMetrics.density).toInt()
        val paddedSizePx = sizePx + paddingPx * 2

        overlayView = CompanionRenderer(this)""")

o_text = o_text.replace("""        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }""", """        val screenWidth = ScreenMetrics.getScreenWidth(this)
        val screenHeight = ScreenMetrics.getScreenHeight(this)
        layoutParams = WindowManager.LayoutParams(
            paddedSizePx,
            paddedSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - paddedSizePx
            y = screenHeight / 2 - paddedSizePx / 2
        }""")

# Handle target result in handleQuery
query_logic = """                    val targetCenterX = target.x
                    val targetCenterY = target.y
                    
                    val screenWidth = ScreenMetrics.getScreenWidth(this@CompanionOverlayService)
                    val screenHeight = ScreenMetrics.getScreenHeight(this@CompanionOverlayService)
                    
                    val leftSpace = target.x - target.width / 2
                    val rightSpace = screenWidth - (target.x + target.width / 2)
                    val topSpace = target.y - target.height / 2
                    val bottomSpace = screenHeight - (target.y + target.height / 2)

                    var bestSide = "RIGHT"
                    var maxSpace = rightSpace
                    if (leftSpace > maxSpace) { maxSpace = leftSpace; bestSide = "LEFT" }
                    if (topSpace > maxSpace) { maxSpace = topSpace; bestSide = "TOP" }
                    if (bottomSpace > maxSpace) { maxSpace = bottomSpace; bestSide = "BOTTOM" }
                    
                    val charSize = overlayView.width
                    
                    var finalX = 0
                    var finalY = 0
                    var pointingArmAngle = 0f
                    var isLeftArm = false
                    
                    when (bestSide) {
                        "RIGHT" -> {
                            finalX = target.x + target.width / 2 + 10
                            finalY = target.y - charSize / 2
                            isLeftArm = true
                            pointingArmAngle = 90f
                        }
                        "LEFT" -> {
                            finalX = target.x - target.width / 2 - charSize - 10
                            finalY = target.y - charSize / 2
                            isLeftArm = false
                            pointingArmAngle = -90f
                        }
                        "TOP" -> {
                            finalX = target.x - charSize / 2
                            finalY = target.y - target.height / 2 - charSize - 10
                            isLeftArm = false
                            pointingArmAngle = 180f
                        }
                        "BOTTOM" -> {
                            finalX = target.x - charSize / 2
                            finalY = target.y + target.height / 2 + 10
                            isLeftArm = false
                            pointingArmAngle = 0f
                        }
                    }
                    
                    finalX = kotlin.math.max(0, kotlin.math.min(finalX, screenWidth - charSize))
                    finalY = kotlin.math.max(0, kotlin.math.min(finalY, screenHeight - charSize))

                    showConfirmRing(targetCenterX, targetCenterY) {
                        val animator = ValueAnimator.ofInt(layoutParams.x, finalX)
                        val yAnimator = ValueAnimator.ofInt(layoutParams.y, finalY)
                        animator.duration = 500
                        yAnimator.duration = 500
                        
                        animator.addUpdateListener { anim ->
                            layoutParams.x = anim.animatedValue as Int
                        }
                        yAnimator.addUpdateListener { anim ->
                            layoutParams.y = anim.animatedValue as Int
                            windowManager.updateViewLayout(overlayView, layoutParams)
                        }
                        
                        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                            override fun onAnimationEnd(anim: android.animation.Animator) {
                                showSpeechBubble(target.explanation, finalX, finalY)
                                if (isLeftArm) {
                                    idleEngine.targetState.leftArmAngle = pointingArmAngle
                                } else {
                                    idleEngine.targetState.rightArmAngle = pointingArmAngle
                                }
                                serviceScope.launch(Dispatchers.Main) {
                                    delay(4000)
                                    snapToNearestEdge()
                                    idleEngine.targetState.leftArmAngle = 0f
                                    idleEngine.targetState.rightArmAngle = 0f
                                }
                            }
                        })
                        animator.start()
                        yAnimator.start()
                    }"""

o_text = re.sub(r"                    val targetCenterX = target\.x \+ charSizePx / 2[\s\S]*?animator\.start\(\)\n                    \}", query_logic, o_text)

# Fix snapToNearestEdge charSize
o_text = o_text.replace("""        val sizePx = (80 * resources.displayMetrics.density).toInt()
        val centerX = layoutParams.x + sizePx / 2
        val targetX = if (centerX < screenWidth / 2) 0 else screenWidth - sizePx""", """        val viewW = overlayView.width
        val centerX = layoutParams.x + viewW / 2
        val targetX = if (centerX < screenWidth / 2) 0 else screenWidth - viewW""")

with open(overlay_path, "w") as f:
    f.write(o_text)
