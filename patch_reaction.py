import re

overlay_path = "app/src/main/java/com/devenlucaz/doscom/ui/ChatInputOverlay.kt"
with open(overlay_path, "r") as f:
    content = f.read()

# Replace handleReaction
old_handle = """    private fun handleReaction(reaction: String) {
        val inputs = BrainInput.buildInputs(context)
        val targetOutputs = IntArray(7) 

        if (reaction == "😤") {
            EmotionalMemory.recordNegative(context)
            BrainManager.brain.learn(inputs, targetOutputs, reward = -0.5f)
            onReactedNegative()
        } else {
            EmotionalMemory.recordPositive(context)
            BrainManager.brain.learn(inputs, targetOutputs, reward = 1.0f)
            onReactedPositive()
        }
        BrainManager.brain.save(context)
        
        dismiss(submitted = false)
    }"""

new_handle = """    private fun handleReaction(reaction: String) {
        val inputs = BrainInput.buildInputs(context)
        val targetOutputs = IntArray(7) 

        val intent = android.content.Intent("com.devenlucaz.doscom.REACTION")
        if (reaction == "😤") {
            EmotionalMemory.recordNegative(context)
            BrainManager.brain.learn(inputs, targetOutputs, reward = -0.5f)
            intent.putExtra("reactionType", "negative")
        } else {
            EmotionalMemory.recordPositive(context)
            BrainManager.brain.learn(inputs, targetOutputs, reward = 1.0f)
            intent.putExtra("reactionType", "positive")
        }
        BrainManager.brain.save(context)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        
        dismiss(submitted = false)
    }"""

content = content.replace(old_handle, new_handle)

# Remove callbacks from constructor
content = content.replace(",\n    private val onReactedPositive: () -> Unit = {}", "")
content = content.replace(",\n    private val onReactedNegative: () -> Unit = {}", "")
content = content.replace("    private val onReactedPositive: () -> Unit = {},\n", "")
content = content.replace("    private val onReactedNegative: () -> Unit = {}\n", "")

with open(overlay_path, "w") as f:
    f.write(content)

service_path = "app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt"
with open(service_path, "r") as f:
    content = f.read()

# Add reactionReceiver definition
receiver_code = """
    private val appCategoryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val categoryName = intent?.getStringExtra("category") ?: return
            handleAppCategoryReaction(categoryName)
        }
    }

    private val reactionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val reactionType = intent?.getStringExtra("reactionType") ?: return
            if (reactionType == "positive") {
                idleEngine.targetState.blushVisible = true
                idleEngine.targetState.bodyOffsetY = -20f
                handler.postDelayed({
                    idleEngine.targetState.blushVisible = false
                    idleEngine.targetState.bodyOffsetY = 0f
                }, 1000)
            } else if (reactionType == "negative") {
                idleEngine.targetState.antennaGlow = 0.2f
                idleEngine.targetState.bodyOffsetY = 10f
                idleEngine.targetState.mouthExpression = 2 
                handler.postDelayed({
                    idleEngine.targetState.antennaGlow = 1.0f
                    idleEngine.targetState.bodyOffsetY = 0f
                    idleEngine.targetState.mouthExpression = 0
                }, 1000)
            }
        }
    }
"""

content = content.replace("""
    private val appCategoryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val categoryName = intent?.getStringExtra("category") ?: return
            handleAppCategoryReaction(categoryName)
        }
    }
""", receiver_code)

# Register receiver
reg_code = """
            LocalBroadcastManager.getInstance(this).registerReceiver(
                appCategoryReceiver,
                IntentFilter("APP_CONTEXT_CHANGED")
            )
            LocalBroadcastManager.getInstance(this).registerReceiver(
                reactionReceiver,
                IntentFilter("com.devenlucaz.doscom.REACTION")
            )"""

content = content.replace("""
            LocalBroadcastManager.getInstance(this).registerReceiver(
                appCategoryReceiver,
                IntentFilter("APP_CONTEXT_CHANGED")
            )""", reg_code)

# Unregister receiver
unreg_code = """
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(appCategoryReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(reactionReceiver)"""

content = content.replace("""
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(appCategoryReceiver)""", unreg_code)

# Remove onReacted callbacks from ChatInputOverlay instantiation
old_instantiation = """
            onReactedPositive = {
                idleEngine.targetState.blushVisible = true
                idleEngine.targetState.bodyOffsetY = -20f
                handler.postDelayed({
                    idleEngine.targetState.blushVisible = false
                    idleEngine.targetState.bodyOffsetY = 0f
                }, 2000)
            },
            onReactedNegative = {
                idleEngine.targetState.antennaGlow = 0.2f
                idleEngine.targetState.mouthExpression = 2 
                handler.postDelayed({
                    idleEngine.targetState.antennaGlow = 1.0f
                    idleEngine.targetState.mouthExpression = 0
                }, 3000)
            }
        )"""

content = content.replace(old_instantiation, "\n        )")

with open(service_path, "w") as f:
    f.write(content)

