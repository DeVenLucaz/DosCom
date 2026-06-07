package com.devenlucaz.doscom.personality

import com.devenlucaz.doscom.animation.IdleAnimationEngine

enum class UserMood { NORMAL, FOCUSED, TIRED, HYPED, SILLY, SUPPORTIVE }

object MoodEngine {
    var currentMood: UserMood = UserMood.NORMAL

    fun applyMoodToAnimation(engine: IdleAnimationEngine) {
        when (currentMood) {
            UserMood.TIRED -> {
                engine.animSpeedMultiplier = 0.5f
                engine.targetState.eyesHalf = true
            }
            UserMood.HYPED -> {
                engine.animSpeedMultiplier = 1.5f
            }
            UserMood.SILLY -> {
                engine.animSpeedMultiplier = 1.2f
            }
            UserMood.FOCUSED -> {
                engine.animSpeedMultiplier = 0.3f
                engine.targetState.bodyOffsetY = 0f
            }
            UserMood.SUPPORTIVE -> {
                engine.animSpeedMultiplier = 0.8f
                engine.targetState.mouthExpression = 1 // happy
            }
            UserMood.NORMAL -> {
                engine.animSpeedMultiplier = 1.0f
            }
        }
    }

    fun detectFromChat(input: String): UserMood? {
        val lower = input.lowercase()
        return when {
            lower.contains("focus") || lower.contains("work") || lower.contains("busy") -> UserMood.FOCUSED
            lower.contains("tired") || lower.contains("sleep") || lower.contains("exhausted") -> UserMood.TIRED
            lower.contains("hype") || lower.contains("excited") || lower.contains("let's go") -> UserMood.HYPED
            lower.contains("joke") || lower.contains("silly") || lower.contains("haha") -> UserMood.SILLY
            lower.contains("love") || lower.contains("thank") || lower.contains("support") -> UserMood.SUPPORTIVE
            else -> null
        }
    }
}
