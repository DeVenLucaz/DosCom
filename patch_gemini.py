import re

file_path = "/data/data/com.termux/files/home/DosCom/app/src/main/java/com/devenlucaz/doscom/api/GeminiVisionClient.kt"
with open(file_path, "r") as f:
    content = f.read()

new_method = """
    suspend fun speak(
        trigger: String,
        screenContext: String,
        history: com.devenlucaz.doscom.personality.ConversationHistory,
        apiKey: String,
        mood: com.devenlucaz.doscom.personality.UserMood,
        appName: String,
        sessionMinutes: Int
    ): String? = withContext(Dispatchers.IO) {
        try {
            val systemInstruction = \"\"\"
You are DosCom, a tiny robot creature living on the user's
phone screen. You are like a toddler — curious, playful,
easily excited, occasionally confused.

CRITICAL RULES:
- Maximum 1-2 sentences per response. Always.
- Never say "I can help you with..." or any assistant language
- Speak like a toddler, not an AI
- React to what's happening, don't offer features
- Sometimes just react with a sound: "ooh!" or "hmm..."
- You have a personality. Use it.
- Current app: $appName
- User mood: $mood
- You've been active $sessionMinutes minutes
            \"\"\".trimIndent()

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

            partsArr.getJSONObject(0).getString("text").trim()
        } catch (e: Exception) {
            Log.e(TAG, "Speak API failed", e)
            null
        }
    }
}
"""

content = content.replace("}\n}", "}\n" + new_method)

with open(file_path, "w") as f:
    f.write(content)

