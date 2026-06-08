import re

file_path = "/data/data/com.termux/files/home/DosCom/app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt"
with open(file_path, "r") as f:
    content = f.read()

# Add properties
props = """
    private lateinit var prefs: SharedPreferences
    private val conversationHistory = com.devenlucaz.doscom.personality.ConversationHistory()
    private val serviceStartTime = System.currentTimeMillis()
    private fun getSessionMinutes(): Int = ((System.currentTimeMillis() - serviceStartTime) / 60000).toInt()
"""

if "private val conversationHistory" not in content:
    content = content.replace("    private lateinit var prefs: SharedPreferences", props)

# Replace handleQuery
old_handle = """    private fun handleQuery(query: String, screenshot: Bitmap?) {
        layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        windowManager.updateViewLayout(overlayView, layoutParams)

        showSpeechBubble("Let me look...", layoutParams.x, layoutParams.y)"""

new_handle = """    private fun handleQuery(query: String, screenshot: Bitmap?) {
        val currentMode = com.devenlucaz.doscom.mode.ModeManager.getMode(this)
        
        layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        windowManager.updateViewLayout(overlayView, layoutParams)

        com.devenlucaz.doscom.personality.MoodEngine.detectFromChat(query)?.let { mood ->
            com.devenlucaz.doscom.personality.MoodEngine.currentMood = mood
            com.devenlucaz.doscom.personality.MoodEngine.applyMoodToAnimation(idleEngine)
        }

        if (currentMode == com.devenlucaz.doscom.mode.CompanionMode.ALIVE) {
            showSpeechBubble("...", layoutParams.x, layoutParams.y)
            return
        }

        if (currentMode == com.devenlucaz.doscom.mode.CompanionMode.AWAKE) {
            com.devenlucaz.doscom.personality.EmotionalMemory.recordPositive(this, 0.05f)
            showSpeechBubble("Hmm...", layoutParams.x, layoutParams.y)
            idleEngine.targetState.mouthExpression = 0
            
            serviceScope.launch {
                val response = com.devenlucaz.doscom.api.GeminiVisionClient.speak(
                    trigger = query,
                    screenContext = "",
                    history = conversationHistory,
                    apiKey = com.devenlucaz.doscom.utils.ConfigManager.loadApiKey(this@CompanionOverlayService) ?: "",
                    mood = com.devenlucaz.doscom.personality.MoodEngine.currentMood,
                    appName = "Unknown",
                    sessionMinutes = getSessionMinutes()
                )
                
                withContext(Dispatchers.Main) {
                    if (response != null) {
                        showSpeechBubble(response, layoutParams.x, layoutParams.y)
                        conversationHistory.addMessage("user", query)
                        conversationHistory.addMessage("model", response)
                    } else {
                        showSpeechBubble("...?", layoutParams.x, layoutParams.y)
                    }
                }
            }
            return
        }

        showSpeechBubble("Let me look...", layoutParams.x, layoutParams.y)"""

if "currentMode == com.devenlucaz.doscom.mode.CompanionMode.AWAKE" not in content:
    content = content.replace(old_handle, new_handle)

with open(file_path, "w") as f:
    f.write(content)

