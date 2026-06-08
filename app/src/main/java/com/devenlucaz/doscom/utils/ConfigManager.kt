package com.devenlucaz.doscom.utils

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

object ConfigManager {
    private const val TAG = "ConfigManager"
    private const val CONFIG_FILE = "config.json"
    private const val KEY_API_KEY = "gemini_vision_api_key"

    fun saveApiKey(context: Context, key: String) {
        try {
            val configFile = File(context.filesDir, CONFIG_FILE)
            val json = JSONObject()
            json.put(KEY_API_KEY, key)
            configFile.writeText(json.toString())
            configFile.setReadable(true, true)
            configFile.setWritable(true, true)
            Log.d(TAG, "API key saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save API key", e)
        }
    }

    fun loadApiKey(context: Context): String? {
        return try {
            val configFile = File(context.filesDir, CONFIG_FILE)
            if (!configFile.exists()) return null
            val json = JSONObject(configFile.readText())
            val key = json.optString(KEY_API_KEY, "")
            if (key.isNullOrBlank()) null else key
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load API key", e)
            null
        }
    }

    fun hasApiKey(context: Context): Boolean = loadApiKey(context) != null
}
