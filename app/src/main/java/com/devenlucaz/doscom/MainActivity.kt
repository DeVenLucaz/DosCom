import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.devenlucaz.doscom.service.DosComAccessibilityService
import com.devenlucaz.doscom.service.ServiceManager

class MainActivity : AppCompatActivity() {

    private lateinit var apkPickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null) // Prevent saved state crash loops
        
        val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        val lastCrash = prefs.getString("last_crash", null)
        if (lastCrash != null) {
            prefs.edit().remove("last_crash").apply()
            val scrollView = android.widget.ScrollView(this)
            val textView = TextView(this).apply {
                text = "FATAL CRASH PREVENTED. LOG:\n\n$lastCrash"
                setPadding(32, 32, 32, 32)
                setTextIsSelectable(true)
            }
            scrollView.addView(textView)
            setContentView(scrollView)
            return // STOP EXECUTION! Do not initialize anything else.
        }
        
        apkPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                installApk(uri)
            }
        }

        try {
            checkPermissionsAndStart()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Init Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val overlayGranted = Settings.canDrawOverlays(this)
        val accessibilityGranted = isAccessibilityServiceEnabled(this, DosComAccessibilityService::class.java)
        
        if (overlayGranted && accessibilityGranted) {
            try {
                ServiceManager.startOverlayService(this)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to start service: ${e.message}", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

    private fun checkPermissionsAndStart() {
        val overlayGranted = Settings.canDrawOverlays(this)
        val accessibilityGranted = isAccessibilityServiceEnabled(this, DosComAccessibilityService::class.java)

        if (!overlayGranted || !accessibilityGranted) {
            showPermissionUI(overlayGranted, accessibilityGranted)
        }
    }

    private fun showPermissionUI(overlayGranted: Boolean, accessibilityGranted: Boolean) {
        try {
            val scrollView = android.widget.ScrollView(this)
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(64, 64, 64, 64)
            }
            scrollView.addView(layout)

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
                    text = "Android 13 restricts Accessibility for sideloaded apps.\n\nOption A (Stock Android): Open App Info, tap 3-dots, 'Allow restricted settings'.\n\nOption B (ColorOS): The 3-dots menu is missing. You MUST bypass it by reinstalling the APK through this app itself. Click 'ColorOS Bypass', select the doscom app-release.apk you downloaded, and click Update."
                    setPadding(0, 32, 0, 32)
                }
                layout.addView(desc)

                val btnAppInfo = Button(this).apply {
                    text = "Option A: Open App Info"
                    setOnClickListener {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                        startActivity(intent)
                    }
                }
                layout.addView(btnAppInfo)
                
                val btnBypass = Button(this).apply {
                    text = "Option B: ColorOS Bypass (Select APK)"
                    setOnClickListener {
                        apkPickerLauncher.launch(arrayOf("*/*"))
                    }
                }
                layout.addView(btnBypass)

                val btnAccessibility = Button(this).apply {
                    text = "Step 2: Open Accessibility Settings"
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

            setContentView(scrollView)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "UI Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun installApk(uri: Uri) {
        Toast.makeText(this, "Preparing installation... Please wait.", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val packageInstaller = packageManager.packageInstaller
                val params = android.content.pm.PackageInstaller.SessionParams(
                    android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
                )
                val sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)

                val outStream = session.openWrite("doscom_update", 0, -1)
                contentResolver.openInputStream(uri)?.use { inStream ->
                    inStream.copyTo(outStream)
                }
                session.fsync(outStream)
                outStream.close()

                val intent = Intent(this, MainActivity::class.java)
                intent.action = "com.devenlucaz.doscom.INSTALL_COMPLETE"
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                )

                session.commit(pendingIntent.intentSender)
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
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
