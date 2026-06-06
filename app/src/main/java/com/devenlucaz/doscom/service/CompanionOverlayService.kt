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

class CompanionOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: CompanionRenderer
    private lateinit var layoutParams: WindowManager.LayoutParams

    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = object : Runnable {
        override fun run() {
            scheduleNextIdleBehavior()
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        setupOverlayView()
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

        overlayView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                    windowManager.updateViewLayout(overlayView, layoutParams)
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    val newX = initialX + deltaX
                    val newY = initialY + deltaY

                    val screenWidth = ScreenMetrics.getScreenWidth(this)
                    val screenHeight = ScreenMetrics.getScreenHeight(this)
                    val statusBarHeight = ScreenMetrics.getStatusBarHeight(this)

                    layoutParams.x = max(0, min(newX, screenWidth - view.width))
                    layoutParams.y = max(0, min(newY, screenHeight - view.height - statusBarHeight))

                    windowManager.updateViewLayout(overlayView, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    windowManager.updateViewLayout(overlayView, layoutParams)
                    snapToNearestEdge()
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
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopIdleBehaviors()
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
}
