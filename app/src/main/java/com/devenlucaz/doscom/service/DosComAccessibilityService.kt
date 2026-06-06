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
        // Empty for now, to be implemented in Phase 4.2
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
