package com.devenlucaz.doscom.service

import android.content.Context
import android.content.Intent
import android.os.Build

object ServiceManager {

    fun startOverlayService(context: Context) {
        val intent = Intent(context, CompanionOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopOverlayService(context: Context) {
        val intent = Intent(context, CompanionOverlayService::class.java)
        context.stopService(intent)
    }
}
