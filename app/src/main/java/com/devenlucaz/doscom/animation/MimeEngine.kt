package com.devenlucaz.doscom.animation

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import com.devenlucaz.doscom.character.CompanionRenderer
import com.devenlucaz.doscom.personality.UserMood
import kotlin.math.abs
import kotlin.math.hypot

enum class MimeType {
    WALK, SLIDE, STAIRCASE, SKATEBOARD, TIGHTROPE,
    SHOPPING_CART, BALLOON, ROCKET, ELEVATOR,
    CLIMB_UP, CLIMB_DOWN, FALL, MOONWALK, CRAWL
}

object MimeEngine {

    var animSpeedMultiplier: Float = 1.0f

    fun selectMime(
        fromX: Float, fromY: Float,
        toX: Float, toY: Float,
        currentMood: UserMood,
        currentHour: Int
    ): MimeType {
        if (Math.random() < 0.01) return MimeType.MOONWALK
        
        val dx = toX - fromX
        val dy = toY - fromY
        val distance = hypot(dx, dy)

        return when {
            distance < 100f -> MimeType.WALK
            distance in 100f..300f -> {
                if (currentMood == UserMood.FOCUSED) MimeType.TIGHTROPE else MimeType.SKATEBOARD
            }
            else -> {
                when {
                    currentMood == UserMood.HYPED && dy > 0 -> MimeType.FALL
                    currentMood == UserMood.HYPED && dy < 0 -> MimeType.ROCKET
                    currentMood == UserMood.SILLY -> if (dy < 0) MimeType.BALLOON else MimeType.SHOPPING_CART
                    currentHour >= 23 || currentHour < 6 || currentMood == UserMood.TIRED -> MimeType.CRAWL
                    dy > 50f -> MimeType.SLIDE
                    dy < -50f -> MimeType.STAIRCASE
                    else -> MimeType.WALK
                }
            }
        }
    }

    // Since we need to update window layout parameters, we accept an onUpdate callback
    fun executeMime(
        mime: MimeType,
        fromPos: PointF,
        toPos: PointF,
        renderer: CompanionRenderer,
        onUpdate: (Float, Float) -> Unit,
        onComplete: () -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        val duration = 1500L
        val frames = 30
        val delayPerFrame = duration / frames

        var currentFrame = 0

        val runnable = object : Runnable {
            override fun run() {
                val progress = currentFrame.toFloat() / frames
                val currentX = fromPos.x + (toPos.x - fromPos.x) * progress
                val currentY = fromPos.y + (toPos.y - fromPos.y) * progress

                onUpdate(currentX, currentY)

                if (currentFrame < frames) {
                    currentFrame++
                    handler.postDelayed(this, delayPerFrame)
                } else {
                    onComplete()
                }
            }
        }
        handler.post(runnable)
    }
}
