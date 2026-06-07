package com.devenlucaz.doscom

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devenlucaz.doscom.api.GeminiVisionClient
import com.devenlucaz.doscom.onboarding.OnboardingActivity
import com.devenlucaz.doscom.service.DosComAccessibilityService
import com.devenlucaz.doscom.service.ServiceManager
import com.devenlucaz.doscom.utils.ConfigManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load API key on start
        val apiKey = ConfigManager.loadApiKey(this)
        if (apiKey != null) {
            GeminiVisionClient.configure(apiKey)
        }

        val prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        val onboardingComplete = prefs.getBoolean("onboarding_complete", false)

        if (!onboardingComplete) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // Check critical permissions
        if (!Settings.canDrawOverlays(this)) {
            // Reset onboarding if overlay permission was revoked
            prefs.edit().putBoolean("onboarding_complete", false).apply()
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_complete", false)) return
        if (!Settings.canDrawOverlays(this)) return

        // Start service and show control panel
        try {
            ServiceManager.startOverlayService(this)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start DosCom: ${e.message}", Toast.LENGTH_LONG).show()
        }

        showControlPanel()
    }

    private fun showControlPanel() {
        val density = resources.displayMetrics.density
        val dp = { value: Int -> (value * density).toInt() }

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(48), dp(32), dp(32))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        scrollView.addView(layout)

        val iconView = TextView(this).apply {
            text = "\uD83E\uDD16"
            textSize = 64f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
        layout.addView(iconView)

        val titleView = TextView(this).apply {
            text = "DosCom is running"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#212121"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        layout.addView(titleView)

        val descView = TextView(this).apply {
            text = "Your AI companion is floating on your screen.\nLong-press DosCom to ask a question."
            textSize = 16f
            setTextColor(Color.parseColor("#616161"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(32))
        }
        layout.addView(descView)

        // Status indicators
        val statusLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F5F5F5"))
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(24)
            }
        }

        val overlayStatus = Settings.canDrawOverlays(this)
        val accessibilityStatus = DosComAccessibilityService.isConnected()
        val visionStatus = GeminiVisionClient.isConfigured()

        addStatusRow(statusLayout, "Overlay", overlayStatus, dp)
        addStatusRow(statusLayout, "Accessibility", accessibilityStatus, dp)
        addStatusRow(statusLayout, "Vision API", visionStatus, dp)

        layout.addView(statusLayout)

        val stopButton = Button(this).apply {
            text = "Stop DosCom"
            setTextColor(Color.WHITE)
            textSize = 16f
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F44336"))
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(32), dp(16), dp(32), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
            setOnClickListener {
                ServiceManager.stopOverlayService(this@MainActivity)
                Toast.makeText(this@MainActivity, "DosCom stopped", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        layout.addView(stopButton)

        val resetButton = Button(this).apply {
            text = "Re-run Setup"
            setTextColor(Color.parseColor("#6200EE"))
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("onboarding_complete", false).apply()
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
            }
        }
        layout.addView(resetButton)

        setContentView(scrollView)
    }

    private fun addStatusRow(parent: LinearLayout, label: String, active: Boolean, dp: (Int) -> Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }

        val dot = TextView(this).apply {
            text = if (active) "\u25CF" else "\u25CB"
            setTextColor(if (active) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
            textSize = 16f
            setPadding(0, 0, dp(8), 0)
        }
        row.addView(dot)

        val labelView = TextView(this).apply {
            text = "$label: ${if (active) "Active" else "Inactive"}"
            textSize = 14f
            setTextColor(Color.parseColor("#424242"))
        }
        row.addView(labelView)

        parent.addView(row)
    }
}
