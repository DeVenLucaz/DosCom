import os

path = "/data/data/com.termux/files/home/DosCom/app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt"
with open(path, "r") as f:
    code = f.read()

# Add fields
code = code.replace(
"""    private lateinit var layoutParams: WindowManager.LayoutParams
    private var lastScreenshot: android.graphics.Bitmap? = null""",
"""    private lateinit var layoutParams: WindowManager.LayoutParams
    private var lastScreenshot: android.graphics.Bitmap? = null
    
    private var semiGhostView: View? = null
    private var semiGhostLayoutParams: WindowManager.LayoutParams? = null
    private var initialPinchDist = 0f
    private var isPinching = false
    private var initialScaleForPinch = 1f
    private var doubleTapRunnable: Runnable? = null""")

# Replace touch listener logic
touch_logic_old = """                MotionEvent.ACTION_DOWN -> {
                    val now = System.currentTimeMillis()
                    if (now - lastManualTapTime < 400) {
                        manualTapCount++
                        if (manualTapCount == 3) {
                            manualTapCount = 0
                            val intent = android.content.Intent(this@CompanionOverlayService, com.devenlucaz.doscom.settings.SettingsActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            startActivity(intent)
                        }
                    } else {
                        manualTapCount = 1
                    }
                    lastManualTapTime = now
                    
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    lastDragX = event.rawX
                    lastDragY = event.rawY
                    dragDistanceAccumulator = 0f
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                        isDragging = true
                    }

                    if (isDragging) {"""

touch_logic_new = """                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) {
                        isPinching = true
                        val x0 = event.getX(0)
                        val y0 = event.getY(0)
                        val x1 = event.getX(1)
                        val y1 = event.getY(1)
                        initialPinchDist = kotlin.math.sqrt(((x1-x0)*(x1-x0) + (y1-y0)*(y1-y0)).toDouble()).toFloat()
                        initialScaleForPinch = idleEngine.targetState.scale
                    }
                    true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.pointerCount <= 2 && isPinching) {
                        isPinching = false
                        val scaleInt = ((idleEngine.targetState.scale - 0.5f) / (1.5f / 14f)).toInt()
                        prefs.edit().putInt("mascot_scale", kotlin.math.max(0, kotlin.math.min(scaleInt, 14))).apply()
                    }
                    true
                }
                MotionEvent.ACTION_DOWN -> {
                    val now = System.currentTimeMillis()
                    if (now - lastManualTapTime > 500) {
                        manualTapCount = 1
                    } else {
                        manualTapCount++
                    }
                    lastManualTapTime = now

                    doubleTapRunnable?.let { handler.removeCallbacks(it) }
                    
                    if (manualTapCount == 3) {
                        val intent = android.content.Intent(this@CompanionOverlayService, com.devenlucaz.doscom.settings.SettingsActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        idleEngine.targetState.bodyRotation = 360f
                        idleEngine.targetState.antennaGlow = 2.0f
                        handler.postDelayed({ 
                            idleEngine.targetState.bodyRotation = 0f 
                            idleEngine.targetState.antennaGlow = 1.0f
                        }, 500)
                        manualTapCount = 0
                    } else if (manualTapCount == 2) {
                        doubleTapRunnable = Runnable {
                            if (manualTapCount == 2) {
                                val newMode = com.devenlucaz.doscom.mode.ModeManager.cycleMode(this@CompanionOverlayService)
                                overlayView.antennaColor = when(newMode) {
                                    com.devenlucaz.doscom.mode.CompanionMode.ALIVE -> android.graphics.Color.WHITE
                                    com.devenlucaz.doscom.mode.CompanionMode.AWAKE -> android.graphics.Color.parseColor("#00B4FF")
                                    com.devenlucaz.doscom.mode.CompanionMode.AWARE -> android.graphics.Color.parseColor("#00FF88")
                                }
                                idleEngine.targetState.bodyOffsetY = -30f
                                handler.postDelayed({ idleEngine.targetState.bodyOffsetY = 0f }, 300)
                                manualTapCount = 0
                            }
                        }
                        handler.postDelayed(doubleTapRunnable!!, 200)
                    }
                    
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    lastDragX = event.rawX
                    lastDragY = event.rawY
                    dragDistanceAccumulator = 0f
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isPinching && event.pointerCount >= 2) {
                        val x0 = event.getX(0)
                        val y0 = event.getY(0)
                        val x1 = event.getX(1)
                        val y1 = event.getY(1)
                        val currentDist = kotlin.math.sqrt(((x1-x0)*(x1-x0) + (y1-y0)*(y1-y0)).toDouble()).toFloat()
                        if (initialPinchDist > 10f) {
                            var newScale = initialScaleForPinch * (currentDist / initialPinchDist)
                            newScale = kotlin.math.max(0.5f, kotlin.math.min(newScale, 2.0f))
                            idleEngine.targetState.scale = newScale
                            idleEngine.targetState.scaleX = newScale
                        }
                        return@setOnTouchListener true
                    }
                    if (isPinching) return@setOnTouchListener true
                    
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (kotlin.math.abs(deltaX) > 10 || kotlin.math.abs(deltaY) > 10) {
                        isDragging = true
                    }

                    if (isDragging) {"""

code = code.replace(touch_logic_old, touch_logic_new)

update_ghost_old = """    private fun updateGhostMode(mode: Int) {
        if (!::windowManager.isInitialized || !::overlayView.isInitialized || !::layoutParams.isInitialized) return
        when (mode) {
            0 -> { // Interactive
                layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                overlayView.alpha = 1.0f
            }
            1 -> { // Semi-Ghost
                layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                overlayView.alpha = 0.6f
            }
            2 -> { // Full Ghost
                layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                overlayView.alpha = 0.3f
            }
        }
        try {
            windowManager.updateViewLayout(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }"""

update_ghost_new = """    private fun updateRobotLayout() {
        try {
            windowManager.updateViewLayout(overlayView, layoutParams)
            if (semiGhostView != null && semiGhostLayoutParams != null) {
                val sizePx = (60 * resources.displayMetrics.density).toInt()
                val centerX = layoutParams.x + overlayView.width / 2
                val centerY = layoutParams.y + overlayView.height / 2
                semiGhostLayoutParams!!.x = centerX - sizePx / 2
                semiGhostLayoutParams!!.y = centerY - sizePx / 2
                windowManager.updateViewLayout(semiGhostView, semiGhostLayoutParams)
            }
        } catch (e: Exception) {}
    }

    private fun setupSemiGhostView() {
        val sizePx = (60 * resources.displayMetrics.density).toInt()
        semiGhostView = View(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        
        val centerX = layoutParams.x + overlayView.width / 2
        val centerY = layoutParams.y + overlayView.height / 2

        semiGhostLayoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = centerX - sizePx / 2
            y = centerY - sizePx / 2
        }

        var touchDownRunnable: Runnable? = null

        semiGhostView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownRunnable = Runnable {
                        updateGhostMode(0) // Interactive
                        idleEngine.targetState.eyesWide = true
                        idleEngine.targetState.leftArmAngle = -160f
                        idleEngine.targetState.rightArmAngle = -160f
                        handler.postDelayed({
                            idleEngine.targetState.eyesWide = false
                            idleEngine.targetState.leftArmAngle = 0f
                            idleEngine.targetState.rightArmAngle = 0f
                        }, 1500)
                        
                        handler.postDelayed({
                            if (prefs.getInt("ghost_mode", 0) == 1) { 
                                updateGhostMode(1)
                            }
                        }, 10000)
                    }
                    handler.postDelayed(touchDownRunnable!!, 1000)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_MOVE -> {
                    touchDownRunnable?.let { handler.removeCallbacks(it) }
                    true
                }
                else -> false
            }
        }
        
        try {
            windowManager.addView(semiGhostView, semiGhostLayoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateGhostMode(mode: Int) {
        if (!::windowManager.isInitialized || !::overlayView.isInitialized || !::layoutParams.isInitialized) return
        
        semiGhostView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            semiGhostView = null
        }

        when (mode) {
            0 -> { // Interactive
                layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                overlayView.alpha = 1.0f
            }
            1 -> { // Semi-Ghost
                layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                overlayView.alpha = 0.6f
                setupSemiGhostView()
            }
            2 -> { // Full Ghost
                layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                overlayView.alpha = 0.3f
            }
        }
        updateRobotLayout()
    }"""

code = code.replace(update_ghost_old, update_ghost_new)

# Global replace updateViewLayout
code = code.replace("windowManager.updateViewLayout(overlayView, layoutParams)", "updateRobotLayout()")

# For ACTION_UP, MotionEvent.ACTION_CANCEL in touch listener, if isDragging handleDragRelease, add isPinching = false
code = code.replace("""                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        handleDragRelease()
                    }
                    isDragging = false
                    true
                }""", """                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isPinching = false
                    if (isDragging) {
                        handleDragRelease()
                    }
                    isDragging = false
                    true
                }""")

code = code.replace("""            if (::overlayView.isInitialized) {
                windowManager.removeView(overlayView)
            }""", """            if (::overlayView.isInitialized) {
                windowManager.removeView(overlayView)
            }
            semiGhostView?.let { windowManager.removeView(it) }""")


with open(path, "w") as f:
    f.write(code)
