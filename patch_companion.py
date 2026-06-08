import re

file_path = "app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt"

with open(file_path, "r") as f:
    content = f.read()

# Update mascot_scale
old_mascot = """            "mascot_scale" -> {
                val scale = sharedPreferences.getInt("mascot_scale", 2)
                val scaleFloat = 0.5f + (scale * 0.375f) // map 0-4 to 0.5-2.0
                idleEngine.targetState.scale = scaleFloat
                idleEngine.targetState.scaleX = scaleFloat
            }"""

new_mascot = """            "mascot_scale" -> {
                val scale = sharedPreferences.getInt("mascot_scale", 7)
                val scaleFloat = 0.5f + (scale * (1.5f / 14f)) // map 0-14 to 0.5-2.0
                idleEngine.targetState.scale = scaleFloat
                idleEngine.targetState.scaleX = scaleFloat
            }
            "ghost_mode" -> {
                val mode = sharedPreferences.getInt("ghost_mode", 0)
                updateGhostMode(mode)
            }"""

content = content.replace(old_mascot, new_mascot)

# Add updateGhostMode function before startIdleBehaviors
update_func = """    private fun updateGhostMode(mode: Int) {
        if (!::windowManager.isInitialized || !::overlayView.isInitialized || !::layoutParams.isInitialized) return
        when (mode) {
            0 -> { // Interactive
                layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                overlayView.alpha = 1.0f
            }
            1 -> { // Semi-Ghost
                layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                overlayView.alpha = 0.6f
            }
            2 -> { // Full Ghost
                layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                overlayView.alpha = 0.3f
            }
        }
        try {
            windowManager.updateViewLayout(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startIdleBehaviors() {"""

content = content.replace("    private fun startIdleBehaviors() {", update_func)

# Trigger initial ghost mode
trigger_old = """        prefsListener.onSharedPreferenceChanged(prefs, "sleep_timer")"""
trigger_new = """        prefsListener.onSharedPreferenceChanged(prefs, "sleep_timer")
        prefsListener.onSharedPreferenceChanged(prefs, "ghost_mode")"""

content = content.replace(trigger_old, trigger_new)

with open(file_path, "w") as f:
    f.write(content)
