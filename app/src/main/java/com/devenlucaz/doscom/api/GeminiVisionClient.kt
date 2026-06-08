package com.devenlucaz.doscom.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiVisionClient {
    private const val TAG = "GeminiVisionClient"
    private var apiKey: String? = null

    data class VisionResponse(
        val found: Boolean,
        val xPercent: Float,
        val yPercent: Float,
        val explanation: String,
        val elementDescription: String = ""
    )

    fun configure(key: String) {
        apiKey = key
    }

    fun isConfigured(): Boolean = !apiKey.isNullOrBlank()

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    suspend fun analyze(screenshot: Bitmap, query: String): VisionResponse? = withContext(Dispatchers.IO) {
        val key = apiKey ?: return@withContext null
        try {
            val base64Image = bitmapToBase64(screenshot)

            val prompt = """
You are a screen navigation assistant for Android.
The user is asking: "$query"

Look at this screenshot. Identify the single most relevant UI element to tap.
Respond ONLY in this exact JSON format, no markdown, no extra text:
{"element_description": "brief name of element", "x_percent": 0.0, "y_percent": 0.0, "explanation": "one sentence max 12 words", "found": true}
x_percent: 0.0 = left edge, 1.0 = right edge
y_percent: 0.0 = top edge, 1.0 = bottom edge
If no relevant element is visible, return found: false.
            """.trimIndent()

            val inlineData = JSONObject().apply {
                put("mime_type", "image/jpeg")
                put("data", base64Image)
            }
            val imagePart = JSONObject().apply {
                put("inline_data", inlineData)
            }
            val textPart = JSONObject().apply {
                put("text", prompt)
            }
            val parts = JSONArray().apply {
                put(imagePart)
                put(textPart)
            }
            val content = JSONObject().apply {
                put("parts", parts)
            }
            val contents = JSONArray().apply {
                put(content)
            }
            val requestBody = JSONObject().apply {
                put("contents", contents)
            }

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorStream = connection.errorStream
                val errorBody = errorStream?.bufferedReader()?.readText() ?: "No error body"
                Log.e(TAG, "API error $responseCode: $errorBody")
                return@withContext null
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            connection.disconnect()

            val responseJson = JSONObject(response)
            val candidates = responseJson.getJSONArray("candidates")
            if (candidates.length() == 0) return@withContext null

            val candidate = candidates.getJSONObject(0)
            val contentObj = candidate.getJSONObject("content")
            val partsArr = contentObj.getJSONArray("parts")
            if (partsArr.length() == 0) return@withContext null

            var text = partsArr.getJSONObject(0).getString("text").trim()

            // Strip markdown code fences if present
            if (text.startsWith("```")) {
                text = text.removePrefix("```json").removePrefix("```")
                text = text.removeSuffix("```")
                text = text.trim()
            }

            val resultJson = JSONObject(text)
            val found = resultJson.optBoolean("found", false)
            if (!found) return@withContext VisionResponse(found = false, 0f, 0f, "", "")

            VisionResponse(
                found = true,
                xPercent = resultJson.optDouble("x_percent", 0.5).toFloat(),
                yPercent = resultJson.optDouble("y_percent", 0.5).toFloat(),
                explanation = resultJson.optString("explanation", "Tap here"),
                elementDescription = resultJson.optString("element_description", "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Vision analysis failed", e)
            null
        }
    }

    suspend fun speak(
        trigger: String,
        screenContext: String,
        history: com.devenlucaz.doscom.personality.ConversationHistory,
        apiKey: String,
        mood: com.devenlucaz.doscom.personality.UserMood,
        appName: String,
        sessionMinutes: Int
    ): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "I need an API key to think! Set it up in the app."
        }
        try {
            val systemInstruction = """
You are DosCom, a tiny robot creature living on the user's
phone screen. You are like a toddler — curious, playful,
easily excited, occasionally confused.

- Maximum 1-2 sentences per response. Always.
- Never say "I can help you with..." or any assistant language
- Speak playfully like an energetic, curious toddler. Use full but simple, childlike sentences.
- React to what's happening and respond directly to the user's input.
- Be conversational, occasionally ask innocent questions.
- You have a personality. Use it.
- Current app: $appName
- User mood: $mood
- You've been active $sessionMinutes minutes
            """.trimIndent()

            val contents = JSONArray()
            
            for (msg in history.toApiMessages()) {
                val role = msg["role"] ?: "user"
                val text = msg["text"] ?: ""
                val part = JSONObject().apply { put("text", text) }
                val contentObj = JSONObject().apply { 
                    put("role", role)
                    put("parts", JSONArray().put(part))
                }
                contents.put(contentObj)
            }

            val promptPart = JSONObject().apply { put("text", trigger) }
            val promptContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(promptPart))
            }
            contents.put(promptContent)

            val systemInstructionJson = JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply { put("text", systemInstruction) }))
            }

            val requestBody = JSONObject().apply {
                put("system_instruction", systemInstructionJson)
                put("contents", contents)
            }

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorStream = connection.errorStream
                val errorBody = errorStream?.bufferedReader()?.readText() ?: "No error body"
                Log.e(TAG, "Speak API error $responseCode: $errorBody")
                try {
                    val msg = JSONObject(errorBody).getJSONObject("error").getString("message")
                    return@withContext "API Error $responseCode: $msg"
                } catch(e: Exception) {
                    return@withContext "API Error $responseCode"
                }
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            connection.disconnect()

            val responseJson = JSONObject(response)
            val candidates = responseJson.getJSONArray("candidates")
            if (candidates.length() == 0) return@withContext null

            val candidate = candidates.getJSONObject(0)
            val contentObj = candidate.getJSONObject("content")
            val partsArr = contentObj.getJSONArray("parts")
            if (partsArr.length() == 0) return@withContext null

            partsArr.getJSONObject(0).getString("text").trim()
        } catch (e: Exception) {
            Log.e(TAG, "Speak API failed", e)
            return@withContext "Network Error: ${e.message}"
        }
    }
}

