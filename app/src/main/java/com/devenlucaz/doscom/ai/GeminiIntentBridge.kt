package com.devenlucaz.doscom.ai

import android.content.Context
import android.content.Intent
import android.net.Uri

object GeminiIntentBridge {

    fun sendToGemini(context: Context, query: String) {
        // Try deep link first
        val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://gemini.google.com/app?q=${Uri.encode(query)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (deepLinkIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(deepLinkIntent)
            return
        }
        // Fallback: launch Gemini app package directly
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage("com.google.android.apps.bard")
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            // Gemini not installed, open Play Store
            val playIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.google.android.apps.bard"))
            playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(playIntent)
        }
    }

    fun isGeminiInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(
            "com.google.android.apps.bard") != null
}
