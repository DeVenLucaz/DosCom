package com.devenlucaz.doscom.animation

import com.devenlucaz.doscom.character.AnimationState

object MovementEngine {
    fun generateClimbFrames(direction: Int): List<AnimationState> {
        // direction: 1 = down, -1 = up
        return listOf(
            AnimationState(
                leftArmAngle = -67.5f, rightArmAngle = -22.5f,
                leftLegAngle = -22.5f, rightLegAngle = 0f
            ),
            AnimationState(
                leftArmAngle = -22.5f, rightArmAngle = -22.5f,
                leftLegAngle = -10f, rightLegAngle = -10f
            ),
            AnimationState(
                leftArmAngle = -22.5f, rightArmAngle = -67.5f,
                leftLegAngle = 0f, rightLegAngle = -22.5f
            ),
            AnimationState(
                leftArmAngle = -22.5f, rightArmAngle = -22.5f,
                leftLegAngle = -10f, rightLegAngle = -10f
            )
        )
    }

    fun generateCrawlFrames(direction: Int): List<AnimationState> {
        // direction: 1 = right, -1 = left
        return listOf(
            AnimationState(
                leftArmAngle = -22.5f, rightArmAngle = 22.5f,
                leftLegAngle = 22.5f, rightLegAngle = -22.5f,
                bodyRotation = 45f
            ),
            AnimationState(
                leftArmAngle = 0f, rightArmAngle = 0f,
                leftLegAngle = 0f, rightLegAngle = 0f,
                bodyRotation = 45f
            ),
            AnimationState(
                leftArmAngle = 22.5f, rightArmAngle = -22.5f,
                leftLegAngle = -22.5f, rightLegAngle = 22.5f,
                bodyRotation = 45f
            ),
            AnimationState(
                leftArmAngle = 0f, rightArmAngle = 0f,
                leftLegAngle = 0f, rightLegAngle = 0f,
                bodyRotation = 45f
            )
        )
    }
}
