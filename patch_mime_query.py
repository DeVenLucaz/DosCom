import re

file_path = "/data/data/com.termux/files/home/DosCom/app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt"
with open(file_path, "r") as f:
    content = f.read()

pattern = re.compile(r'                        val animator = ValueAnimator\.ofInt\(layoutParams\.x, finalX\)\n.*?animator\.start\(\)', re.DOTALL)

replacement = """                        val currentMood = com.devenlucaz.doscom.personality.MoodEngine.currentMood
                        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                        
                        val mimeType = com.devenlucaz.doscom.animation.MimeEngine.selectMime(
                            layoutParams.x.toFloat(), layoutParams.y.toFloat(),
                            finalX.toFloat(), finalY.toFloat(),
                            currentMood, currentHour
                        )
                        
                        val fromPos = android.graphics.PointF(layoutParams.x.toFloat(), layoutParams.y.toFloat())
                        val toPos = android.graphics.PointF(finalX.toFloat(), finalY.toFloat())
                        
                        com.devenlucaz.doscom.animation.MimeEngine.executeMime(
                            mimeType, fromPos, toPos, overlayView,
                            onUpdate = { cx, cy ->
                                layoutParams.x = cx.toInt()
                                layoutParams.y = cy.toInt()
                                windowManager.updateViewLayout(overlayView, layoutParams)
                            },
                            onComplete = {
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
                        )"""

content = pattern.sub(replacement, content)

with open(file_path, "w") as f:
    f.write(content)

