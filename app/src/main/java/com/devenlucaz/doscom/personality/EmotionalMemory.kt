package com.devenlucaz.doscom.personality

import android.content.Context
import android.content.SharedPreferences

object EmotionalMemory {
    private const val PREFS_NAME = "doscom_emotion_prefs"
    private const val KEY_SENTIMENT = "sentiment_score"
    private const val KEY_LAST_TICK = "last_decay_tick"

    private var currentSentiment: Float = 0.0f
    private var initialized = false

    private fun initIfNeeded(context: Context) {
        if (!initialized) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            currentSentiment = prefs.getFloat(KEY_SENTIMENT, 0.0f)
            applyDailyDecay(prefs)
            initialized = true
        }
    }

    private fun applyDailyDecay(prefs: SharedPreferences) {
        val lastTick = prefs.getLong(KEY_LAST_TICK, 0L)
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        
        if (now - lastTick > oneDayMs) {
            val daysElapsed = ((now - lastTick) / oneDayMs).toInt()
            for (i in 0 until daysElapsed) {
                currentSentiment *= 0.9f // Natural decay towards 0
            }
            if (Math.abs(currentSentiment) < 0.01f) {
                currentSentiment = if (currentSentiment > 0) 0.01f else -0.01f
            }
            prefs.edit()
                .putFloat(KEY_SENTIMENT, currentSentiment)
                .putLong(KEY_LAST_TICK, now)
                .apply()
        }
    }

    fun recordPositive(context: Context, weight: Float = 0.1f) {
        initIfNeeded(context)
        currentSentiment = (currentSentiment + weight).coerceAtMost(1.0f)
        save(context)
    }

    fun recordNegative(context: Context, weight: Float = 0.05f) {
        initIfNeeded(context)
        currentSentiment = (currentSentiment - weight).coerceAtLeast(-1.0f)
        save(context)
    }

    fun getSentiment(context: Context): Float {
        initIfNeeded(context)
        return currentSentiment
    }

    fun getEffectMultiplier(context: Context): Float {
        val sentiment = getSentiment(context)
        // map -1.0..1.0 to 0.5..1.5
        return 1.0f + (sentiment * 0.5f)
    }

    private fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_SENTIMENT, currentSentiment).apply()
    }
}
