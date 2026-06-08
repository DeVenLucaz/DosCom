package com.devenlucaz.doscom

import android.app.Application
import android.content.Context
import android.util.Log

class DosComApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("last_crash", true)
                .commit()
                
            defaultHandler?.uncaughtException(thread, e)
        }
    }
}
