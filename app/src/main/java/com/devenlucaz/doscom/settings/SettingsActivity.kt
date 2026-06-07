package com.devenlucaz.doscom.settings

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devenlucaz.doscom.BuildConfig
import com.devenlucaz.doscom.R
import com.devenlucaz.doscom.mode.CompanionMode
import com.devenlucaz.doscom.mode.ModeManager
import com.devenlucaz.doscom.utils.ConfigManager
import com.devenlucaz.doscom.brain.BrainManager
import com.devenlucaz.doscom.systems.BirthdaySystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var cardAlive: LinearLayout
    private lateinit var cardAwake: LinearLayout
    private lateinit var cardAware: LinearLayout
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)

        initModeSection()
        initAppearanceSection()
        initBehaviorSection()
        initPersonalitySection()
        initApiSection()
        initAboutSection()
    }

    private fun initModeSection() {
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
            mouthOpen = true
        )
        
        renderAware.state = com.devenlucaz.doscom.character.AnimationState(
            leftArmAngle = -90f,
            rightArmAngle = -90f,
            eyesHalf = true
        )

        updateModeUI(ModeManager.getMode(this))

        val clickAlive = View.OnClickListener {
            ModeManager.setMode(this@SettingsActivity, CompanionMode.ALIVE)
            updateModeUI(CompanionMode.ALIVE)
        }
        val clickAwake = View.OnClickListener {
            ModeManager.setMode(this@SettingsActivity, CompanionMode.AWAKE)
            updateModeUI(CompanionMode.AWAKE)
        }
        val clickAware = View.OnClickListener {
            ModeManager.setMode(this@SettingsActivity, CompanionMode.AWARE)
            updateModeUI(CompanionMode.AWARE)
        }
        
        cardAlive.setOnClickListener(clickAlive)
        renderAlive.setOnClickListener(clickAlive)
        cardAwake.setOnClickListener(clickAwake)
        renderAwake.setOnClickListener(clickAwake)
        cardAware.setOnClickListener(clickAware)
        renderAware.setOnClickListener(clickAware)
    }

    private fun updateModeUI(mode: CompanionMode) {
        val unselectedBg = createBorderDrawable(Color.TRANSPARENT)
        cardAlive.background = unselectedBg
        cardAwake.background = unselectedBg
        cardAware.background = unselectedBg

        when (mode) {
            CompanionMode.ALIVE -> cardAlive.background = createBorderDrawable(Color.WHITE)
            CompanionMode.AWAKE -> cardAwake.background = createBorderDrawable(Color.parseColor("#00B4FF"))
            CompanionMode.AWARE -> cardAware.background = createBorderDrawable(Color.parseColor("#00FF88"))
        }
    }

    private fun createBorderDrawable(borderColor: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 16f
        drawable.setColor(Color.parseColor("#2A2A3E"))
        if (borderColor != Color.TRANSPARENT) {
            drawable.setStroke(4, borderColor)
        }
        return drawable
    }

    private fun initAppearanceSection() {
        val seekMascotSize = findViewById<SeekBar>(R.id.seekMascotSize)
        val seekAnimSpeed = findViewById<SeekBar>(R.id.seekAnimSpeed)

        seekMascotSize.progress = prefs.getInt("mascot_scale", 2)
        seekAnimSpeed.progress = prefs.getInt("anim_speed", 2)

        seekMascotSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().putInt("mascot_scale", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekAnimSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().putInt("anim_speed", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun initBehaviorSection() {
        val spinnerSleepTimer = findViewById<Spinner>(R.id.spinnerSleepTimer)
        val spinnerBugCatching = findViewById<Spinner>(R.id.spinnerBugCatching)
        val spinnerGhostMode = findViewById<Spinner>(R.id.spinnerGhostMode)

        val sleepAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("1min", "5min", "10min", "Never"))
        sleepAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSleepTimer.adapter = sleepAdapter

        val bugAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Off", "Rare", "Sometimes", "Always"))
        bugAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBugCatching.adapter = bugAdapter

        val ghostAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Interactive", "Semi-Ghost", "Full Ghost"))
        ghostAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGhostMode.adapter = ghostAdapter
        
        spinnerSleepTimer.setSelection(prefs.getInt("sleep_timer", 0))
        spinnerBugCatching.setSelection(prefs.getInt("bug_catching", 0))
        spinnerGhostMode.setSelection(prefs.getInt("ghost_mode", 0))
        
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (parent == spinnerSleepTimer) prefs.edit().putInt("sleep_timer", position).apply()
                if (parent == spinnerBugCatching) prefs.edit().putInt("bug_catching", position).apply()
                if (parent == spinnerGhostMode) prefs.edit().putInt("ghost_mode", position).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerSleepTimer.onItemSelectedListener = listener
        spinnerBugCatching.onItemSelectedListener = listener
        spinnerGhostMode.onItemSelectedListener = listener
    }

    private fun initPersonalitySection() {
        val spinnerMonth = findViewById<Spinner>(R.id.spinnerBirthMonth)
        val spinnerDay = findViewById<Spinner>(R.id.spinnerBirthDay)
        val btnResetBrain = findViewById<Button>(R.id.btnResetBrain)
        val tvInstallDate = findViewById<TextView>(R.id.tvInstallDate)

        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter

        val days = (1..31).map { it.toString() }.toTypedArray()
        val dayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDay.adapter = dayAdapter

        val installDateStr = prefs.getString("install_date", null)
        if (installDateStr == null) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            prefs.edit().putString("install_date", dateStr).apply()
            tvInstallDate.text = "DosCom has been with you since $dateStr"
        } else {
            tvInstallDate.text = "DosCom has been with you since $installDateStr"
        }

        btnResetBrain.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Brain?")
                .setMessage("This will erase all of DosCom's memories and learned behaviors. Are you sure?")
                .setPositiveButton("Reset") { _, _ -> 
                    BrainManager.brain.reset(this)
                    Toast.makeText(this, "Brain Reset!", Toast.LENGTH_SHORT).show() 
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        val savedMonth = prefs.getInt("birth_month", 0)
        val savedDay = prefs.getInt("birth_day", 0)
        spinnerMonth.setSelection(savedMonth)
        spinnerDay.setSelection(savedDay)
        
        val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val m = spinnerMonth.selectedItemPosition
                val d = spinnerDay.selectedItemPosition
                prefs.edit().putInt("birth_month", m).putInt("birth_day", d).apply()
                val bdayPrefs = getSharedPreferences("doscom_birthday_prefs", Context.MODE_PRIVATE)
                bdayPrefs.edit().putInt("birthday_month", m).putInt("birthday_day", d + 1).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerMonth.onItemSelectedListener = itemSelectedListener
        spinnerDay.onItemSelectedListener = itemSelectedListener
    }

    private fun initApiSection() {
        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val btnSaveApiKey = findViewById<Button>(R.id.btnSaveApiKey)
        
        etApiKey.setText(ConfigManager.loadApiKey(this) ?: "")

        btnSaveApiKey.setOnClickListener {
            val key = etApiKey.text.toString()
            ConfigManager.saveApiKey(this, key)
            
            Toast.makeText(this@SettingsActivity, "Validating API Key...", Toast.LENGTH_SHORT).show()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    
                    val body = """{"contents":[{"parts":[{"text":"hello"}]}]}"""
                    conn.outputStream.write(body.toByteArray())
                    
                    val responseCode = conn.responseCode
                    withContext(Dispatchers.Main) {
                        if (responseCode == 200) {
                            Toast.makeText(this@SettingsActivity, "Key works!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@SettingsActivity, "Invalid key", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsActivity, "Invalid key", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun initAboutSection() {
        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        val btnResetAll = findViewById<Button>(R.id.btnResetAll)

        tvVersion.text = "Version ${BuildConfig.VERSION_NAME}"

        btnResetAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset All Settings?")
                .setMessage("Are you sure you want to revert to default settings?")
                .setPositiveButton("Reset") { _, _ -> Toast.makeText(this, "Settings Reset!", Toast.LENGTH_SHORT).show() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
