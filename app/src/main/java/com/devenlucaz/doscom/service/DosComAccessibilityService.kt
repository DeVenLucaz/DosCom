package com.devenlucaz.doscom.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class DosComAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DosComAccessibility"
        var instance: DosComAccessibilityService? = null
            private set

        fun isConnected(): Boolean {
            return instance != null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "DosComAccessibilityService connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val currentMode = com.devenlucaz.doscom.mode.ModeManager.getMode(this)
        
        val prefs = getSharedPreferences("doscom_prefs", android.content.Context.MODE_PRIVATE)
        val passiveObservation = prefs.getBoolean("passive_observation", true)
        
        if (currentMode == com.devenlucaz.doscom.mode.CompanionMode.AWARE && passiveObservation) {
            val pkg = event.packageName?.toString() ?: ""
            val node = event.source
            val viewId = node?.viewIdResourceName
            if (pkg.isNotEmpty() && !viewId.isNullOrEmpty()) {
                com.devenlucaz.doscom.observation.RepeatDetector.onEvent(pkg, viewId) {
                    val intent = android.content.Intent("com.devenlucaz.doscom.REPEAT_TRIGGER")
                    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "DosComAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            Log.d(TAG, "DosComAccessibilityService disconnected")
        }
    }
}
