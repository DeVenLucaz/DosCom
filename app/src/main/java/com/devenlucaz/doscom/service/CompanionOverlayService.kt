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
import com.devenlucaz.doscom.character.CharacterState
import com.devenlucaz.doscom.character.CompanionRenderer
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

import com.devenlucaz.doscom.character.CompanionAnimator
import com.devenlucaz.doscom.screen.ScreenReader
import com.devenlucaz.doscom.service.DosComAccessibilityService
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class CompanionOverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: CompanionRenderer
    private lateinit var layoutParams: WindowManager.LayoutParams
    
    var lastScreenshot: Bitmap? = null

    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = object : Runnable {
        override fun run() {
            scheduleNextIdleBehavior()
        }
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val reactionType = intent?.getStringExtra(DosComNotificationListener.EXTRA_REACTION_TYPE) ?: return
            handleNotificationReaction(reactionType)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        try {
            startForegroundServiceNotification()
            setupOverlayView()
            LocalBroadcastManager.getInstance(this).registerReceiver(
                notificationReceiver,
                IntentFilter(DosComNotificationListener.ACTION_NOTIFICATION_REACTION)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun setupOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val sizePx = (80 * resources.displayMetrics.density).toInt()

        overlayView = CompanionRenderer(this)

        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

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

                overlayView.setState(CharacterState.LISTEN)

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
                return true
            }
        })

        overlayView.setOnTouchListener { view, event ->
            if (event == null) return@setOnTouchListener false
            
            gestureDetector.onTouchEvent(event)
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
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

                        layoutParams.x = max(0, min(initialX + deltaX, screenWidth - view.width))
                        layoutParams.y = max(0, min(initialY + deltaY, screenHeight - view.height - statusBarHeight))

                        windowManager.updateViewLayout(overlayView, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        snapToNearestEdge()
                    }
                    isDragging = false
                    true
                }
                else -> false
            }
        }

        windowManager.addView(overlayView, layoutParams)
        startIdleBehaviors()
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
        stopIdleBehaviors()
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
        val sizePx = (80 * resources.displayMetrics.density).toInt()
        val centerX = layoutParams.x + sizePx / 2
        val targetX = if (centerX < screenWidth / 2) 0 else screenWidth - sizePx

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

    private fun startIdleBehaviors() {
        scheduleNextIdleBehavior()
    }

    private fun stopIdleBehaviors() {
        idleHandler.removeCallbacks(idleRunnable)
    }

    private fun scheduleNextIdleBehavior() {
        val states = listOf(
            CharacterState.IDLE_BOB,
            CharacterState.IDLE_BLINK,
            CharacterState.IDLE_LOOK_LEFT,
            CharacterState.IDLE_LOOK_RIGHT
        )
        val weights = listOf(0.2f, 0.4f, 0.2f, 0.2f)
        val r = Random.nextFloat()
        var cumulative = 0f
        var selectedState = CharacterState.IDLE_BOB
        for (i in states.indices) {
            cumulative += weights[i]
            if (r <= cumulative) {
                selectedState = states[i]
                break
            }
        }
        
        overlayView.setState(selectedState)
        
        val nextDelay = Random.nextLong(3000, 8000)
        idleHandler.postDelayed(idleRunnable, nextDelay)
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
        if (dosComY > screenHeight / 2) {
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
                overlayView.setState(CharacterState.IDLE_BOB)
            },
            onVoiceStart = {
                overlayView.setState(CharacterState.LISTEN)
            },
            onVoiceError = {
                showSpeechBubble("Couldn't hear that, try typing?", layoutParams.x, layoutParams.y)
                overlayView.setState(CharacterState.IDLE_BOB)
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
                    overlayView.setState(CharacterState.REACT_WORRY)
                } else {
                    val targetCenterX = target.x + charSizePx / 2
                    val targetCenterY = target.y + charSizePx / 2

                    showConfirmRing(targetCenterX, targetCenterY) {
                        CompanionAnimator.walkTo(target.x, target.y, overlayView, layoutParams, windowManager) {
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
                        }
                    }
                }
            }
        }
    }

    private fun handleNotificationReaction(reactionType: String) {
        when (reactionType) {
            DosComNotificationListener.REACTION_WAVE -> {
                overlayView.setState(CharacterState.REACT_WAVE)
                Handler(Looper.getMainLooper()).postDelayed({
                    overlayView.setState(CharacterState.IDLE_BOB)
                }, 2000)
            }
            DosComNotificationListener.REACTION_WORRY -> {
                overlayView.setState(CharacterState.REACT_WORRY)
                showSpeechBubble("Low battery!", layoutParams.x, layoutParams.y)
                Handler(Looper.getMainLooper()).postDelayed({
                    overlayView.setState(CharacterState.IDLE_BOB)
                }, 3000)
            }
            DosComNotificationListener.REACTION_HAPPY -> {
                overlayView.setState(CharacterState.REACT_HAPPY)
                Handler(Looper.getMainLooper()).postDelayed({
                    overlayView.setState(CharacterState.IDLE_BOB)
                }, 2000)
            }
        }
    }
}
