package com.devenlucaz.doscom.events

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

enum class AppCategory {
    MUSIC, VIDEO, GAMING, CAMERA, MAPS, CALCULATOR, SOCIAL, NEW_INSTALL, OTHER
}

class AppContextWatcher(private val context: Context) {
    private var lastPackage = ""
    private var isRunning = false
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun start() {
        isRunning = true
        Thread {
            while (isRunning) {
                checkApp()
                Thread.sleep(1000)
            }
        }.start()
    }

    fun stop() {
        isRunning = false
    }

    private fun checkApp() {
        val time = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(time - 1000 * 10, time)
        var currentApp = lastPackage
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentApp = event.packageName
            }
        }
        
        if (currentApp != lastPackage && currentApp.isNotEmpty()) {
            lastPackage = currentApp
            val category = categorizeApp(currentApp)
            val intent = Intent("APP_CONTEXT_CHANGED").apply {
                putExtra("category", category.name)
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

    private fun categorizeApp(pkg: String): AppCategory {
        return when {
            pkg.contains("spotify") || pkg.contains("music") -> AppCategory.MUSIC
            pkg.contains("youtube") || pkg.contains("netflix") -> AppCategory.VIDEO
            pkg.contains("game") || pkg.contains("play") -> AppCategory.GAMING
            pkg.contains("camera") -> AppCategory.CAMERA
            pkg.contains("map") || pkg.contains("navigation") -> AppCategory.MAPS
            pkg.contains("calc") -> AppCategory.CALCULATOR
            pkg.contains("instagram") || pkg.contains("tiktok") || pkg.contains("twitter") || pkg.contains("facebook") -> AppCategory.SOCIAL
            else -> AppCategory.OTHER
        }
    }
}
