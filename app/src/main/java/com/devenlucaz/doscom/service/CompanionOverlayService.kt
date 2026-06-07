package com.devenlucaz.doscom.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.devenlucaz.doscom.R
import android.os.Handler
import android.os.Looper
import com.devenlucaz.doscom.character.CompanionRenderer
import com.devenlucaz.doscom.animation.IdleAnimationEngine
import com.devenlucaz.doscom.character.AnimationQueue
import com.devenlucaz.doscom.utils.ScreenMetrics
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import android.os.Vibrator
import android.os.VibrationEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import com.devenlucaz.doscom.utils.ScreenshotHelper
import kotlin.math.abs
import android.view.GestureDetector

import com.devenlucaz.doscom.screen.ScreenReader
import com.devenlucaz.doscom.service.DosComAccessibilityService
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.SharedPreferences
import com.devenlucaz.doscom.mode.ModeManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class CompanionOverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: CompanionRenderer
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var lastScreenshot: android.graphics.Bitmap? = null
    
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var prefs: SharedPreferences
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "mascot_scale" -> {
                val scale = sharedPreferences.getInt("mascot_scale", 2)
                val scaleFloat = 0.5f + (scale * 0.375f) // map 0-4 to 0.5-2.0
                idleEngine.targetState.scale = scaleFloat
                idleEngine.targetState.scaleX = scaleFloat
            }
            "anim_speed" -> {
                val speed = sharedPreferences.getInt("anim_speed", 2)
                val speedFloat = 0.5f + (speed * 0.25f) // map 0-4 to 0.5-1.5
                idleEngine.animSpeedMultiplier = speedFloat
            }
            "sleep_timer" -> {
                val sleepPos = sharedPreferences.getInt("sleep_timer", 0)
                idleEngine.sleepTimerMs = when(sleepPos) {
                    0 -> 60 * 1000L
                    1 -> 5 * 60 * 1000L
                    2 -> 10 * 60 * 1000L
                    else -> Long.MAX_VALUE
                }
            }
        }
    }


    private val animationQueue = AnimationQueue()
    private lateinit var idleEngine: IdleAnimationEngine

    private lateinit var phoneEventReceiver: com.devenlucaz.doscom.events.PhoneEventReceiver
    private lateinit var appContextWatcher: com.devenlucaz.doscom.events.AppContextWatcher
    private lateinit var timeReactionEngine: com.devenlucaz.doscom.events.TimeReactionEngine



    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val reactionType = intent?.getStringExtra(DosComNotificationListener.EXTRA_REACTION_TYPE) ?: return
            handleNotificationReaction(reactionType)
        }
    }

    private val appCategoryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val categoryName = intent?.getStringExtra("category") ?: return
            handleAppCategoryReaction(categoryName)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        try {
            com.devenlucaz.doscom.brain.BrainManager.init(this)
            startForegroundServiceNotification()
            setupOverlayView()
            LocalBroadcastManager.getInstance(this).registerReceiver(
                notificationReceiver,
                IntentFilter(DosComNotificationListener.ACTION_NOTIFICATION_REACTION)
            )
            LocalBroadcastManager.getInstance(this).registerReceiver(
                appCategoryReceiver,
                IntentFilter("APP_CONTEXT_CHANGED")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun setupOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val sizePx = (80 * resources.displayMetrics.density).toInt()
        val paddingPx = (40 * resources.displayMetrics.density).toInt()
        val paddedSizePx = sizePx + paddingPx * 2

        overlayView = CompanionRenderer(this)

        idleEngine = IdleAnimationEngine(
            context = this,
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
        idleEngine.sleepTimerMs = 5 * 60 * 1000L

        val screenWidth = ScreenMetrics.getScreenWidth(this)
        val screenHeight = ScreenMetrics.getScreenHeight(this)
        val visualOffsetPx = (58 * resources.displayMetrics.density).toInt()
        layoutParams = WindowManager.LayoutParams(
            paddedSizePx,
            paddedSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - paddedSizePx + visualOffsetPx
            y = screenHeight / 2 - paddedSizePx / 2
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        var lastDragX = 0f
        var lastDragY = 0f
        var dragFrameIndex = 0
        var dragDistanceAccumulator = 0f

        var manualTapCount = 0
        var lastManualTapTime = 0L

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (isDragging) return
                
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }

                idleEngine.interact()
                idleEngine.targetState.mouthExpression = 1 // LISTEN
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
            }
            
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (com.devenlucaz.doscom.systems.BirthdaySystem.isDosCombBirthday(this@CompanionOverlayService)) {
                    idleEngine.targetState.eyesWide = true
                    idleEngine.targetState.blushVisible = true
                    idleEngine.targetState.bodyOffsetY = -30f
                    idleEngine.targetState.leftArmAngle = -150f
                    idleEngine.targetState.rightArmAngle = -150f
                    
                    com.devenlucaz.doscom.personality.EmotionalMemory.recordPositive(this@CompanionOverlayService, 0.3f)
                    val inputs = com.devenlucaz.doscom.brain.BrainInput.buildInputs(this@CompanionOverlayService)
                    com.devenlucaz.doscom.brain.BrainManager.brain.learn(inputs, IntArray(7), 1.0f)
                    com.devenlucaz.doscom.brain.BrainManager.brain.save(this@CompanionOverlayService)
                    
                    handler.postDelayed({
                        idleEngine.targetState.eyesWide = false
                        idleEngine.targetState.blushVisible = false
                        idleEngine.targetState.bodyOffsetY = 0f
                        idleEngine.targetState.leftArmAngle = 0f
                        idleEngine.targetState.rightArmAngle = 0f
                    }, 2000)
                } else if (com.devenlucaz.doscom.systems.BirthdaySystem.isUserBirthday(this@CompanionOverlayService)) {
                    if (idleEngine.targetState.activeProp == com.devenlucaz.doscom.character.PropType.GIFT_BOX) {
                        idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.NONE
                        val toy = com.devenlucaz.doscom.systems.ToyBoxSystem.selectToy(this@CompanionOverlayService)
                        com.devenlucaz.doscom.systems.ToyBoxSystem.startToyActivity(toy, idleEngine)
                        idleEngine.targetState.bodyOffsetY = -20f
                        handler.postDelayed({ idleEngine.targetState.bodyOffsetY = 0f }, 2000)
                    } else if (idleEngine.targetState.activeProp == com.devenlucaz.doscom.character.PropType.TINY_CAKE) {
                        idleEngine.targetState.leftArmAngle = -90f
                        idleEngine.targetState.mouthExpression = 2 
                        handler.postDelayed({ 
                            idleEngine.targetState.leftArmAngle = 0f
                            idleEngine.targetState.mouthExpression = 1 
                            idleEngine.targetState.bodyOffsetY = -20f
                            handler.postDelayed({ idleEngine.targetState.bodyOffsetY = 0f }, 1000)
                        }, 1000)
                    }
                }
                return true
            }
        })

        overlayView.setOnTouchListener { view, event ->
            if (event == null) return@setOnTouchListener false
            
            gestureDetector.onTouchEvent(event)
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
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

                    if (isDragging) {
                        val screenWidth = ScreenMetrics.getScreenWidth(this)
                        val screenHeight = ScreenMetrics.getScreenHeight(this)
                        val statusBarHeight = ScreenMetrics.getStatusBarHeight(this)
                        val visualOffsetPx = (58 * resources.displayMetrics.density).toInt()

                        layoutParams.x = max(-visualOffsetPx, min(initialX + deltaX, screenWidth - view.width + visualOffsetPx))
                        layoutParams.y = max(-paddingPx, min(initialY + deltaY, screenHeight - view.height - statusBarHeight + paddingPx))

                        windowManager.updateViewLayout(overlayView, layoutParams)
                        
                        val dx = event.rawX - lastDragX
                        val dy = event.rawY - lastDragY
                        val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        
                        lastDragX = event.rawX
                        lastDragY = event.rawY
                        
                        dragDistanceAccumulator += dist
                        if (dragDistanceAccumulator > 20f) {
                            dragDistanceAccumulator = 0f
                            dragFrameIndex = (dragFrameIndex + 1) % 4
                            
                            val pose = com.devenlucaz.doscom.animation.PoseEngine.detectPose(
                                layoutParams.x, layoutParams.y,
                                screenWidth, screenHeight,
                                view.width, view.height
                            )
                            
                            val stateList = when (pose) {
                                com.devenlucaz.doscom.animation.RobotPose.HANG_LEFT, com.devenlucaz.doscom.animation.RobotPose.HANG_RIGHT -> {
                                    val dir = if (dy < 0) -1 else 1 
                                    com.devenlucaz.doscom.animation.MovementEngine.generateClimbFrames(dir)
                                }
                                com.devenlucaz.doscom.animation.RobotPose.GRIP_TOP, com.devenlucaz.doscom.animation.RobotPose.SIT_BOTTOM -> {
                                    val dir = if (dx > 0) 1 else -1 
                                    com.devenlucaz.doscom.animation.MovementEngine.generateCrawlFrames(dir)
                                }
                                else -> null
                            }
                            
                            if (stateList != null) {
                                overlayView.state = stateList[dragFrameIndex]
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        handleDragRelease()
                    }
                    isDragging = false
                    true
                }
                else -> false
            }
        }

        windowManager.addView(overlayView, layoutParams)
        startIdleBehaviors()

        prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        // trigger initial load
        prefsListener.onSharedPreferenceChanged(prefs, "mascot_scale")
        prefsListener.onSharedPreferenceChanged(prefs, "anim_speed")
        prefsListener.onSharedPreferenceChanged(prefs, "sleep_timer")

        
        phoneEventReceiver = com.devenlucaz.doscom.events.PhoneEventReceiver(idleEngine) {
            val screenHeight = ScreenMetrics.getScreenHeight(this)
            val animator = ValueAnimator.ofInt(layoutParams.y, screenHeight - overlayView.height)
            animator.duration = 500
            animator.addUpdateListener { anim ->
                layoutParams.y = anim.animatedValue as Int
                windowManager.updateViewLayout(overlayView, layoutParams)
            }
            animator.start()
        }
        phoneEventReceiver.register(this)

        appContextWatcher = com.devenlucaz.doscom.events.AppContextWatcher(this)
        appContextWatcher.start()

        timeReactionEngine = com.devenlucaz.doscom.events.TimeReactionEngine(this, idleEngine, windowManager)
        timeReactionEngine.start()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "doscom_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "DosCom Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps DosCom floating in the background"
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("DosCom is running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(appCategoryReceiver)
        if (::phoneEventReceiver.isInitialized) phoneEventReceiver.unregister(this)
        if (::appContextWatcher.isInitialized) appContextWatcher.stop()
        if (::timeReactionEngine.isInitialized) timeReactionEngine.stop()
        stopIdleBehaviors()
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        serviceScope.cancel()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun snapToNearestEdge() {
        val screenWidth = resources.displayMetrics.widthPixels
        val viewW = overlayView.width
        val visualOffsetPx = (58 * resources.displayMetrics.density).toInt()
        val centerX = layoutParams.x + viewW / 2
        val targetX = if (centerX < screenWidth / 2) -visualOffsetPx else screenWidth - viewW + visualOffsetPx

        val animator = ValueAnimator.ofInt(layoutParams.x, targetX)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            layoutParams.x = animation.animatedValue as Int
            try {
                windowManager.updateViewLayout(overlayView, layoutParams)
            } catch (e: Exception) {
                // Ignore if view was removed
            }
        }
        animator.start()
    }

    private fun handleDragRelease() {
        val screenWidth = ScreenMetrics.getScreenWidth(this)
        val screenHeight = ScreenMetrics.getScreenHeight(this)
        val viewW = overlayView.width
        val viewH = overlayView.height
        
        val pose = com.devenlucaz.doscom.animation.PoseEngine.detectPose(
            layoutParams.x, layoutParams.y,
            screenWidth, screenHeight,
            viewW, viewH
        )
        val targetState = com.devenlucaz.doscom.animation.PoseEngine.getTargetState(pose)
        
        if (pose == com.devenlucaz.doscom.animation.RobotPose.FLOATING) {
            lerpAnimationState(overlayView.state, targetState, 400L) {
                val idleState = com.devenlucaz.doscom.character.AnimationState()
                lerpAnimationState(overlayView.state, idleState, 200L)
                serviceScope.launch(Dispatchers.Main) {
                    delay(kotlin.random.Random.nextLong(3000L, 15000L))
                    snapToNearestEdge()
                }
            }
        } else {
            if (pose == com.devenlucaz.doscom.animation.RobotPose.HANG_LEFT || pose == com.devenlucaz.doscom.animation.RobotPose.HANG_RIGHT) {
                snapToNearestEdge()
            }
            lerpAnimationState(overlayView.state, targetState, 400L)
        }
    }

    private fun lerpAnimationState(
        startState: com.devenlucaz.doscom.character.AnimationState,
        endState: com.devenlucaz.doscom.character.AnimationState,
        durationMs: Long,
        onComplete: () -> Unit = {}
    ) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = durationMs
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedFraction
            overlayView.state = com.devenlucaz.doscom.character.AnimationState(
                leftArmAngle = lerp(startState.leftArmAngle, endState.leftArmAngle, fraction),
                rightArmAngle = lerp(startState.rightArmAngle, endState.rightArmAngle, fraction),
                leftLegAngle = lerp(startState.leftLegAngle, endState.leftLegAngle, fraction),
                rightLegAngle = lerp(startState.rightLegAngle, endState.rightLegAngle, fraction),
                bodyOffsetY = lerp(startState.bodyOffsetY, endState.bodyOffsetY, fraction),
                bodyOffsetX = lerp(startState.bodyOffsetX, endState.bodyOffsetX, fraction),
                bodyRotation = lerp(startState.bodyRotation, endState.bodyRotation, fraction),
                scaleX = endState.scaleX,
                eyesClosed = endState.eyesClosed,
                eyesHalf = endState.eyesHalf,
                eyesWide = endState.eyesWide,
                pupilOffsetX = endState.pupilOffsetX,
                pupilOffsetY = endState.pupilOffsetY,
                mouthExpression = endState.mouthExpression,
                mouthOpen = endState.mouthOpen,
                blushVisible = endState.blushVisible,
                tongueOut = endState.tongueOut,
                antennaGlow = endState.antennaGlow,
                scale = endState.scale,
                activeProp = endState.activeProp
            )
        }
        animator.addListener(object: android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                onComplete()
            }
        })
        animator.start()
    }
    
    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }

    private fun startIdleBehaviors()

        prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        // trigger initial load
        prefsListener.onSharedPreferenceChanged(prefs, "mascot_scale")
        prefsListener.onSharedPreferenceChanged(prefs, "anim_speed")
        prefsListener.onSharedPreferenceChanged(prefs, "sleep_timer")
 {
        idleEngine.start()
    }

    private fun stopIdleBehaviors()
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(prefsListener) {
        idleEngine.stop()
    }



    fun showSpeechBubble(text: String, dosComX: Int, dosComY: Int) {
        val speechBubble = com.devenlucaz.doscom.ui.SpeechBubble(this)
        speechBubble.setText(text)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val screenHeight = ScreenMetrics.getScreenHeight(this)
        val sizePx = (80 * resources.displayMetrics.density).toInt()
        val bubbleOffset = (100 * resources.displayMetrics.density).toInt()

        params.x = dosComX
        val isAbove = dosComY > screenHeight / 2
        speechBubble.setDirection(isAbove)
        
        if (isAbove) {
            params.y = kotlin.math.max(0, dosComY - bubbleOffset)
        } else {
            params.y = dosComY + sizePx + (20 * resources.displayMetrics.density).toInt()
        }

        try {
            windowManager.addView(speechBubble, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showConfirmRing(x: Int, y: Int, onComplete: () -> Unit = {}) {
        val sizePx = (100 * resources.displayMetrics.density).toInt()
        val ringView = com.devenlucaz.doscom.ui.ConfirmRing(this, windowManager, onComplete)

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x - sizePx / 2
            this.y = y - sizePx / 2
        }

        try {
            windowManager.addView(ringView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showChatInput() {
        val chatOverlay = com.devenlucaz.doscom.ui.ChatInputOverlay(
            this,
            windowManager,
            lastScreenshot,
            onQuerySubmitted = { query, screenshot ->
                handleQuery(query, screenshot)
            },
            onClose = {
                // Restore flags when dismissed
                layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(overlayView, layoutParams)
                idleEngine.interact()
                idleEngine.targetState.mouthExpression = 0
                idleEngine.targetState.leftArmAngle = 0f
                idleEngine.targetState.bodyOffsetY = 0f
            },
            onVoiceStart = {
                idleEngine.interact()
                idleEngine.targetState.mouthExpression = 1 // LISTEN
            },
            onVoiceError = {
                showSpeechBubble("Couldn't hear that, try typing?", layoutParams.x, layoutParams.y)
                idleEngine.interact()
                idleEngine.targetState.mouthExpression = 0
                idleEngine.targetState.leftArmAngle = 0f
                idleEngine.targetState.bodyOffsetY = 0f
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
                }, 3000) // changed from 30000ms to 3000ms for responsiveness
            }
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(chatOverlay, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleQuery(query: String, screenshot: Bitmap?) {
        layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        windowManager.updateViewLayout(overlayView, layoutParams)
        
        showSpeechBubble("Let me look...", layoutParams.x, layoutParams.y)

        serviceScope.launch {
            val accessibilityService = DosComAccessibilityService.instance
            val charSizePx = (80 * resources.displayMetrics.density).toInt()
            
            val target = ScreenReader.findTarget(
                this@CompanionOverlayService,
                query,
                accessibilityService,
                screenshot,
                charSizePx
            )

            withContext(Dispatchers.Main) {
                if (target == null) {
                    showSpeechBubble("Hmm, I couldn't find that.", layoutParams.x, layoutParams.y)
                    idleEngine.targetState.mouthExpression = 2 // REACT_WORRY
                } else {
                    val targetCenterX = target.x
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
                    
                    val visualOffsetPx = (58 * resources.displayMetrics.density).toInt()
                    finalX = kotlin.math.max(-visualOffsetPx, kotlin.math.min(finalX, screenWidth - charSize + visualOffsetPx))
                    finalY = kotlin.math.max(-paddingPx, kotlin.math.min(finalY, screenHeight - charSize + paddingPx))

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
                    }
                }
            }
        }
    }

    private fun handleNotificationReaction(reactionType: String) {
        when (reactionType) {
            DosComNotificationListener.REACTION_WAVE -> {
                idleEngine.interact()
                idleEngine.targetState.leftArmAngle = -160f
                Handler(Looper.getMainLooper()).postDelayed({
                    idleEngine.interact()
                idleEngine.targetState.mouthExpression = 0
                idleEngine.targetState.leftArmAngle = 0f
                idleEngine.targetState.bodyOffsetY = 0f
                }, 2000)
            }
            DosComNotificationListener.REACTION_WORRY -> {
                idleEngine.targetState.mouthExpression = 2 // REACT_WORRY
                showSpeechBubble("Low battery!", layoutParams.x, layoutParams.y)
                Handler(Looper.getMainLooper()).postDelayed({
                    idleEngine.interact()
                idleEngine.targetState.mouthExpression = 0
                idleEngine.targetState.leftArmAngle = 0f
                idleEngine.targetState.bodyOffsetY = 0f
                }, 3000)
            }
            DosComNotificationListener.REACTION_HAPPY -> {
                idleEngine.interact()
                idleEngine.targetState.mouthExpression = 1
                idleEngine.targetState.bodyOffsetY = -10f
                Handler(Looper.getMainLooper()).postDelayed({
                    idleEngine.interact()
                idleEngine.targetState.mouthExpression = 0
                idleEngine.targetState.leftArmAngle = 0f
                idleEngine.targetState.bodyOffsetY = 0f
                }, 2000)
            }
        }
    }

    private fun handleAppCategoryReaction(categoryName: String) {
        when (categoryName) {
            "MUSIC" -> idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.BOOMBOX
            "CAMERA" -> idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.BINOCULARS
            "MAPS" -> idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.TREASURE_MAP
            "CALCULATOR" -> idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.ABACUS
            else -> idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.NONE
        }
        Handler(Looper.getMainLooper()).postDelayed({
            idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.NONE
        }, 5000)
    }
}
