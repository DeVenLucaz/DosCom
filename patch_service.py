import re

with open("app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt", "r") as f:
    text = f.read()

# Add properties
fields = """    private val animationQueue = AnimationQueue()
    private lateinit var idleEngine: IdleAnimationEngine

    private lateinit var phoneEventReceiver: com.devenlucaz.doscom.events.PhoneEventReceiver
    private lateinit var appContextWatcher: com.devenlucaz.doscom.events.AppContextWatcher
    private lateinit var timeReactionEngine: com.devenlucaz.doscom.events.TimeReactionEngine"""

text = text.replace("    private val animationQueue = AnimationQueue()\n    private lateinit var idleEngine: IdleAnimationEngine", fields)

# Add receiver
receiver = """    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val reactionType = intent?.getStringExtra(DosComNotificationListener.EXTRA_REACTION_TYPE) ?: return
            handleNotificationReaction(reactionType)
        }
    }

    private val appCategoryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val categoryName = intent?.getStringExtra("category") ?: return
            handleAppCategoryReaction(categoryName)
        }
    }"""

text = text.replace("    private val notificationReceiver = object : BroadcastReceiver() {\n        override fun onReceive(context: Context?, intent: Intent?) {\n            val reactionType = intent?.getStringExtra(DosComNotificationListener.EXTRA_REACTION_TYPE) ?: return\n            handleNotificationReaction(reactionType)\n        }\n    }", receiver)

# Register receiver
reg = """            LocalBroadcastManager.getInstance(this).registerReceiver(
                notificationReceiver,
                IntentFilter(DosComNotificationListener.ACTION_NOTIFICATION_REACTION)
            )
            LocalBroadcastManager.getInstance(this).registerReceiver(
                appCategoryReceiver,
                IntentFilter("APP_CONTEXT_CHANGED")
            )"""

text = text.replace("""            LocalBroadcastManager.getInstance(this).registerReceiver(
                notificationReceiver,
                IntentFilter(DosComNotificationListener.ACTION_NOTIFICATION_REACTION)
            )""", reg)

# Add setup
setup = """        windowManager.addView(overlayView, layoutParams)
        startIdleBehaviors()
        
        phoneEventReceiver = com.devenlucaz.doscom.events.PhoneEventReceiver(idleEngine) {
            val screenHeight = ScreenMetrics.getScreenHeight(this)
            val animator = ValueAnimator.ofInt(layoutParams.y, screenHeight - overlayView.height)
            animator.duration = 500
            animator.addUpdateListener { anim ->
                layoutParams.y = anim.animatedValue as Int
                windowManager.updateViewLayout(overlayView, layoutParams)
            }
            animator.start()
        }
        phoneEventReceiver.register(this)

        appContextWatcher = com.devenlucaz.doscom.events.AppContextWatcher(this)
        appContextWatcher.start()

        timeReactionEngine = com.devenlucaz.doscom.events.TimeReactionEngine(this, idleEngine, windowManager)
        timeReactionEngine.start()"""

text = text.replace("""        windowManager.addView(overlayView, layoutParams)
        startIdleBehaviors()""", setup)

# Unregister
unreg = """    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(appCategoryReceiver)
        if (::phoneEventReceiver.isInitialized) phoneEventReceiver.unregister(this)
        if (::appContextWatcher.isInitialized) appContextWatcher.stop()
        if (::timeReactionEngine.isInitialized) timeReactionEngine.stop()"""

text = text.replace("""    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)""", unreg)

# App category reaction
reaction = """    private fun handleAppCategoryReaction(categoryName: String) {
        when (categoryName) {
            "MUSIC" -> idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.BOOMBOX
            "CAMERA" -> idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.BINOCULARS
            "MAPS" -> idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.TREASURE_MAP
            "CALCULATOR" -> idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.ABACUS
            else -> idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.NONE
        }
        Handler(Looper.getMainLooper()).postDelayed({
            idleEngine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.NONE
        }, 5000)
    }
}"""

text = text.replace("    }\n}", "    }\n\n" + reaction)

with open("app/src/main/java/com/devenlucaz/doscom/service/CompanionOverlayService.kt", "w") as f:
    f.write(text)

