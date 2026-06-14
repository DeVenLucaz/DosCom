package com.devenlucaz.doscom.brain

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.util.Calendar

object BrainInput {
    fun buildInputs(context: Context): FloatArray {
        val inputs = FloatArray(16)
        
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

        // 2: user activity level
        inputs[2] = 0.5f

        // 3: session length
        inputs[3] = 0.5f

        // 4: recent interaction sentiment
        inputs[4] = com.devenlucaz.doscom.personality.EmotionalMemory.getSentiment(context)

        // 5: Personality trait: Explorer
        inputs[5] = if (com.devenlucaz.doscom.brain.PersonalityGrowth.getDominantType() == com.devenlucaz.doscom.brain.PersonalityType.EXPLORER) 1.0f else 0.0f

        // 6: Personality trait: Playful
        inputs[6] = if (com.devenlucaz.doscom.brain.PersonalityGrowth.getDominantType() == com.devenlucaz.doscom.brain.PersonalityType.PLAYFUL) 1.0f else 0.0f

        // 7: Personality trait: Talkative/Curious
        inputs[7] = if (com.devenlucaz.doscom.brain.PersonalityGrowth.getDominantType() == com.devenlucaz.doscom.brain.PersonalityType.CURIOUS) 1.0f else 0.5f

        // --- NEW INPUTS ---
        
        // 8: Random environmental noise (injects chaos)
        inputs[8] = Math.random().toFloat()
        
        // 9: Is Charging? (Logical input for Left Brain)
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        inputs[9] = if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) 1.0f else 0.0f
        
        // 10: Is Night Time? (Boosts sleep/chill probability)
        inputs[10] = if (hour >= 22 || hour < 6) 1.0f else 0.0f
        
        // 11: Is Morning? (Boosts active/work probability)
        inputs[11] = if (hour in 6..11) 1.0f else 0.0f
        
        // 12: Attention Starvation (increases as time passes)
        inputs[12] = 0.5f // TODO: wire to actual interaction timer
        
        // 13: High Energy (Inverse of battery & time)
        inputs[13] = if (inputs[0] > 0.4f && inputs[10] == 0.0f) 1.0f else 0.0f
        
        // 14: Screen Orientation (0 = portrait, 1 = landscape)
        inputs[14] = if (context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 1.0f else 0.0f
        
        // 15: Last Drag Velocity
        inputs[15] = 0.5f // Default

        return inputs
    }
}
