import sys, re

with open("app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt", "r") as f:
    text = f.read()

# Replace imports
text = text.replace("import com.devenlucaz.doscom.character.CharacterState\n", "")
text = text.replace("import com.devenlucaz.doscom.character.CompanionAnimator\n", "")
text = text.replace("import com.devenlucaz.doscom.character.CompanionRenderer", "import com.devenlucaz.doscom.character.CompanionRenderer\nimport com.devenlucaz.doscom.animation.IdleAnimationEngine\nimport com.devenlucaz.doscom.character.AnimationQueue")

# Replace variables
text = text.replace("    var lastScreenshot: Bitmap? = null", "    var lastScreenshot: Bitmap? = null\n\n    private val animationQueue = AnimationQueue()\n    private lateinit var idleEngine: IdleAnimationEngine")

# Remove old idle logic
text = text.replace("""    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = object : Runnable {
        override fun run() {
            scheduleNextIdleBehavior()
        }
    }""", "")

# Instantiate IdleAnimationEngine in setupOverlayView
setup_replace = """        overlayView = CompanionRenderer(this)

        idleEngine = IdleAnimationEngine(
            queue = animationQueue,
            onUpdateState = { state ->
                overlayView.state = state
                overlayView.invalidate()
            },
            onDrawZzz = { particles ->
                overlayView.zzzParticles = particles
                overlayView.invalidate()
            }
        )
        idleEngine.animSpeedMultiplier = 1f
        idleEngine.sleepTimerMs = 5 * 60 * 1000L"""

text = text.replace("        overlayView = CompanionRenderer(this)", setup_replace)

# onLongPress
text = text.replace("overlayView.setState(CharacterState.LISTEN)", "idleEngine.interact()\n                idleEngine.targetState.mouthExpression = 1 // LISTEN")

# onClose chat input & onVoiceError
text = text.replace("overlayView.setState(CharacterState.IDLE_BOB)", "idleEngine.interact()\n                idleEngine.targetState.mouthExpression = 0\n                idleEngine.targetState.leftArmAngle = 0f\n                idleEngine.targetState.bodyOffsetY = 0f")

# handleQuery walkTo 
text = text.replace("""                        CompanionAnimator.walkTo(target.x, target.y, overlayView, layoutParams, windowManager) {
                            showSpeechBubble(target.explanation, target.x, target.y)
                            overlayView.setState(CharacterState.POINT)
                            
                            serviceScope.launch(Dispatchers.Main) {
                                delay(4000)
                                CompanionAnimator.walkToEdge(
                                    this@CompanionOverlayService, 
                                    overlayView, 
                                    layoutParams, 
                                    windowManager
                                ) {
                                    overlayView.setState(CharacterState.IDLE_BOB)
                                }
                            }
                        }""", """                        val animator = ValueAnimator.ofInt(layoutParams.x, target.x)
                        animator.duration = 500
                        animator.addUpdateListener { anim ->
                            layoutParams.x = anim.animatedValue as Int
                            layoutParams.y = target.y
                            windowManager.updateViewLayout(overlayView, layoutParams)
                        }
                        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                            override fun onAnimationEnd(anim: android.animation.Animator) {
                                showSpeechBubble(target.explanation, target.x, target.y)
                                idleEngine.targetState.rightArmAngle = -90f // POINT equivalent
                                serviceScope.launch(Dispatchers.Main) {
                                    delay(4000)
                                    snapToNearestEdge()
                                    idleEngine.targetState.rightArmAngle = 0f // reset point
                                }
                            }
                        })
                        animator.start()""")

text = text.replace("overlayView.setState(CharacterState.REACT_WORRY)", "idleEngine.targetState.mouthExpression = 2 // REACT_WORRY")

# walkToEdge in handleDragRelease
text = text.replace("""                CompanionAnimator.walkToEdge(
                    this@CompanionOverlayService,
                    overlayView,
                    layoutParams,
                    windowManager
                ) {
                    val idleState = com.devenlucaz.doscom.character.AnimationState()
                    lerpAnimationState(overlayView.state, idleState, 200L)
                }""", """                snapToNearestEdge()
                val idleState = com.devenlucaz.doscom.character.AnimationState()
                lerpAnimationState(overlayView.state, idleState, 200L)""")

# idle behaviors
text = text.replace("""    private fun startIdleBehaviors() {
        scheduleNextIdleBehavior()
    }""", """    private fun startIdleBehaviors() {
        idleEngine.start()
    }""")

text = text.replace("""    private fun stopIdleBehaviors() {
        idleHandler.removeCallbacks(idleRunnable)
    }""", """    private fun stopIdleBehaviors() {
        idleEngine.stop()
    }""")

text = re.sub(r"    private fun scheduleNextIdleBehavior\(\) \{[\s\S]*?    \}\n", "", text)

# handleNotificationReaction
text = text.replace("overlayView.setState(CharacterState.REACT_WAVE)", "idleEngine.interact()\n                idleEngine.targetState.leftArmAngle = -160f")
text = text.replace("overlayView.setState(CharacterState.REACT_HAPPY)", "idleEngine.interact()\n                idleEngine.targetState.mouthExpression = 1\n                idleEngine.targetState.bodyOffsetY = -10f")

with open("app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt", "w") as f:
    f.write(text)
