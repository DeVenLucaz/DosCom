package com.devenlucaz.doscom.mode

import android.content.Context

enum class TouchMode { INTERACTIVE, SEMI_GHOST, FULL_GHOST }

object TouchModeManager {
    fun getMode(context: Context): TouchMode {
        val prefs = context.getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        val modeInt = prefs.getInt("ghost_mode", 0)
        return TouchMode.values().getOrElse(modeInt) { TouchMode.INTERACTIVE }
    }

    fun setMode(context: Context, mode: TouchMode) {
        val prefs = context.getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("ghost_mode", mode.ordinal).apply()
    }
}
