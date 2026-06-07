package com.devenlucaz.doscom.systems

import android.content.Context
import android.graphics.PointF

object HomeCornerSystem {
    private var homeCorner: PointF? = null
    
    fun recordIdlePosition(context: Context, x: Int, y: Int) {
        val prefs = context.getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        val key = "pos_${x / 100}_${y / 100}"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
        
        if (count > 50) {
            homeCorner = PointF(x.toFloat(), y.toFloat())
        }
    }
    
    fun getHomeCorner(): PointF? {
        return homeCorner
    }
    
    fun isAtHome(x: Float, y: Float): Boolean {
        val home = homeCorner ?: return false
        val dx = x - home.x
        val dy = y - home.y
        return Math.sqrt((dx*dx + dy*dy).toDouble()) < 150.0
    }
}
