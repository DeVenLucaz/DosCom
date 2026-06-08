package com.devenlucaz.doscom.observation

object RepeatDetector {
    private val eventCounts = HashMap<String, Pair<Int, Long>>()
    private val cooldowns = HashMap<String, Long>()
    
    fun onEvent(pkg: String, viewId: String?, onTrigger: () -> Unit) {
        if (viewId.isNullOrEmpty() || pkg.isEmpty()) return
        
        val now = System.currentTimeMillis()
        val cooldownEnd = cooldowns[pkg] ?: 0L
        if (now < cooldownEnd) return
        
        val key = "$pkg:$viewId"
        val pair = eventCounts[key] ?: Pair(0, now)
        val count = pair.first
        val firstTime = pair.second
        
        if (now - firstTime > 60000) {
            eventCounts[key] = Pair(1, now)
        } else {
            val newCount = count + 1
            if (newCount >= 3) {
                onTrigger()
                cooldowns[pkg] = now + 5 * 60 * 1000L
                eventCounts.remove(key)
            } else {
                eventCounts[key] = Pair(newCount, firstTime)
            }
        }
    }
}
