package com.devenlucaz.doscom

import android.app.Application
import android.content.Context
import android.util.Log

class DosComApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            val stackTrace = Log.getStackTraceString(e)
            getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_crash", stackTrace)
                .commit()
                
            defaultHandler?.uncaughtException(thread, e)
        }
    }
}
