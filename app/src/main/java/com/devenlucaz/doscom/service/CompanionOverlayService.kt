package com.devenlucaz.doscom.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
    
    private var semiGhostView: View? = null
    private var semiGhostLayoutParams: WindowManager.LayoutParams? = null
    private var initialPinchDist = 0f
    private var isPinching = false
    private var initialScaleForPinch = 1f
    private var doubleTapRunnable: Runnable? = null
    
    private val handler = Handler(Looper.getMainLooper())


    private lateinit var prefs: SharedPreferences
    private val conversationHistory = com.devenlucaz.doscom.personality.ConversationHistory()
    private val serviceStartTime = System.currentTimeMillis()
    private fun getSessionMinutes(): Int = ((System.currentTimeMillis() - serviceStartTime) / 60000).toInt()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "mascot_scale" -> {
                val scale = sharedPreferences.getInt("mascot_scale", 7)
                val scaleFloat = 0.5f + (scale * (1.5f / 14f)) // map 0-14 to 0.5-2.0
                idleEngine.targetState.scale = scaleFloat
                idleEngine.targetState.scaleX = scaleFloat
            }
            "ghost_mode" -> {
                val mode = sharedPreferences.getInt("ghost_mode", 0)
                updateGhostMode(mode)
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

    private val reactionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val reactionType = intent?.getStringExtra("reactionType") ?: return
            if (reactionType == "positive") {
                idleEngine.targetState.blushVisible = true
                idleEngine.targetState.bodyOffsetY = -25f
                handler.postDelayed({
                    idleEngine.targetState.blushVisible = false
                    idleEngine.targetState.bodyOffsetY = 0f
                }, 1500)
            } else if (reactionType == "negative") {
                idleEngine.targetState.antennaGlow = 0.2f
                idleEngine.targetState.mouthExpression = 2 
                handler.postDelayed({
                    idleEngine.targetState.antennaGlow = 1.0f
                    idleEngine.targetState.bodyOffsetY = 0f
                    idleEngine.targetState.mouthExpression = 0
                }, 1500)
            }
        }
    }

    private val repeatReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            idleEngine.targetState.bodyRotation = 10f
            idleEngine.targetState.scaleX = 1.1f
            idleEngine.targetState.eyesWide = true
            showSpeechBubble("psst... you good? 👀", layoutParams.x, layoutParams.y) {
                serviceScope.launch(Dispatchers.Main) {
                    try {
                        lastScreenshot = ScreenshotHelper.captureScreen(this@CompanionOverlayService)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(300)
                    layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                    updateRobotLayout()
                    showChatInput()
                }
            }
            handler.postDelayed({
                idleEngine.targetState.bodyRotation = 0f
                idleEngine.targetState.scaleX = 1f
                idleEngine.targetState.eyesWide = false
            }, 3000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_DISABLE_GHOST_MODE" -> {
                prefs.edit().putInt("ghost_mode", 0).apply()
                updateGhostMode(0)
                updateForegroundNotification()
            }
            "ACTION_TOGGLE_GHOST_MODE" -> {
                val currentGhostMode = prefs.getInt("ghost_mode", 0)
                val newMode = if (currentGhostMode == 2) 0 else 2
                prefs.edit().putInt("ghost_mode", newMode).apply()
                updateGhostMode(newMode)
                updateForegroundNotification()
            }
            "ACTION_STOP" -> stopSelf()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        updateForegroundNotification()
        try {
            com.devenlucaz.doscom.brain.BrainManager.init(this)
            
            // Crash Recovery check
            if (prefs.getBoolean("last_crash", false)) {
                prefs.edit().putBoolean("last_crash", false).apply()
                handler.postDelayed({
                    idleEngine.targetState.leftArmAngle = -45f
                    idleEngine.targetState.rightArmAngle = 45f
                    idleEngine.targetState.mouthExpression = 2
                    showSpeechBubble("oops \uD83D\uDE05", layoutParams.x, layoutParams.y) {}
                    handler.postDelayed({
                        idleEngine.targetState.leftArmAngle = 0f
                        idleEngine.targetState.rightArmAngle = 0f
                        idleEngine.targetState.mouthExpression = 0
                    }, 3000)
                }, 1000)
            }
            
            setupOverlayView()
            LocalBroadcastManager.getInstance(this).registerReceiver(
                notificationReceiver,
                IntentFilter(DosComNotificationListener.ACTION_NOTIFICATION_REACTION)
            )
            LocalBroadcastManager.getInstance(this).registerReceiver(
                appCategoryReceiver,
                IntentFilter("APP_CONTEXT_CHANGED")
            )
            LocalBroadcastManager.getInstance(this).registerReceiver(
                reactionReceiver,
                IntentFilter("com.devenlucaz.doscom.REACTION")
            )
            LocalBroadcastManager.getInstance(this).registerReceiver(
                repeatReceiver,
                IntentFilter("com.devenlucaz.doscom.REPEAT_TRIGGER")
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
        val speedInt = prefs.getInt("anim_speed", 2)
        val animSpeed = 0.5f + (speedInt * 0.25f)
        idleEngine.animSpeedMultiplier = animSpeed
        com.devenlucaz.doscom.animation.MimeEngine.animSpeedMultiplier = animSpeed
        com.devenlucaz.doscom.animation.ClimbEngine.animSpeedMultiplier = animSpeed
        idleEngine.sleepTimerMs = 5 * 60 * 1000L

        val screenWidth = ScreenMetrics.getScreenWidth(this)
        val screenHeight = ScreenMetrics.getScreenHeight(this)
        val visualOffsetPx = (58 * resources.displayMetrics.density).toInt()
        layoutParams = WindowManager.LayoutParams(
            paddedSizePx,
            paddedSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
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
                val reactionBox = com.devenlucaz.doscom.ui.ReactionBox(
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
                            updateRobotLayout()
                            showChatInput()
                        }
                    },
                    onReactedPositive = {
                        idleEngine.targetState.blushVisible = true
                        idleEngine.targetState.bodyOffsetY = -25f
                        handler.postDelayed({
                            idleEngine.targetState.blushVisible = false
                            idleEngine.targetState.bodyOffsetY = 0f
                        }, 1500)
                    },
                    onReactedNegative = {
                        idleEngine.targetState.antennaGlow = 0.2f
                        idleEngine.targetState.mouthExpression = 2
                        handler.postDelayed({
                            idleEngine.targetState.antennaGlow = 1.0f
                            idleEngine.targetState.mouthExpression = 0
                        }, 1500)
                    }
                )
                reactionBox.show()
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
                MotionEvent.ACTION_POINTER_DOWN -> {
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

                    if (isDragging) {
                        val screenWidth = ScreenMetrics.getScreenWidth(this)
                        val screenHeight = ScreenMetrics.getScreenHeight(this)
                        val statusBarHeight = ScreenMetrics.getStatusBarHeight(this)
                        val visualOffsetPx = (58 * resources.displayMetrics.density).toInt()

                        layoutParams.x = max(-visualOffsetPx, min(initialX + deltaX, screenWidth - view.width + visualOffsetPx))
                        layoutParams.y = max(-paddingPx, min(initialY + deltaY, screenHeight - view.height - statusBarHeight + paddingPx))

                        updateRobotLayout()
                        
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
                    isPinching = false
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
        prefsListener.onSharedPreferenceChanged(prefs, "ghost_mode")

        
        phoneEventReceiver = com.devenlucaz.doscom.events.PhoneEventReceiver(idleEngine) {
            val screenHeight = ScreenMetrics.getScreenHeight(this)
            val animator = ValueAnimator.ofInt(layoutParams.y, screenHeight - overlayView.height)
            animator.duration = 500
            animator.addUpdateListener { anim ->
                layoutParams.y = anim.animatedValue as Int
                updateRobotLayout()
            }
            animator.start()
        }
        phoneEventReceiver.register(this)

        appContextWatcher = com.devenlucaz.doscom.events.AppContextWatcher(this)
        appContextWatcher.start()

        timeReactionEngine = com.devenlucaz.doscom.events.TimeReactionEngine(this, idleEngine, windowManager)
        timeReactionEngine.start()
    }

    private fun updateForegroundNotification() {
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

        val currentMode = com.devenlucaz.doscom.mode.ModeManager.getMode(this)
        val modeStr = when (currentMode) {
            com.devenlucaz.doscom.mode.CompanionMode.ALIVE -> "ALIVE"
            com.devenlucaz.doscom.mode.CompanionMode.AWAKE -> "AWAKE"
            com.devenlucaz.doscom.mode.CompanionMode.AWARE -> "AWARE"
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("DosCom is running ($modeStr)")
            .setContentText("Your cosmic companion is active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        // Settings Action
        val settingsIntent = Intent(this, com.devenlucaz.doscom.settings.SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            this, 0, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Settings", settingsPendingIntent)

        // Ghost Mode Toggle Action
        val currentGhostMode = prefs.getInt("ghost_mode", 0)
        val ghostToggleIntent = Intent(this, CompanionOverlayService::class.java).apply {
            action = "ACTION_TOGGLE_GHOST_MODE"
        }
        val ghostTogglePendingIntent = PendingIntent.getService(
            this, 1, ghostToggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, if (currentGhostMode == 2) "Disable Ghost Mode" else "Enable Ghost Mode", ghostTogglePendingIntent)

        // Stop Action
        val stopIntent = Intent(this, CompanionOverlayService::class.java).apply {
            action = "ACTION_STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Stop", stopPendingIntent)

        val notification = builder.build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver) } catch (e: Exception) { e.printStackTrace() }
        try { LocalBroadcastManager.getInstance(this).unregisterReceiver(appCategoryReceiver) } catch (e: Exception) { e.printStackTrace() }
        try { LocalBroadcastManager.getInstance(this).unregisterReceiver(reactionReceiver) } catch (e: Exception) { e.printStackTrace() }
        try { LocalBroadcastManager.getInstance(this).unregisterReceiver(repeatReceiver) } catch (e: Exception) { e.printStackTrace() }
        try { if (::phoneEventReceiver.isInitialized) phoneEventReceiver.unregister(this) } catch (e: Exception) { e.printStackTrace() }
        try { if (::appContextWatcher.isInitialized) appContextWatcher.stop() } catch (e: Exception) { e.printStackTrace() }
        try { if (::timeReactionEngine.isInitialized) timeReactionEngine.stop() } catch (e: Exception) { e.printStackTrace() }
        try { stopIdleBehaviors() } catch (e: Exception) { e.printStackTrace() }
        try { if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(prefsListener) } catch (e: Exception) { e.printStackTrace() }
        try { serviceScope.cancel() } catch (e: Exception) { e.printStackTrace() }
        try {
            if (::overlayView.isInitialized) {
                windowManager.removeView(overlayView)
            }
            semiGhostView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
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
                updateRobotLayout()
            } catch (e: Exception) {
                // Ignore if view was removed
            }
        }
        animator.start()
    }

    fun triggerShakeReaction() {
        val screenWidth = com.devenlucaz.doscom.utils.ScreenMetrics.getScreenWidth(this)
        val viewW = overlayView.width
        val visualOffsetPx = (58 * resources.displayMetrics.density).toInt()
        val centerX = layoutParams.x + viewW / 2
        val isLeftEdge = centerX < screenWidth / 2
        val targetX = if (isLeftEdge) -visualOffsetPx else screenWidth - viewW + visualOffsetPx

        val animator = android.animation.ValueAnimator.ofInt(layoutParams.x, targetX)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            layoutParams.x = animation.animatedValue as Int
            try { updateRobotLayout() } catch (e: Exception) {}
        }
        animator.start()

        idleEngine.targetState.eyesWide = true
        if (isLeftEdge) {
            idleEngine.targetState.leftArmAngle = -90f
            idleEngine.targetState.rightArmAngle = 0f
            idleEngine.targetState.bodyRotation = 5f
            idleEngine.targetState.scaleX = 1f
        } else {
            idleEngine.targetState.leftArmAngle = 0f
            idleEngine.targetState.rightArmAngle = 90f
            idleEngine.targetState.bodyRotation = -5f
            idleEngine.targetState.scaleX = -1f
        }

        handler.postDelayed({
            idleEngine.targetState.eyesWide = false
            idleEngine.targetState.eyesHalf = true
            idleEngine.targetState.pupilOffsetX = if (isLeftEdge) 15f else -15f

            handler.postDelayed({
                idleEngine.targetState.eyesHalf = false
                idleEngine.targetState.pupilOffsetX = 0f
                idleEngine.targetState.leftArmAngle = 0f
                idleEngine.targetState.rightArmAngle = 0f
                idleEngine.targetState.bodyRotation = 0f
                idleEngine.targetState.scaleX = 1f
            }, 2000)
        }, 2000)
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

    private fun updateRobotLayout() {
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
    }

    private fun startIdleBehaviors() {
        prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        // trigger initial load
        prefsListener.onSharedPreferenceChanged(prefs, "mascot_scale")
        prefsListener.onSharedPreferenceChanged(prefs, "anim_speed")
        prefsListener.onSharedPreferenceChanged(prefs, "sleep_timer")
        prefsListener.onSharedPreferenceChanged(prefs, "ghost_mode")

        idleEngine.start()
    }

    private fun stopIdleBehaviors() {
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        idleEngine.stop()
    }



    fun showSpeechBubble(text: String, dosComX: Int, dosComY: Int, onClick: (() -> Unit)? = null) {
        val speechBubble = com.devenlucaz.doscom.ui.SpeechBubble(this)
        speechBubble.setText(text)
        speechBubble.onBubbleClick = onClick

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
                updateRobotLayout()
                idleEngine.interact()
                
                handler.postDelayed({
                    idleEngine.targetState.mouthExpression = 0
                    idleEngine.targetState.leftArmAngle = 0f
                    idleEngine.targetState.bodyOffsetY = 0f
                }, 2000)
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
        val currentMode = com.devenlucaz.doscom.mode.ModeManager.getMode(this)
        
        idleEngine.interact()
        layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        updateRobotLayout()

        com.devenlucaz.doscom.personality.MoodEngine.detectFromChat(query)?.let { mood ->
            com.devenlucaz.doscom.personality.MoodEngine.currentMood = mood
            com.devenlucaz.doscom.personality.MoodEngine.applyMoodToAnimation(idleEngine)
        }

        if (currentMode == com.devenlucaz.doscom.mode.CompanionMode.ALIVE) {
            showSpeechBubble("...", layoutParams.x, layoutParams.y)
            return
        }

        val qLower = query.lowercase()
        val isLocateRequest = qLower.startsWith("find") || qLower.startsWith("where is") || qLower.startsWith("show me") || qLower.startsWith("locate")

        if (!isLocateRequest && (currentMode == com.devenlucaz.doscom.mode.CompanionMode.AWAKE || currentMode == com.devenlucaz.doscom.mode.CompanionMode.AWARE)) {
            com.devenlucaz.doscom.personality.EmotionalMemory.recordPositive(this, 0.05f)
            showSpeechBubble("Hmm...", layoutParams.x, layoutParams.y)
            idleEngine.targetState.mouthExpression = 0
            
            var screenCtx = ""
            var passScreenshot: Bitmap? = null
            if (currentMode == com.devenlucaz.doscom.mode.CompanionMode.AWARE) {
                screenCtx = ScreenReader.buildScreenContext(DosComAccessibilityService.instance)
                if (qLower.contains("can you see") || qLower.contains("look at this") || qLower.contains("show you") || qLower.contains("see this")) {
                    passScreenshot = screenshot
                }
            }

            serviceScope.launch {
                val response = com.devenlucaz.doscom.api.GeminiVisionClient.speak(
                    trigger = query,
                    screenContext = screenCtx,
                    history = conversationHistory,
                    apiKey = com.devenlucaz.doscom.utils.ConfigManager.loadApiKey(this@CompanionOverlayService) ?: "",
                    mood = com.devenlucaz.doscom.personality.MoodEngine.currentMood,
                    appName = "Unknown",
                    sessionMinutes = getSessionMinutes(),
                    screenshot = passScreenshot
                )
                
                withContext(Dispatchers.Main) {
                    if (response != null) {
                        showSpeechBubble(response, layoutParams.x, layoutParams.y)
                        conversationHistory.addMessage("user", query)
                        conversationHistory.addMessage("model", response)
                    } else {
                        showSpeechBubble("...?", layoutParams.x, layoutParams.y)
                    }
                }
            }
            return
        }

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
                    showSpeechBubble("👀 not there...", layoutParams.x, layoutParams.y)
                    idleEngine.targetState.bodyRotation = 15f
                    handler.postDelayed({
                        idleEngine.targetState.bodyRotation = -15f
                        handler.postDelayed({
                            idleEngine.targetState.bodyRotation = 0f
                        }, 150)
                    }, 150)
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
                        val currentMood = com.devenlucaz.doscom.personality.MoodEngine.currentMood
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
                                updateRobotLayout()
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
                        )
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
