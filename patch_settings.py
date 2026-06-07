import re

kt_file = "app/src/main/java/com/devenlucaz/doscom/settings/SettingsActivity.kt"
with open(kt_file, "r") as f:
    content = f.read()

# Replace initModeSection
new_init = """    private fun initModeSection() {
        cardAlive = findViewById(R.id.cardAlive)
        cardAwake = findViewById(R.id.cardAwake)
        cardAware = findViewById(R.id.cardAware)

        val renderAlive = findViewById<com.devenlucaz.doscom.character.CompanionRenderer>(R.id.render_cardAlive)
        val renderAwake = findViewById<com.devenlucaz.doscom.character.CompanionRenderer>(R.id.render_cardAwake)
        val renderAware = findViewById<com.devenlucaz.doscom.character.CompanionRenderer>(R.id.render_cardAware)

        renderAlive.state = com.devenlucaz.doscom.character.AnimationState()
        
        renderAwake.state = com.devenlucaz.doscom.character.AnimationState(
            eyesWide = true,
            leftArmAngle = -45f,
            rightArmAngle = -45f,
            mouthOpen = 0.5f
        )
        
        renderAware.state = com.devenlucaz.doscom.character.AnimationState(
            leftArmAngle = -90f,
            rightArmAngle = -90f,
            eyesHalf = true
        )

        val currentMode = ModeManager.getMode(this)
        updateModeUI(currentMode)

        cardAlive.setOnClickListener {
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
        }
    }"""

pattern = r"    private fun initModeSection\(\) \{.*?(?=    private fun updateModeUI)"
content = re.sub(pattern, new_init + "\n\n", content, flags=re.DOTALL)

with open(kt_file, "w") as f:
    f.write(content)
