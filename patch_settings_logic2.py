import re

file_path = "app/src/main/java/com/devenlucaz/doscom/settings/SettingsActivity.kt"

with open(file_path, "r") as f:
    content = f.read()

# Fix Mode Section clicks
mode_old = """        cardAlive.setOnClickListener {
            ModeManager.setMode(this, CompanionMode.ALIVE)
            updateModeUI(CompanionMode.ALIVE)
        }
        cardAwake.setOnClickListener {
            ModeManager.setMode(this, CompanionMode.AWAKE)
            updateModeUI(CompanionMode.AWAKE)
        }
        cardAware.setOnClickListener {
            ModeManager.setMode(this, CompanionMode.AWARE)
            updateModeUI(CompanionMode.AWARE)
        }"""
mode_new = """        val clickAlive = android.view.View.OnClickListener {
            ModeManager.setMode(this, CompanionMode.ALIVE)
            updateModeUI(CompanionMode.ALIVE)
        }
        val clickAwake = android.view.View.OnClickListener {
            ModeManager.setMode(this, CompanionMode.AWAKE)
            updateModeUI(CompanionMode.AWAKE)
        }
        val clickAware = android.view.View.OnClickListener {
            ModeManager.setMode(this, CompanionMode.AWARE)
            updateModeUI(CompanionMode.AWARE)
        }
        cardAlive.setOnClickListener(clickAlive)
        renderAlive.setOnClickListener(clickAlive)
        cardAwake.setOnClickListener(clickAwake)
        renderAwake.setOnClickListener(clickAwake)
        cardAware.setOnClickListener(clickAware)
        renderAware.setOnClickListener(clickAware)"""

content = content.replace(mode_old, mode_new)

with open(file_path, "w") as f:
    f.write(content)
