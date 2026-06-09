package com.devenlucaz.doscom.events

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.devenlucaz.doscom.animation.IdleAnimationEngine
import android.database.ContentObserver
import android.provider.MediaStore
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.View
import android.graphics.PixelFormat
import java.util.Calendar

class TimeReactionEngine(
    private val context: Context,
    private val engine: IdleAnimationEngine,
    private val windowManager: WindowManager
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    
    private var lastShakeTime = 0L
    private var keyboardView: View? = null

    private val ringerObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            checkRingerMode()
        }
    }

    private val screenshotObserver = object : ContentObserver(handler) {
        private var lastScreenshotTime = 0L
        override fun onChange(selfChange: Boolean) {
            val now = System.currentTimeMillis()
            if (now - lastScreenshotTime > 2000) {
                lastScreenshotTime = now
                triggerPhotobomb()
            }
        }
    }

    private val unlockReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            if (intent?.action == android.content.Intent.ACTION_USER_PRESENT) {
                checkBirthdays()
            }
        }
    }

    fun start() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL)
        
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, ringerObserver
        )
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, screenshotObserver
        )

        val unlockFilter = android.content.IntentFilter(android.content.Intent.ACTION_USER_PRESENT)
        context.registerReceiver(unlockReceiver, unlockFilter)
        
        checkTime()
        checkRingerMode()
        checkAirplaneMode()
        setupKeyboardDetector()
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        context.contentResolver.unregisterContentObserver(ringerObserver)
        context.contentResolver.unregisterContentObserver(screenshotObserver)
        try { context.unregisterReceiver(unlockReceiver) } catch (e: Exception) {}
        if (keyboardView != null) {
            try { windowManager.removeView(keyboardView) } catch (e: Exception) {}
        }
    }

    private fun checkTime() {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (hour in 5..6) {
            engine.targetState.eyesHalf = true
            engine.targetState.mouthExpression = 2 
        } else if (hour < 5) {
            engine.targetState.bodyOffsetY = 10f 
        }
    }

    private fun checkRingerMode() {
        val mode = audioManager.ringerMode
        if (mode == AudioManager.RINGER_MODE_SILENT) {
            engine.animSpeedMultiplier = 0.5f
        } else {
            engine.animSpeedMultiplier = 1.0f
        }
    }

    private fun checkAirplaneMode() {
        val isAirplaneMode = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
        if (isAirplaneMode) {
            engine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.PILOT_HAT
            engine.targetState.leftArmAngle = -90f
            engine.targetState.rightArmAngle = 90f
        }
    }

    private fun checkBirthdays() {
        if (com.devenlucaz.doscom.systems.BirthdaySystem.isUserBirthday(context)) {
            engine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.PARTY_HAT
            
            when (com.devenlucaz.doscom.systems.BirthdaySystem.getBirthdayDayPhase()) {
                com.devenlucaz.doscom.systems.BirthdayPhase.MIDNIGHT_UNLOCK -> {
                    if (context is com.devenlucaz.doscom.service.CompanionOverlayService) {
                        context.showSpeechBubble("🎂", 0, 0)
                    }
                }
                com.devenlucaz.doscom.systems.BirthdayPhase.MORNING -> {
                    engine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.GIFT_BOX
                    handler.postDelayed({
                        if (engine.targetState.activeProp == com.devenlucaz.doscom.character.PropType.GIFT_BOX) {
                            engine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.PARTY_HAT
                        }
                    }, 3000)
                }
                com.devenlucaz.doscom.systems.BirthdayPhase.AFTERNOON -> {
                    engine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.TINY_CAKE
                    handler.postDelayed({
                        if (engine.targetState.activeProp == com.devenlucaz.doscom.character.PropType.TINY_CAKE) {
                            engine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.PARTY_HAT
                            engine.targetState.mouthExpression = 0
                        }
                    }, 5000)
                }
                com.devenlucaz.doscom.systems.BirthdayPhase.EVENING -> {
                    engine.targetState.bodyRotation = 10f
                    if (context is com.devenlucaz.doscom.service.CompanionOverlayService) {
                        context.showSpeechBubble("♥", 0, 0)
                    }
                }
            }
        } else if (com.devenlucaz.doscom.systems.BirthdaySystem.isDosCombBirthday(context)) {
            engine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.TINY_CAKE
            engine.targetState.scaleX = 1.2f
            handler.postDelayed({ engine.targetState.scaleX = 1.0f }, 3000)
        }
    }

    private fun triggerPhotobomb() {
        engine.targetState.leftArmAngle = -160f
        engine.targetState.rightArmAngle = -160f
        engine.targetState.mouthExpression = 1
        engine.targetState.eyesWide = true
        handler.postDelayed({
            engine.targetState.leftArmAngle = 0f
            engine.targetState.rightArmAngle = 0f
            engine.targetState.mouthExpression = 0
            engine.targetState.eyesWide = false
        }, 2000)
    }

    private fun setupKeyboardDetector() {
        keyboardView = View(context)
        val params = WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSPARENT
        )
        try {
            windowManager.addView(keyboardView, params)
            keyboardView?.viewTreeObserver?.addOnGlobalLayoutListener {
                val heightDiff = keyboardView!!.rootView.height - keyboardView!!.height
                if (heightDiff > 150 * context.resources.displayMetrics.density) {
                    engine.targetState.bodyOffsetY = -30f
                    engine.targetState.eyesWide = true
                } else {
                    engine.targetState.bodyOffsetY = 0f
                    engine.targetState.eyesWide = false
                }
            }
        } catch (e: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val accel = Math.sqrt((x*x + y*y + z*z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH
            
            if (accel > 15f) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > 5000) {
                    lastShakeTime = now
                    if (context is com.devenlucaz.doscom.service.CompanionOverlayService) {
                        context.triggerShakeReaction()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
