import sys

file_path = "/data/data/com.termux/files/home/DosCom/app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt"

with open(file_path, "r") as f:
    content = f.read()

# 1. onStartCommand
old_start_cmd = """    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_DISABLE_GHOST_MODE") {
            prefs.edit().putInt("ghost_mode", 0).apply()
            updateGhostMode(0)
            updateForegroundNotification()
        }
        return START_NOT_STICKY
    }"""

new_start_cmd = """    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_DISABLE_GHOST_MODE" -> {
                prefs.edit().putInt("ghost_mode", 0).apply()
                updateGhostMode(0)
                updateForegroundNotification()
            }
            "ACTION_TOGGLE_GHOST_MODE" -> {
                val currentGhostMode = prefs.getInt("ghost_mode", 0)
                val newMode = if (currentGhostMode == 2) 0 else 2
                prefs.edit().putInt("ghost_mode", newMode).apply()
                updateGhostMode(newMode)
                updateForegroundNotification()
            }
            "ACTION_STOP" -> stopSelf()
        }
        return START_STICKY
    }"""
content = content.replace(old_start_cmd, new_start_cmd)

# 2. updateForegroundNotification
old_notification = """    private fun updateForegroundNotification() {
        val channelId = "doscom_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "DosCom Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps DosCom floating in the background"
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("DosCom is running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        val currentGhostMode = prefs.getInt("ghost_mode", 0)
        if (currentGhostMode != 0) {
            val disableIntent = Intent(this, CompanionOverlayService::class.java).apply {
                action = "ACTION_DISABLE_GHOST_MODE"
            }
            val disablePendingIntent = PendingIntent.getService(
                this, 0, disableIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Disable Ghost Mode", disablePendingIntent)
        }

        val notification = builder.build()
        startForeground(1, notification)
    }"""

new_notification = """    private fun updateForegroundNotification() {
        val channelId = "doscom_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "DosCom Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps DosCom floating in the background"
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val currentMode = com.devenlucaz.doscom.mode.ModeManager.getMode(this)
        val modeStr = when (currentMode) {
            com.devenlucaz.doscom.mode.CompanionMode.ALIVE -> "ALIVE"
            com.devenlucaz.doscom.mode.CompanionMode.AWAKE -> "AWAKE"
            com.devenlucaz.doscom.mode.CompanionMode.AWARE -> "AWARE"
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("DosCom is running ($modeStr)")
            .setContentText("Your cosmic companion is active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        // Settings Action
        val settingsIntent = Intent(this, com.devenlucaz.doscom.settings.SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            this, 0, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Settings", settingsPendingIntent)

        // Ghost Mode Toggle Action
        val currentGhostMode = prefs.getInt("ghost_mode", 0)
        val ghostToggleIntent = Intent(this, CompanionOverlayService::class.java).apply {
            action = "ACTION_TOGGLE_GHOST_MODE"
        }
        val ghostTogglePendingIntent = PendingIntent.getService(
            this, 1, ghostToggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, if (currentGhostMode == 2) "Disable Ghost Mode" else "Enable Ghost Mode", ghostTogglePendingIntent)

        // Stop Action
        val stopIntent = Intent(this, CompanionOverlayService::class.java).apply {
            action = "ACTION_STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Stop", stopPendingIntent)

        val notification = builder.build()
        startForeground(1, notification)
    }"""
content = content.replace(old_notification, new_notification)

old_on_create_start = """    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        updateForegroundNotification()
        try {
            com.devenlucaz.doscom.brain.BrainManager.init(this)
            setupOverlayView()"""

new_on_create_start = """    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("doscom_prefs", Context.MODE_PRIVATE)
        updateForegroundNotification()
        
        try {
            com.devenlucaz.doscom.brain.BrainManager.init(this)
            // Crash Recovery check
            if (prefs.getBoolean("last_crash", false)) {
                prefs.edit().putBoolean("last_crash", false).apply()
                handler.postDelayed({
                    idleEngine.targetState.leftArmAngle = -45f
                    idleEngine.targetState.rightArmAngle = 45f
                    idleEngine.targetState.mouthExpression = 2
                    showSpeechBubble("oops \uD83D\uDE05", layoutParams.x, layoutParams.y) {}
                    handler.postDelayed({
                        idleEngine.targetState.leftArmAngle = 0f
                        idleEngine.targetState.rightArmAngle = 0f
                        idleEngine.targetState.mouthExpression = 0
                    }, 3000)
                }, 1000)
            }
            
            setupOverlayView()"""
content = content.replace(old_on_create_start, new_on_create_start)

old_setup_view = """        idleEngine.animSpeedMultiplier = 1f
        idleEngine.sleepTimerMs = 5 * 60 * 1000L"""

new_setup_view = """        val animSpeed = prefs.getFloat("anim_speed", 1.0f)
        idleEngine.animSpeedMultiplier = animSpeed
        com.devenlucaz.doscom.animation.MimeEngine.animSpeedMultiplier = animSpeed
        com.devenlucaz.doscom.animation.ClimbEngine.animSpeedMultiplier = animSpeed
        idleEngine.sleepTimerMs = 5 * 60 * 1000L"""
content = content.replace(old_setup_view, new_setup_view)

with open(file_path, "w") as f:
    f.write(content)
