package com.devenlucaz.doscom.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.devenlucaz.doscom.api.GeminiVisionClient
import com.devenlucaz.doscom.character.CompanionRenderer
import com.devenlucaz.doscom.mode.CompanionMode
import com.devenlucaz.doscom.mode.ModeManager
import com.devenlucaz.doscom.service.DosComAccessibilityService
import com.devenlucaz.doscom.service.ServiceManager
import com.devenlucaz.doscom.utils.BatteryOptimizationHelper
import com.devenlucaz.doscom.utils.ConfigManager
import java.time.LocalDate

class OnboardingActivity : AppCompatActivity() {

    private var currentStep = 1
    private val AUDIO_PERMISSION_REQUEST = 1002
    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showStep(currentStep)
    }

    override fun onDestroy() {
        super.onDestroy()
        pollRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onResume() {
        super.onResume()
        if (currentStep == 3 && Settings.canDrawOverlays(this)) {
            nextStep()
        } else if (currentStep == 5 && DosComAccessibilityService.isConnected()) {
            nextStep()
        }
    }

    private fun showStep(step: Int) {
        pollRunnable?.let { handler.removeCallbacks(it) }
        val density = resources.displayMetrics.density
        val dp = { value: Int -> (value * density).toInt() }

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0A0A0F"))
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(64), dp(32), dp(32))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        scrollView.addView(layout)

        when (step) {
            1 -> buildStep1(layout, dp)
            2 -> buildStep2(layout, dp)
            3 -> buildStep3(layout, dp)
            4 -> {
                val mode = ModeManager.getMode(this)
                if (mode == CompanionMode.AWAKE || mode == CompanionMode.AWARE) {
                    buildStep4(layout, dp)
                } else {
                    nextStep()
                    return
                }
            }
            5 -> {
                if (ModeManager.getMode(this) == CompanionMode.AWARE) {
                    buildStep5(layout, dp)
                } else {
                    nextStep()
                    return
                }
            }
            6 -> {
                if (ModeManager.getMode(this) == CompanionMode.AWARE) {
                    buildStep6(layout, dp)
                } else {
                    nextStep()
                    return
                }
            }
            7 -> buildStep7(layout, dp)
            8 -> buildStep8(layout, dp)
        }

        setContentView(scrollView)
    }

    private fun buildStep1(layout: LinearLayout, dp: (Int) -> Int) {
        val renderer = CompanionRenderer(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(100), dp(100)).apply {
                bottomMargin = dp(32)
            }
        }
        layout.addView(renderer)

        val title = TextView(this).apply {
            text = "hi."
            textSize = 32f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        layout.addView(title)

        val subtitle = TextView(this).apply {
            text = "I live on your phone."
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(48))
        }
        layout.addView(subtitle)

        val nextBtn = Button(this).apply {
            text = "Next \u2192"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6200EE"))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener { nextStep() }
        }
        layout.addView(nextBtn)
    }

    private fun buildStep2(layout: LinearLayout, dp: (Int) -> Int) {
        val title = TextView(this).apply {
            text = "Choose Mode"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        val sub1 = TextView(this).apply {
            text = "You can change this anytime."
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, dp(4))
        }
        val sub2 = TextView(this).apply {
            text = "Double tap me to switch."
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, dp(24))
        }
        layout.addView(title)
        layout.addView(sub1)
        layout.addView(sub2)

        val modes = listOf(
            Triple(CompanionMode.ALIVE, "🐾 ALIVE", "A living digital pet."),
            Triple(CompanionMode.AWAKE, "💬 AWAKE", "A pet that talks via Gemini."),
            Triple(CompanionMode.AWARE, "🧠 AWARE", "A pet that sees your screen.")
        )

        for ((mode, name, desc) in modes) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1A1A2E"))
                    cornerRadius = dp(16).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(16) }
                
                setOnClickListener {
                    ModeManager.setMode(this@OnboardingActivity, mode)
                    nextStep()
                }
            }
            
            val mTitle = TextView(this).apply {
                text = name
                setTextColor(Color.WHITE)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
            }
            val mDesc = TextView(this).apply {
                text = desc
                setTextColor(Color.LTGRAY)
            }
            card.addView(mTitle)
            card.addView(mDesc)
            layout.addView(card)
        }
    }

    private fun buildStep3(layout: LinearLayout, dp: (Int) -> Int) {
        val title = TextView(this).apply {
            text = "Overlay Permission"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        }
        val sub = TextView(this).apply {
            text = "I need to float on your screen"
            setTextColor(Color.LTGRAY)
            textSize = 18f
            setPadding(0, 0, 0, dp(32))
        }
        layout.addView(title)
        layout.addView(sub)

        val btn = Button(this).apply {
            text = "Let me in"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6200EE"))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:\$packageName")))
            }
        }
        layout.addView(btn)

        pollRunnable = object : Runnable {
            override fun run() {
                if (Settings.canDrawOverlays(this@OnboardingActivity)) {
                    nextStep()
                } else {
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.postDelayed(pollRunnable!!, 500)
    }

    private fun buildStep4(layout: LinearLayout, dp: (Int) -> Int) {
        val title = TextView(this).apply {
            text = "API Key"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        }
        val sub1 = TextView(this).apply {
            text = "I think better with a brain."
            setTextColor(Color.LTGRAY)
            textSize = 16f
            setPadding(0, 0, 0, dp(8))
        }
        val sub2 = TextView(this).apply {
            text = "Get a free key at ai.google.dev"
            setTextColor(Color.parseColor("#00B4FF"))
            textSize = 14f
            setPadding(0, 0, 0, dp(24))
        }
        layout.addView(title)
        layout.addView(sub1)
        layout.addView(sub2)

        val input = EditText(this).apply {
            hint = "Gemini API Key"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A2E"))
                cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(24) }
        }
        layout.addView(input)

        val btn = Button(this).apply {
            text = "Save"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6200EE"))
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            setOnClickListener {
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    ConfigManager.saveApiKey(this@OnboardingActivity, key)
                    GeminiVisionClient.configure(key)
                }
                nextStep()
            }
        }
        layout.addView(btn)

        val skip = Button(this).apply {
            text = "Skip"
            setTextColor(Color.LTGRAY)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { nextStep() }
        }
        layout.addView(skip)
    }

    private fun buildStep5(layout: LinearLayout, dp: (Int) -> Int) {
        val title = TextView(this).apply {
            text = "Accessibility"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        }
        layout.addView(title)

        val lines = listOf(
            "I can notice what's on your screen.",
            "I never record or send your screen anywhere.",
            "I just use it to point at things."
        )
        for (line in lines) {
            layout.addView(TextView(this).apply {
                text = line
                setTextColor(Color.LTGRAY)
                setPadding(0, 0, 0, dp(8))
            })
        }

        if (Build.MANUFACTURER.equals("OPPO", true) || Build.MANUFACTURER.equals("OnePlus", true)) {
            layout.addView(TextView(this).apply {
                text = "ColorOS / OxygenOS Fix:\nGo to Special Access \u2192 Accessibility"
                setTextColor(Color.parseColor("#FFCA28"))
                setPadding(0, dp(16), 0, 0)
            })
        }

        val btn = Button(this).apply {
            text = "Open Settings"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6200EE"))
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(24) }
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
        layout.addView(btn)

        pollRunnable = object : Runnable {
            override fun run() {
                if (DosComAccessibilityService.isConnected()) {
                    nextStep()
                } else {
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.postDelayed(pollRunnable!!, 500)
    }

    private fun buildStep6(layout: LinearLayout, dp: (Int) -> Int) {
        val title = TextView(this).apply {
            text = "Microphone"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        }
        val sub = TextView(this).apply {
            text = "Want to talk to me?"
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, dp(32))
        }
        layout.addView(title)
        layout.addView(sub)

        val btn = Button(this).apply {
            text = "Allow Audio"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6200EE"))
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            setOnClickListener {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_PERMISSION_REQUEST)
            }
        }
        layout.addView(btn)

        val skip = Button(this).apply {
            text = "Skip"
            setTextColor(Color.LTGRAY)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { nextStep() }
        }
        layout.addView(skip)
    }

    private fun buildStep7(layout: LinearLayout, dp: (Int) -> Int) {
        val title = TextView(this).apply {
            text = "Battery"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        }
        val sub = TextView(this).apply {
            text = "Stay with me."
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, dp(16))
        }
        layout.addView(title)
        layout.addView(sub)

        if (Build.MANUFACTURER.equals("OPPO", true) || Build.MANUFACTURER.equals("OnePlus", true)) {
            val colorOs = TextView(this).apply {
                text = "ColorOS / OxygenOS users: Please also allow Auto-Launch in your battery settings."
                setTextColor(Color.parseColor("#FFCA28"))
                setPadding(0, 0, 0, dp(16))
            }
            layout.addView(colorOs)
        }

        val btn = Button(this).apply {
            text = "Disable Battery Optimization"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6200EE"))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                BatteryOptimizationHelper.requestBatteryExemption(this@OnboardingActivity)
                handler.postDelayed({ nextStep() }, 1500)
            }
        }
        layout.addView(btn)
        
        val nextBtn = Button(this).apply {
            text = "Next \u2192"
            setTextColor(Color.LTGRAY)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { nextStep() }
        }
        layout.addView(nextBtn)
    }

    private fun buildStep8(layout: LinearLayout, dp: (Int) -> Int) {
        val renderer = CompanionRenderer(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(100), dp(100)).apply {
                bottomMargin = dp(32)
            }
        }
        layout.addView(renderer)

        val mode = ModeManager.getMode(this)
        val msg = when (mode) {
            CompanionMode.ALIVE -> "I'm home. \uD83D\uDC3E"
            CompanionMode.AWAKE -> "ready to chat \uD83D\uDCAC"
            CompanionMode.AWARE -> "I've got your back \uD83E\uDDE0" 
        }

        val bubble = TextView(this).apply {
            text = msg
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(48))
        }
        layout.addView(bubble)

        val btn = Button(this).apply {
            text = "Finish"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#4CAF50"))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                val prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("onboarding_complete", true).apply()
                if (!prefs.contains("install_date")) {
                    prefs.edit().putString("install_date", LocalDate.now().toString()).apply()
                }
                ServiceManager.startOverlayService(this@OnboardingActivity)
                finish()
            }
        }
        layout.addView(btn)
    }

    private fun nextStep() {
        currentStep++
        if (currentStep > 8) {
            finish()
        } else {
            showStep(currentStep)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST) {
            nextStep()
        }
    }
}
