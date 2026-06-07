package com.devenlucaz.doscom.service

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class DosComNotificationListener : NotificationListenerService() {
    companion object {
        const val TAG = "DosComNotifListener"
        const val ACTION_NOTIFICATION_REACTION = "com.devenlucaz.doscom.NOTIFICATION_REACTION"
        const val EXTRA_REACTION_TYPE = "reaction_type"
        const val REACTION_WAVE = "wave"
        const val REACTION_WORRY = "worry"
        const val REACTION_HAPPY = "happy"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        // Skip our own notifications
        if (sbn.packageName == applicationContext.packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras
        val category = notification.category

        val reactionType = when {
            // Battery related
            sbn.packageName.contains("battery", ignoreCase = true) ||
                category == android.app.Notification.CATEGORY_STATUS -> REACTION_WORRY
            // Charging connected
            sbn.packageName.contains("charging", ignoreCase = true) ||
                category == android.app.Notification.CATEGORY_SYSTEM -> REACTION_HAPPY
            // Messages / general notifications
            extras?.getCharSequence("android.text") != null -> REACTION_WAVE
            // Default
            else -> REACTION_WAVE
        }

        Log.d(TAG, "Notification from ${sbn.packageName}, reaction: $reactionType")

        val intent = Intent(ACTION_NOTIFICATION_REACTION).apply {
            putExtra(EXTRA_REACTION_TYPE, reactionType)
        }
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed
    }
}
