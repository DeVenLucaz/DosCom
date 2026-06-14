package com.devenlucaz.doscom.animation

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import com.devenlucaz.doscom.character.CompanionRenderer
import com.devenlucaz.doscom.personality.UserMood
import kotlin.math.abs
import kotlin.math.hypot

enum class MimeType {
    SLIDE
}

object MimeEngine {

    var animSpeedMultiplier: Float = 1.0f

    fun selectMime(
        fromX: Float, fromY: Float,
        toX: Float, toY: Float,
        currentMood: UserMood,
        currentHour: Int
    ): MimeType {
        // Since we migrated to the 3D model, we no longer need the 2D sprite 
        // skateboard/rocket logic. We simply return SLIDE for all interpolation.
        return MimeType.SLIDE
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
