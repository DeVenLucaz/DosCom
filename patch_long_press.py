import re

file_path = "/data/data/com.termux/files/home/DosCom/app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt"
with open(file_path, "r") as f:
    content = f.read()

pattern = re.compile(r'                CoroutineScope\(Dispatchers\.Main\)\.launch \{\n                    try \{\n                        lastScreenshot = ScreenshotHelper\.captureScreen\(this@CompanionOverlayService\)\n                    \} catch \(e: Exception\) \{\n                        e\.printStackTrace\(\)\n                    \}\n                    delay\(300\)\n                    layoutParams\.flags = layoutParams\.flags and WindowManager\.LayoutParams\.FLAG_NOT_FOCUSABLE\.inv\(\)\n                    windowManager\.updateViewLayout\(overlayView, layoutParams\)\n                    showChatInput\(\)\n                \}')

replacement = """                val reactionBox = com.devenlucaz.doscom.ui.ReactionBox(
                    context = this@CompanionOverlayService,
                    windowManager = windowManager,
                    dosComX = layoutParams.x,
                    dosComY = layoutParams.y,
                    onChatClicked = {
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                lastScreenshot = ScreenshotHelper.captureScreen(this@CompanionOverlayService)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            delay(300)
                            layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                            windowManager.updateViewLayout(overlayView, layoutParams)
                            showChatInput()
                        }
                    },
                    onReactedPositive = {
                        idleEngine.targetState.blushVisible = true
                        idleEngine.targetState.bodyOffsetY = -20f
                        handler.postDelayed({
                            idleEngine.targetState.blushVisible = false
                            idleEngine.targetState.bodyOffsetY = 0f
                        }, 2000)
                    },
                    onReactedNegative = {
                        idleEngine.targetState.antennaGlow = 0.2f
                        idleEngine.targetState.mouthExpression = 2
                        handler.postDelayed({
                            idleEngine.targetState.antennaGlow = 1.0f
                            idleEngine.targetState.mouthExpression = 0
                        }, 3000)
                    }
                )
                reactionBox.show()"""

content = pattern.sub(replacement, content)

with open(file_path, "w") as f:
    f.write(content)

