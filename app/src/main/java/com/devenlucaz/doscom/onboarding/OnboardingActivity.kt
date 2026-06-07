package com.devenlucaz.doscom.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.devenlucaz.doscom.api.GeminiVisionClient
import com.devenlucaz.doscom.service.DosComAccessibilityService
import com.devenlucaz.doscom.service.ServiceManager
import com.devenlucaz.doscom.utils.BatteryOptimizationHelper
import com.devenlucaz.doscom.utils.ConfigManager
import com.devenlucaz.doscom.utils.ScreenshotHelper

class OnboardingActivity : AppCompatActivity() {

    private var currentStep = 0
    private val totalSteps = 8
    private val SCREEN_CAPTURE_REQUEST = 1001
    private val AUDIO_PERMISSION_REQUEST = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showStep(currentStep)
    }

    override fun onResume() {
        super.onResume()
        // Re-render current step to update button states
        showStep(currentStep)
    }

    private fun showStep(step: Int) {
        val density = resources.displayMetrics.density
        val dp = { value: Int -> (value * density).toInt() }

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(48), dp(32), dp(32))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        scrollView.addView(layout)

        // Step indicator
        val stepIndicator = TextView(this).apply {
            text = "Step ${step + 1} of $totalSteps"
            textSize = 14f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
        layout.addView(stepIndicator)

        // Emoji icon
        val iconView = TextView(this).apply {
            textSize = 64f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(24))
        }
        layout.addView(iconView)

        // Title
        val titleView = TextView(this).apply {
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#212121"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
        layout.addView(titleView)

        // Description
        val descView = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#616161"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(32))
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        layout.addView(descView)

        // Action button
        val actionButton = Button(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6200EE"))
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(32), dp(16), dp(32), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
        }

        when (step) {
            0 -> {
                iconView.text = "\uD83E\uDD16"
                titleView.text = "Meet DosCom"
                descView.text = "Your AI companion that lives on your screen, sees what you see, and shows you the way.\n\nLet's set up a few permissions so DosCom can help you."
                actionButton.text = "Let's Go \u2192"
                actionButton.setOnClickListener { nextStep() }
            }
            1 -> {
                iconView.text = "\uD83D\uDDA5\uFE0F"
                titleView.text = "Overlay Permission"
                descView.text = "DosCom needs to float on your screen above other apps. This is what makes it a companion!"
                val granted = Settings.canDrawOverlays(this)
                if (granted) {
                    actionButton.text = "\u2713 Granted \u2014 Next \u2192"
                    (actionButton.background as GradientDrawable).setColor(Color.parseColor("#4CAF50"))
                    actionButton.setOnClickListener { nextStep() }
                } else {
                    actionButton.text = "Grant Overlay Permission"
                    actionButton.setOnClickListener {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:$packageName"))
                        startActivity(intent)
                    }
                }
            }
            2 -> {
                iconView.text = "\uD83D\uDC41\uFE0F"
                titleView.text = "Accessibility Service"
                descView.text = "DosCom reads your screen to find buttons, text, and icons. It only scans when you ask \u2014 never in the background."
                val granted = DosComAccessibilityService.isConnected()
                if (granted) {
                    actionButton.text = "\u2713 Connected \u2014 Next \u2192"
                    (actionButton.background as GradientDrawable).setColor(Color.parseColor("#4CAF50"))
                    actionButton.setOnClickListener { nextStep() }
                } else {
                    actionButton.text = "Open Accessibility Settings"
                    actionButton.setOnClickListener {
                        try {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        } catch (e: Exception) {
                            startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                }
            }
            3 -> {
                iconView.text = "\uD83D\uDCF8"
                titleView.text = "Screen Capture"
                descView.text = "DosCom uses this as a backup when it can't read the screen through accessibility. It captures a screenshot only when you ask a question."
                val granted = ScreenshotHelper.hasPermission()
                if (granted) {
                    actionButton.text = "\u2713 Granted \u2014 Next \u2192"
                    (actionButton.background as GradientDrawable).setColor(Color.parseColor("#4CAF50"))
                    actionButton.setOnClickListener { nextStep() }
                } else {
                    actionButton.text = "Grant Screen Capture"
                    actionButton.setOnClickListener {
                        ScreenshotHelper.requestPermission(this, SCREEN_CAPTURE_REQUEST)
                    }
                }
            }
            4 -> {
                iconView.text = "\uD83D\uDD0B"
                titleView.text = "Battery Optimization"
                descView.text = "Keep DosCom alive in the background. Without this, your phone might kill it when the screen is off."
                val granted = BatteryOptimizationHelper.isExempt(this)
                if (granted) {
                    actionButton.text = "\u2713 Exempt \u2014 Next \u2192"
                    (actionButton.background as GradientDrawable).setColor(Color.parseColor("#4CAF50"))
                    actionButton.setOnClickListener { nextStep() }
                } else {
                    actionButton.text = "Disable Battery Optimization"
                    actionButton.setOnClickListener {
                        BatteryOptimizationHelper.requestBatteryExemption(this)
                    }
                }
            }
            5 -> {
                iconView.text = "\uD83D\uDE80"
                titleView.text = "Auto Launch (Oppo/ColorOS)"
                descView.text = "If you're on an Oppo/Realme/OnePlus device:\n\n1. Open Phone Manager\n2. Go to App Auto-Launch\n3. Find DosCom and enable it\n\nThis is a manual step. If you're not on ColorOS, just tap Next."
                actionButton.text = "Next \u2192"
                actionButton.setOnClickListener { nextStep() }
            }
            6 -> {
                iconView.text = "\uD83C\uDF99\uFE0F"
                titleView.text = "Microphone Permission"
                descView.text = "For voice commands. Long-press DosCom and speak your question instead of typing."
                val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    actionButton.text = "\u2713 Granted \u2014 Next \u2192"
                    (actionButton.background as GradientDrawable).setColor(Color.parseColor("#4CAF50"))
                    actionButton.setOnClickListener { nextStep() }
                } else {
                    actionButton.text = "Grant Audio Permission"
                    actionButton.setOnClickListener {
                        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_PERMISSION_REQUEST)
                    }
                }
            }
            7 -> {
                iconView.text = "\uD83D\uDD11"
                titleView.text = "Vision API Key (Optional)"
                descView.text = "If you have a Gemini API key, enter it below. This is only needed when accessibility can't find elements on screen.\n\nYou can skip this and add it later."

                val apiKeyInput = EditText(this).apply {
                    hint = "Paste your Gemini API key here..."
                    setTextColor(Color.BLACK)
                    setHintTextColor(Color.LTGRAY)
                    background = GradientDrawable().apply {
                        setColor(Color.WHITE)
                        setStroke(dp(1), Color.parseColor("#BDBDBD"))
                        cornerRadius = dp(8).toFloat()
                    }
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(16)
                    }
                }
                layout.addView(apiKeyInput)

                actionButton.text = "Save & Finish"
                actionButton.setOnClickListener {
                    val key = apiKeyInput.text.toString().trim()
                    if (key.isNotBlank()) {
                        ConfigManager.saveApiKey(this, key)
                        GeminiVisionClient.configure(key)
                    }
                    completeOnboarding()
                }

                val skipButton = Button(this).apply {
                    text = "Skip \u2192"
                    setTextColor(Color.parseColor("#6200EE"))
                    setBackgroundColor(Color.TRANSPARENT)
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(8)
                    }
                    setOnClickListener { completeOnboarding() }
                }
                layout.addView(actionButton)
                layout.addView(skipButton)
                setContentView(scrollView)
                return
            }
        }

        layout.addView(actionButton)
        setContentView(scrollView)
    }

    private fun nextStep() {
        currentStep++
        if (currentStep >= totalSteps) {
            completeOnboarding()
        } else {
            showStep(currentStep)
        }
    }

    private fun completeOnboarding() {
        getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_complete", true)
            .apply()

        try {
            ServiceManager.startOverlayService(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Toast.makeText(this, "DosCom is now active! \uD83E\uDD16", Toast.LENGTH_SHORT).show()
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_CAPTURE_REQUEST) {
            ScreenshotHelper.onPermissionResult(resultCode, data)
            showStep(currentStep)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST) {
            showStep(currentStep)
        }
    }
}
