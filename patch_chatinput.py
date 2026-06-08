import re

file_path = "app/src/main/java/com/devenlucaz/doscom/ui/ChatInputOverlay.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace handleReaction
old_handle = """    private fun handleReaction(reaction: String) {
        dismiss(submitted = false, reactionCallback = {
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
        })
    }"""

new_handle = """    private fun handleReaction(reaction: String) {
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

content = content.replace(old_handle, new_handle)

# Add clickable flags to TextView
old_tv = """            val btn = TextView(context).apply {
                text = r
                textSize = 24f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setPadding(0, dp(8), 0, dp(8))
                setOnClickListener {
                    handleReaction(r)
                }
            }"""

new_tv = """            val btn = android.widget.Button(context).apply {
                text = r
                textSize = 24f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setPadding(0, dp(8), 0, dp(8))
                setOnClickListener {
                    handleReaction(r)
                }
            }"""

content = content.replace(old_tv, new_tv)

with open(file_path, "w") as f:
    f.write(content)
