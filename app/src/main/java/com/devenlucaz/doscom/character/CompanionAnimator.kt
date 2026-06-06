package com.devenlucaz.doscom.character

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.view.WindowManager
import com.devenlucaz.doscom.utils.ScreenMetrics
import kotlin.math.hypot

object CompanionAnimator {

    private var currentAnimator: ValueAnimator? = null

    fun walkTo(
        targetX: Int,
        targetY: Int,
        characterView: CompanionRenderer,
        layoutParams: WindowManager.LayoutParams,
        windowManager: WindowManager,
        onArrival: () -> Unit
    ) {
        currentAnimator?.cancel()

        val startX = layoutParams.x
        val startY = layoutParams.y

        val distance = hypot((targetX - startX).toDouble(), (targetY - startY).toDouble())
        
        // Duration scales with distance, clamped between 400ms and 2000ms
        val durationMs = distance.toLong().coerceIn(400L, 2000L)

        if (targetX >= startX) {
            characterView.scaleX = 1f
            characterView.setState(CharacterState.WALK_RIGHT)
        } else {
            characterView.scaleX = -1f
            characterView.setState(CharacterState.WALK_LEFT)
        }

        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                val currentX = startX + ((targetX - startX) * fraction).toInt()
                val currentY = startY + ((targetY - startY) * fraction).toInt()

                layoutParams.x = currentX
                layoutParams.y = currentY
                
                try {
                    windowManager.updateViewLayout(characterView, layoutParams)
                } catch (e: Exception) {
                    cancel()
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    characterView.scaleX = 1f
                    onArrival()
                }
            })
            start()
        }
    }

    fun walkToEdge(
        context: Context,
        characterView: CompanionRenderer,
        layoutParams: WindowManager.LayoutParams,
        windowManager: WindowManager,
        onArrival: () -> Unit
    ) {
        val screenWidth = ScreenMetrics.getScreenWidth(context)
        val currentX = layoutParams.x
        
        // Fallback size if view hasn't been laid out yet
        val charWidth = characterView.width.takeIf { it > 0 } ?: (80 * context.resources.displayMetrics.density).toInt()
        
        val targetX = if (currentX + charWidth / 2 < screenWidth / 2) {
            0
        } else {
            screenWidth - charWidth
        }

        val targetY = layoutParams.y

        walkTo(targetX, targetY, characterView, layoutParams, windowManager, onArrival)
    }
}
