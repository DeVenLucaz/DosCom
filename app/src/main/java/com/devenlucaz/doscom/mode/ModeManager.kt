package com.devenlucaz.doscom.mode

import android.content.Context

object ModeManager {
    private const val PREFS_NAME = "doscom_prefs"
    private const val KEY_MODE = "companion_mode"

    fun getMode(context: Context): CompanionMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeString = prefs.getString(KEY_MODE, CompanionMode.ALIVE.name)
        return try {
            CompanionMode.valueOf(modeString ?: CompanionMode.ALIVE.name)
        } catch (e: IllegalArgumentException) {
            CompanionMode.ALIVE
        }
    }

    fun setMode(context: Context, mode: CompanionMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun cycleMode(context: Context): CompanionMode {
        val currentMode = getMode(context)
        val newMode = when (currentMode) {
            CompanionMode.ALIVE -> CompanionMode.AWAKE
            CompanionMode.AWAKE -> CompanionMode.AWARE
            CompanionMode.AWARE -> CompanionMode.ALIVE
        }
        setMode(context, newMode)
        return newMode
    }
}
