package com.devenlucaz.doscom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.devenlucaz.doscom.onboarding.OnboardingActivity
import com.devenlucaz.doscom.service.ServiceManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        val onboardingComplete = prefs.getBoolean("onboarding_complete", false)

        if (onboardingComplete) {
            try {
                ServiceManager.startOverlayService(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            finish()
        } else {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
    }
}
