package com.devenlucaz.doscom.brain

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.util.Calendar

object BrainInput {
    fun buildInputs(context: Context): FloatArray {
        val inputs = FloatArray(8)
        
        // 0: battery level (0.0 to 1.0)
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat() / 100f
        }
        inputs[0] = batteryPct ?: 0.5f

        // 1: timeOfDay (0.0 to 1.0)
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        inputs[1] = (hour * 60 + minute) / (24f * 60f)

        // 2: user activity level (mock for now, assume 0.5)
        inputs[2] = 0.5f

        // 3: session length (mock for now, assume 0.5)
        inputs[3] = 0.5f

        // 4: recent interaction sentiment (stub from Phase 9, assume 0.5)
        inputs[4] = 0.5f

        // 5: screen position (mock for now, assume 0.5)
        inputs[5] = 0.5f

        // 6: app category (mock for now, assume 0.5)
        inputs[6] = 0.5f

        // 7: idle duration (mock for now, assume 0.5)
        inputs[7] = 0.5f

        return inputs
    }
}
