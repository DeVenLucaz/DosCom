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

# Replace handleQuery logic
pattern = re.compile(r'    private fun handleQuery\(query: String, screenshot: Bitmap\?\) \{.*?showSpeechBubble\("Let me look\.\.\.", layoutParams\.x, layoutParams\.y\)', re.DOTALL)

replacement = """    private fun handleQuery(query: String, screenshot: Bitmap?) {
        val currentMode = com.devenlucaz.doscom.mode.ModeManager.getMode(this)
        
        idleEngine.interact()
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

        val qLower = query.lowercase()
        val isLocateRequest = qLower.contains("find") || qLower.contains("where is") || qLower.contains("show me") || qLower.contains("locate")

        if (!isLocateRequest && (currentMode == com.devenlucaz.doscom.mode.CompanionMode.AWAKE || currentMode == com.devenlucaz.doscom.mode.CompanionMode.AWARE)) {
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

content = pattern.sub(replacement, content)

with open(file_path, "w") as f:
    f.write(content)

