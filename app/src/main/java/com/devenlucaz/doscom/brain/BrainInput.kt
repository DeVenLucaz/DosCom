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

        // 4: recent interaction sentiment
        inputs[4] = com.devenlucaz.doscom.personality.EmotionalMemory.getSentiment(context)

        // 5: Personality trait: Explorer
        inputs[5] = if (com.devenlucaz.doscom.brain.PersonalityGrowth.getDominantType() == com.devenlucaz.doscom.brain.PersonalityType.EXPLORER) 1.0f else 0.0f

        // 6: Personality trait: Playful
        inputs[6] = if (com.devenlucaz.doscom.brain.PersonalityGrowth.getDominantType() == com.devenlucaz.doscom.brain.PersonalityType.PLAYFUL) 1.0f else 0.0f

        // 7: Personality trait: Talkative/Curious (blended)
        inputs[7] = if (com.devenlucaz.doscom.brain.PersonalityGrowth.getDominantType() == com.devenlucaz.doscom.brain.PersonalityType.CURIOUS) 1.0f else 0.5f

        return inputs
    }
}
