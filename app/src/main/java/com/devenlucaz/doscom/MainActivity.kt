package com.devenlucaz.doscom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.devenlucaz.doscom.service.DosComAccessibilityService
import com.devenlucaz.doscom.service.ServiceManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissionsAndStart()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val overlayGranted = Settings.canDrawOverlays(this)
        val accessibilityGranted = isAccessibilityServiceEnabled(this, DosComAccessibilityService::class.java)

        if (overlayGranted && accessibilityGranted) {
            ServiceManager.startOverlayService(this)
            finish()
        } else {
            showPermissionUI(overlayGranted, accessibilityGranted)
        }
    }

    private fun showPermissionUI(overlayGranted: Boolean, accessibilityGranted: Boolean) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "DosCom Permissions Required"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(title)

        if (!overlayGranted) {
            val btnOverlay = Button(this).apply {
                text = "1. Grant Overlay Permission"
                setOnClickListener {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                }
            }
            layout.addView(btnOverlay)
        }

        if (!accessibilityGranted) {
            val desc = TextView(this).apply {
                text = "Android 13+ restricts Accessibility for downloaded apps.\n\nTo fix 'Restricted Setting':\n1. Click 'Open App Info' below.\n2. Tap the 3 dots (top right) and 'Allow restricted settings'.\n3. Click 'Open Accessibility' and enable DosCom."
                setPadding(0, 32, 0, 32)
            }
            layout.addView(desc)

            val btnAppInfo = Button(this).apply {
                text = "2. Open App Info"
                setOnClickListener {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                    startActivity(intent)
                }
            }
            layout.addView(btnAppInfo)

            val btnAccessibility = Button(this).apply {
                text = "3. Open Accessibility Settings"
                setOnClickListener {
                    try {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        startActivity(intent)
                    }
                }
            }
            layout.addView(btnAccessibility)
        }

        setContentView(layout)
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        if (enabledServices != null) {
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(context.packageName + "/" + service.name, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}
