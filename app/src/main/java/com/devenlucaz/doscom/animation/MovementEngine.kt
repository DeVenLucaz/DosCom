package com.devenlucaz.doscom.animation

import com.devenlucaz.doscom.character.AnimationState

object MovementEngine {
    fun generateClimbFrames(direction: Int): List<AnimationState> {
        // direction: 1 = down, -1 = up
        return listOf(
            AnimationState(
                leftArmAngle = -135f, rightArmAngle = -45f,
                leftLegAngle = -45f, rightLegAngle = 0f
            ),
            AnimationState(
                leftArmAngle = -90f, rightArmAngle = -90f,
                leftLegAngle = -20f, rightLegAngle = -20f
            ),
            AnimationState(
                leftArmAngle = -45f, rightArmAngle = -135f,
                leftLegAngle = 0f, rightLegAngle = -45f
            ),
            AnimationState(
                leftArmAngle = -90f, rightArmAngle = -90f,
                leftLegAngle = -20f, rightLegAngle = -20f
            )
        )
    }

    fun generateCrawlFrames(direction: Int): List<AnimationState> {
        // direction: 1 = right, -1 = left
        return listOf(
            AnimationState(
                leftArmAngle = -45f, rightArmAngle = 45f,
                leftLegAngle = 45f, rightLegAngle = -45f,
                bodyRotation = 90f
            ),
            AnimationState(
                leftArmAngle = 0f, rightArmAngle = 0f,
                leftLegAngle = 0f, rightLegAngle = 0f,
                bodyRotation = 90f
            ),
            AnimationState(
                leftArmAngle = 45f, rightArmAngle = -45f,
                leftLegAngle = -45f, rightLegAngle = 45f,
                bodyRotation = 90f
            ),
            AnimationState(
                leftArmAngle = 0f, rightArmAngle = 0f,
                leftLegAngle = 0f, rightLegAngle = 0f,
                bodyRotation = 90f
            )
        )
    }
}
