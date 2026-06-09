package com.devenlucaz.doscom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.devenlucaz.doscom.onboarding.OnboardingActivity
import com.devenlucaz.doscom.service.CompanionOverlayService
import com.devenlucaz.doscom.service.ServiceManager
import com.devenlucaz.doscom.settings.SettingsActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        val onboardingComplete = prefs.getBoolean("onboarding_complete", false)

        if (!onboardingComplete) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        try {
            ServiceManager.startOverlayService(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }

        val statusText = TextView(this).apply {
            text = "DosCom is currently running.\n\nUse the overlay to interact with your companion!"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 64)
        }
        layout.addView(statusText)

        val settingsBtn = Button(this).apply {
            text = "Settings"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }
        layout.addView(settingsBtn)

        val stopBtn = Button(this).apply {
            text = "Stop DosCom"
            setOnClickListener {
                val stopIntent = Intent(this@MainActivity, CompanionOverlayService::class.java).apply {
                    action = "ACTION_STOP"
                }
                startService(stopIntent)
                finish()
            }
        }
        layout.addView(stopBtn)

        setContentView(layout)
    }
}
