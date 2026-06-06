package com.devenlucaz.doscom

import android.app.Application
import android.content.Context
import android.util.Log

class DosComApplication : Application() {
    override fun onCreate() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            val stackTrace = android.util.Log.getStackTraceString(e)
            getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_crash", stackTrace)
                .commit()
                
            defaultHandler?.uncaughtException(thread, e)
        }
        
        super.onCreate()
        
        try {
            val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
            val lastCrash = prefs.getString("last_crash", null)
            if (lastCrash != null) {
                prefs.edit().remove("last_crash").apply()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(this, "CRASH: \n" + lastCrash.take(200), android.widget.Toast.LENGTH_LONG).show()
                }
                
                try {
                    val file = java.io.File(getExternalFilesDir(null), "doscom_crash.txt")
                    file.writeText(lastCrash)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
