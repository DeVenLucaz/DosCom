package com.devenlucaz.doscom.events

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import com.devenlucaz.doscom.animation.IdleAnimationEngine
import com.devenlucaz.doscom.character.PropType

class PhoneEventReceiver(
    private val engine: IdleAnimationEngine,
    private val onWalkToBottom: () -> Unit
) : BroadcastReceiver() {

    private var isLowBattery = false
    private val handler = Handler(Looper.getMainLooper())

    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        context.registerReceiver(this, filter)
    }

    fun unregister(context: Context) {
        try { context.unregisterReceiver(this) } catch (e: Exception) {}
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                if (isLowBattery) {
                    engine.targetState.bodyOffsetY = 15f
                    engine.targetState.eyesHalf = true
                    handler.postDelayed({ doChargeSequence() }, 1000)
                } else {
                    doChargeSequence()
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                engine.targetState.activeProp = PropType.NONE
                engine.targetState.eyesWide = true
                handler.postDelayed({ engine.targetState.eyesWide = false }, 1000)
            }
            Intent.ACTION_BATTERY_CHANGED -> {}
            Intent.ACTION_BATTERY_LOW -> {
                isLowBattery = true
                engine.targetState.mouthExpression = 2 
                engine.targetState.bodyOffsetY = 5f
                engine.targetState.eyesHalf = true
            }
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", 0)
                if (state == 1) {
                    engine.targetState.activeProp = PropType.BOOMBOX
                    engine.targetState.bodyOffsetY = -10f
                    handler.postDelayed({ engine.targetState.bodyOffsetY = 0f }, 300)
                } else {
                    engine.targetState.activeProp = PropType.NONE
                }
            }
            Intent.ACTION_SCREEN_OFF -> {
                engine.targetState.eyesClosed = true
                engine.targetState.bodyOffsetY = 10f
            }
            Intent.ACTION_SCREEN_ON -> {
                engine.targetState.eyesClosed = false
                engine.targetState.eyesWide = true
                engine.targetState.bodyOffsetY = 0f
                handler.postDelayed({ engine.targetState.eyesWide = false }, 1000)
            }
        }
    }

    private fun doChargeSequence() {
        onWalkToBottom()
        handler.postDelayed({
            engine.targetState.leftLegAngle = -90f
            engine.targetState.rightLegAngle = -90f
            engine.targetState.mouthExpression = 1
        }, 1000)
    }
}
