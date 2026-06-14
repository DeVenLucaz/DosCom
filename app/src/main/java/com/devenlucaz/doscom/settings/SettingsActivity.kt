package com.devenlucaz.doscom.settings

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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.devenlucaz.doscom.BuildConfig
import com.devenlucaz.doscom.R
import com.devenlucaz.doscom.mode.CompanionMode
import com.devenlucaz.doscom.mode.ModeManager
import com.devenlucaz.doscom.utils.ConfigManager
import com.devenlucaz.doscom.brain.BrainManager
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

        // Ensure BrainManager is initialized in case we started from launcher
        BrainManager.init(this)

        // Mode Section
        cardAlive = findViewById(R.id.cardAlive)
        cardAwake = findViewById(R.id.cardAwake)
        cardAware = findViewById(R.id.cardAware)

        val renderAlive = findViewById<View>(R.id.render_cardAlive)
        val renderAwake = findViewById<View>(R.id.render_cardAwake)
        val renderAware = findViewById<View>(R.id.render_cardAware)

        updateModeUI(ModeManager.getMode(this))

        val clickAlive = View.OnClickListener {
            ModeManager.setMode(this@SettingsActivity, CompanionMode.ALIVE)
            updateModeUI(CompanionMode.ALIVE)
            Toast.makeText(this, "Mode: ALIVE", Toast.LENGTH_SHORT).show()
        }
        val clickAwake = View.OnClickListener {
            ModeManager.setMode(this@SettingsActivity, CompanionMode.AWAKE)
            updateModeUI(CompanionMode.AWAKE)
            Toast.makeText(this, "Mode: AWAKE", Toast.LENGTH_SHORT).show()
        }
        val clickAware = View.OnClickListener {
            ModeManager.setMode(this@SettingsActivity, CompanionMode.AWARE)
            updateModeUI(CompanionMode.AWARE)
            Toast.makeText(this, "Mode: AWARE", Toast.LENGTH_SHORT).show()
        }

        cardAlive.setOnClickListener(clickAlive)
        renderAlive?.setOnClickListener(clickAlive)
        cardAwake.setOnClickListener(clickAwake)
        renderAwake?.setOnClickListener(clickAwake)
        cardAware.setOnClickListener(clickAware)
        renderAware?.setOnClickListener(clickAware)

        // Appearance Section
        val seekMascotSize = findViewById<SeekBar>(R.id.seekMascotSize)
        val seekAnimSpeed = findViewById<SeekBar>(R.id.seekAnimSpeed)

        seekMascotSize.progress = prefs.getInt("mascot_scale", 7)
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

        // Behavior Section
        val spinnerSleepTimer = findViewById<Spinner>(R.id.spinnerSleepTimer)
        val spinnerGhostMode = findViewById<Spinner>(R.id.spinnerGhostMode)

        val sleepAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("1min", "5min", "10min", "Never"))
        sleepAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSleepTimer.adapter = sleepAdapter



        val ghostAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Interactive", "Semi-Ghost", "Full Ghost"))
        ghostAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGhostMode.adapter = ghostAdapter

        spinnerSleepTimer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("sleep_timer", position).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }



        spinnerGhostMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("ghost_mode", position).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerSleepTimer.setSelection(prefs.getInt("sleep_timer", 0))
        spinnerGhostMode.setSelection(prefs.getInt("ghost_mode", 0))

        // Personality Section
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

        val savedMonth = prefs.getInt("birth_month", 0)
        val savedDay = prefs.getInt("birth_day", 0)
        spinnerMonth.setSelection(savedMonth)
        spinnerDay.setSelection(savedDay)

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
                    Toast.makeText(this, "Brain Reset Successfully!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // API Section
        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val btnSaveApiKey = findViewById<Button>(R.id.btnSaveApiKey)

        etApiKey.setText(ConfigManager.loadApiKey(this) ?: "")

        btnSaveApiKey.setOnClickListener {
            val key = etApiKey.text.toString()
            ConfigManager.saveApiKey(this, key)
            Toast.makeText(this, "Validating API Key...", Toast.LENGTH_SHORT).show()

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

        // About Section
        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        val btnResetAll = findViewById<Button>(R.id.btnResetAll)

        tvVersion.text = "Version 2.0.0"

        btnResetAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset All Settings?")
                .setMessage("Are you sure you want to revert to default settings?")
                .setPositiveButton("Reset") { _, _ -> Toast.makeText(this, "Settings Reset!", Toast.LENGTH_SHORT).show() }
                .setNegativeButton("Cancel", null)
                .show()
        }
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
}
