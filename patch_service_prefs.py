import re

file_path = "app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt"

with open(file_path, "r") as f:
    content = f.read()

# Add SharedPreferences listener
imports = """import android.content.SharedPreferences
import com.devenlucaz.doscom.mode.ModeManager"""

if "import android.content.SharedPreferences" not in content:
    content = content.replace("import android.content.IntentFilter", "import android.content.IntentFilter\n" + imports)

listener_code = """
    private lateinit var prefs: SharedPreferences
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "mascot_scale" -> {
                val scale = sharedPreferences.getInt("mascot_scale", 2)
                val scaleFloat = 0.5f + (scale * 0.375f) // map 0-4 to 0.5-2.0
                idleEngine.targetState.scale = scaleFloat
                idleEngine.targetState.scaleX = scaleFloat
            }
            "anim_speed" -> {
                val speed = sharedPreferences.getInt("anim_speed", 2)
                val speedFloat = 0.5f + (speed * 0.25f) // map 0-4 to 0.5-1.5
                idleEngine.animSpeedMultiplier = speedFloat
            }
            "sleep_timer" -> {
                val sleepPos = sharedPreferences.getInt("sleep_timer", 0)
                idleEngine.sleepTimerMs = when(sleepPos) {
                    0 -> 60 * 1000L
                    1 -> 5 * 60 * 1000L
                    2 -> 10 * 60 * 1000L
                    else -> Long.MAX_VALUE
                }
            }
        }
    }
"""

if "prefsListener" not in content:
    content = content.replace("private val handler = Handler(Looper.getMainLooper())", "private val handler = Handler(Looper.getMainLooper())\n" + listener_code)

init_prefs_code = """
        prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        // trigger initial load
        prefsListener.onSharedPreferenceChanged(prefs, "mascot_scale")
        prefsListener.onSharedPreferenceChanged(prefs, "anim_speed")
        prefsListener.onSharedPreferenceChanged(prefs, "sleep_timer")
"""

if "prefs.registerOnSharedPreferenceChangeListener" not in content:
    content = content.replace("startIdleBehaviors()", "startIdleBehaviors()\n" + init_prefs_code)

if "prefs.unregisterOnSharedPreferenceChangeListener" not in content:
    content = content.replace("stopIdleBehaviors()", "stopIdleBehaviors()\n        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)")

with open(file_path, "w") as f:
    f.write(content)
