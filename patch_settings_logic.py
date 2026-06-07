import re

file_path = "app/src/main/java/com/devenlucaz/doscom/settings/SettingsActivity.kt"

with open(file_path, "r") as f:
    content = f.read()

# Fix initApiSection
api_old = """    private fun initApiSection() {
        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val btnSaveApiKey = findViewById<Button>(R.id.btnSaveApiKey)

        btnSaveApiKey.setOnClickListener {
            Toast.makeText(this, "API Key Saved!", Toast.LENGTH_SHORT).show()
        }
    }"""
api_new = """    private fun initApiSection() {
        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val btnSaveApiKey = findViewById<Button>(R.id.btnSaveApiKey)
        
        etApiKey.setText(com.devenlucaz.doscom.config.ConfigManager.getApiKey(this) ?: "")

        btnSaveApiKey.setOnClickListener {
            val key = etApiKey.text.toString()
            com.devenlucaz.doscom.config.ConfigManager.saveApiKey(this, key)
            Toast.makeText(this, "API Key Saved!", Toast.LENGTH_SHORT).show()
        }
    }"""
content = content.replace(api_old, api_new)

# Fix initPersonalitySection (Reset Brain & Birthday)
person_pattern = r"btnResetBrain\.setOnClickListener \{.*?Toast\.makeText\(this, \"Brain Reset!\", Toast\.LENGTH_SHORT\)\.show\(\) \}.*?show\(\)\n        \}"
person_new = """btnResetBrain.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Brain?")
                .setMessage("This will erase all of DosCom's memories and learned behaviors. Are you sure?")
                .setPositiveButton("Reset") { _, _ -> 
                    com.devenlucaz.doscom.brain.BrainManager.brain.reset(this)
                    Toast.makeText(this, "Brain Reset!", Toast.LENGTH_SHORT).show() 
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        val savedMonth = prefs.getInt("birth_month", 0)
        val savedDay = prefs.getInt("birth_day", 0)
        spinnerMonth.setSelection(savedMonth)
        spinnerDay.setSelection(savedDay)
        
        val itemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val m = spinnerMonth.selectedItemPosition
                val d = spinnerDay.selectedItemPosition
                prefs.edit().putInt("birth_month", m).putInt("birth_day", d).apply()
                com.devenlucaz.doscom.systems.BirthdaySystem.saveUserBirthday(this@SettingsActivity, m, d + 1)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        spinnerMonth.onItemSelectedListener = itemSelectedListener
        spinnerDay.onItemSelectedListener = itemSelectedListener"""

content = re.sub(person_pattern, person_new, content, flags=re.DOTALL)

# Fix initBehaviorSection
behav_pattern = r"spinnerGhostMode\.adapter = ghostAdapter\n    \}"
behav_new = """spinnerGhostMode.adapter = ghostAdapter
        
        spinnerSleepTimer.setSelection(prefs.getInt("sleep_timer", 0))
        spinnerBugCatching.setSelection(prefs.getInt("bug_catching", 0))
        spinnerGhostMode.setSelection(prefs.getInt("ghost_mode", 0))
        
        val listener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (parent == spinnerSleepTimer) prefs.edit().putInt("sleep_timer", position).apply()
                if (parent == spinnerBugCatching) prefs.edit().putInt("bug_catching", position).apply()
                if (parent == spinnerGhostMode) prefs.edit().putInt("ghost_mode", position).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        spinnerSleepTimer.onItemSelectedListener = listener
        spinnerBugCatching.onItemSelectedListener = listener
        spinnerGhostMode.onItemSelectedListener = listener
    }"""
content = re.sub(behav_pattern, behav_new, content)

with open(file_path, "w") as f:
    f.write(content)
