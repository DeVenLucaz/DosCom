import re

with open("app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt", "r") as f:
    text = f.read()

# Fix 1: snapToNearestEdge
snap_replacement = """    private fun snapToNearestEdge() {
        val screenWidth = resources.displayMetrics.widthPixels
        val viewW = overlayView.width
        val paddingPx = (40 * resources.displayMetrics.density).toInt()
        val centerX = layoutParams.x + viewW / 2
        val targetX = if (centerX < screenWidth / 2) -paddingPx else screenWidth - viewW + paddingPx

        val animator = ValueAnimator.ofInt(layoutParams.x, targetX)"""

text = text.replace("""    private fun snapToNearestEdge() {
        val screenWidth = resources.displayMetrics.widthPixels
        val viewW = overlayView.width
        val centerX = layoutParams.x + viewW / 2
        val targetX = if (centerX < screenWidth / 2) 0 else screenWidth - viewW

        val animator = ValueAnimator.ofInt(layoutParams.x, targetX)""", snap_replacement)

# Fix 2: LayoutParams initialization
init_replacement = """        layoutParams = WindowManager.LayoutParams(
            paddedSizePx,
            paddedSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - paddedSizePx + paddingPx
            y = screenHeight / 2 - paddedSizePx / 2
        }"""
text = text.replace("""        layoutParams = WindowManager.LayoutParams(
            paddedSizePx,
            paddedSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - paddedSizePx
            y = screenHeight / 2 - paddedSizePx / 2
        }""", init_replacement)

# Fix 3: Drag bounds
drag_replacement = """                        val statusBarHeight = ScreenMetrics.getStatusBarHeight(this)
                        val paddingPx = (40 * resources.displayMetrics.density).toInt()

                        layoutParams.x = max(-paddingPx, min(initialX + deltaX, screenWidth - view.width + paddingPx))
                        layoutParams.y = max(-paddingPx, min(initialY + deltaY, screenHeight - view.height - statusBarHeight + paddingPx))"""

text = text.replace("""                        val statusBarHeight = ScreenMetrics.getStatusBarHeight(this)

                        layoutParams.x = max(0, min(initialX + deltaX, screenWidth - view.width))
                        layoutParams.y = max(0, min(initialY + deltaY, screenHeight - view.height - statusBarHeight))""", drag_replacement)

# Fix 4: handleQuery bounds logic
query_original = """                    val targetCenterX = target.x
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
                    finalY = kotlin.math.max(0, kotlin.math.min(finalY, screenHeight - charSize))"""

query_new = """                    val targetCenterX = target.x
                    val targetCenterY = target.y
                    
                    val screenWidth = ScreenMetrics.getScreenWidth(this@CompanionOverlayService)
                    val screenHeight = ScreenMetrics.getScreenHeight(this@CompanionOverlayService)
                    val statusBarHeight = ScreenMetrics.getStatusBarHeight(this@CompanionOverlayService)
                    val paddingPx = (40 * resources.displayMetrics.density).toInt()
                    
                    val targetWMY = target.y - statusBarHeight
                    
                    val leftSpace = target.x - target.width / 2
                    val rightSpace = screenWidth - (target.x + target.width / 2)
                    val topSpace = targetWMY - target.height / 2
                    val bottomSpace = screenHeight - (targetWMY + target.height / 2)

                    var bestSide = "RIGHT"
                    var maxSpace = rightSpace
                    if (leftSpace > maxSpace) { maxSpace = leftSpace; bestSide = "LEFT" }
                    if (topSpace > maxSpace) { maxSpace = topSpace; bestSide = "TOP" }
                    if (bottomSpace > maxSpace) { maxSpace = bottomSpace; bestSide = "BOTTOM" }
                    
                    val charSize = overlayView.width
                    val coreSize = charSize - paddingPx * 2
                    
                    var finalX = 0
                    var finalY = 0
                    var pointingArmAngle = 0f
                    var isLeftArm = false
                    
                    val targetLeft = target.x - target.width / 2
                    val targetRight = target.x + target.width / 2
                    val targetTop = targetWMY - target.height / 2
                    val targetBottom = targetWMY + target.height / 2
                    
                    when (bestSide) {
                        "RIGHT" -> {
                            finalX = targetRight + 10 - paddingPx
                            finalY = targetWMY - charSize / 2
                            isLeftArm = true
                            pointingArmAngle = 90f
                        }
                        "LEFT" -> {
                            finalX = targetLeft - 10 - coreSize - paddingPx
                            finalY = targetWMY - charSize / 2
                            isLeftArm = false
                            pointingArmAngle = -90f
                        }
                        "TOP" -> {
                            finalX = target.x - charSize / 2
                            finalY = targetTop - 10 - coreSize - paddingPx
                            isLeftArm = false
                            pointingArmAngle = 180f
                        }
                        "BOTTOM" -> {
                            finalX = target.x - charSize / 2
                            finalY = targetBottom + 10 - paddingPx
                            isLeftArm = false
                            pointingArmAngle = 0f
                        }
                    }
                    
                    finalX = kotlin.math.max(-paddingPx, kotlin.math.min(finalX, screenWidth - charSize + paddingPx))
                    finalY = kotlin.math.max(-paddingPx, kotlin.math.min(finalY, screenHeight - charSize + paddingPx))"""

text = text.replace(query_original, query_new)

with open("app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt", "w") as f:
    f.write(text)

