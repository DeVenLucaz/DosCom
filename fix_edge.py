import re

with open("app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt", "r") as f:
    text = f.read()

# Fix setupOverlayView initial x position
init_pattern = r"""        layoutParams = WindowManager\.LayoutParams\([\s\S]*?\.apply \{\n            gravity = Gravity\.TOP or Gravity\.START\n            x = screenWidth - paddedSizePx \+ paddingPx\n            y = screenHeight / 2 - paddedSizePx / 2\n        \}"""
init_repl = """        val visualOffsetPx = (58 * resources.displayMetrics.density).toInt()
        layoutParams = WindowManager.LayoutParams(
            paddedSizePx,
            paddedSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - paddedSizePx + visualOffsetPx
            y = screenHeight / 2 - paddedSizePx / 2
        }"""
text = re.sub(init_pattern, init_repl, text)

# Fix drag bounds
drag_pattern = r"""                        val paddingPx = \(40 \* resources\.displayMetrics\.density\)\.toInt\(\)\n\n                        layoutParams\.x = max\(-paddingPx, min\(initialX \+ deltaX, screenWidth - view\.width \+ paddingPx\)\)"""
drag_repl = """                        val visualOffsetPx = (58 * resources.displayMetrics.density).toInt()

                        layoutParams.x = max(-visualOffsetPx, min(initialX + deltaX, screenWidth - view.width + visualOffsetPx))"""
text = re.sub(drag_pattern, drag_repl, text)

# Fix snapToNearestEdge
snap_pattern = r"""    private fun snapToNearestEdge\(\) \{\n        val screenWidth = resources\.displayMetrics\.widthPixels\n        val viewW = overlayView\.width\n        val paddingPx = \(40 \* resources\.displayMetrics\.density\)\.toInt\(\)\n        val centerX = layoutParams\.x \+ viewW / 2\n        val targetX = if \(centerX < screenWidth / 2\) -paddingPx else screenWidth - viewW \+ paddingPx"""
snap_repl = """    private fun snapToNearestEdge() {
        val screenWidth = resources.displayMetrics.widthPixels
        val viewW = overlayView.width
        val visualOffsetPx = (58 * resources.displayMetrics.density).toInt()
        val centerX = layoutParams.x + viewW / 2
        val targetX = if (centerX < screenWidth / 2) -visualOffsetPx else screenWidth - viewW + visualOffsetPx"""
text = re.sub(snap_pattern, snap_repl, text)

with open("app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt", "w") as f:
    f.write(text)

