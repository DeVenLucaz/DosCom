package com.devenlucaz.doscom.character

import android.os.SystemClock

data class QueuedAnimation(
    val animationId: String,
    val priority: Int,
    val durationMs: Long,
    val enqueueTime: Long = SystemClock.uptimeMillis()
)

class AnimationQueue {
    companion object {
        const val PRIORITY_CRITICAL = 4
        const val PRIORITY_HIGH = 3
        const val PRIORITY_MEDIUM = 2
        const val PRIORITY_LOW = 1
        const val PRIORITY_AMBIENT = 0
    }

    private val queue = mutableListOf<QueuedAnimation>()
    private var currentAnimation: QueuedAnimation? = null
    private var currentAnimationStartTime: Long = 0

    fun enqueue(animationId: String, priority: Int, durationMs: Long) {
        val newAnim = QueuedAnimation(animationId, priority, durationMs)
        
        val active = currentAnimation
        if (active != null) {
            val elapsed = SystemClock.uptimeMillis() - currentAnimationStartTime
            if (elapsed >= active.durationMs) {
                // Current animation finished
                currentAnimation = newAnim
                currentAnimationStartTime = SystemClock.uptimeMillis()
                return
            }
            
            if (priority > active.priority) {
                // Higher priority interrupts lower
                currentAnimation = newAnim
                currentAnimationStartTime = SystemClock.uptimeMillis()
            } else {
                // Same or lower priority: queue it
                queue.add(newAnim)
                queue.sortByDescending { it.priority }
            }
        } else {
            currentAnimation = newAnim
            currentAnimationStartTime = SystemClock.uptimeMillis()
        }
    }

    fun currentPriority(): Int {
        updateState()
        return currentAnimation?.priority ?: -1
    }

    fun isPlaying(): Boolean {
        updateState()
        return currentAnimation != null
    }
    
    private fun updateState() {
        val active = currentAnimation
        if (active != null) {
            val elapsed = SystemClock.uptimeMillis() - currentAnimationStartTime
            if (elapsed >= active.durationMs) {
                currentAnimation = null
                if (queue.isNotEmpty()) {
                    currentAnimation = queue.removeAt(0)
                    currentAnimationStartTime = SystemClock.uptimeMillis()
                }
            }
        } else if (queue.isNotEmpty()) {
            currentAnimation = queue.removeAt(0)
            currentAnimationStartTime = SystemClock.uptimeMillis()
        }
    }
}
