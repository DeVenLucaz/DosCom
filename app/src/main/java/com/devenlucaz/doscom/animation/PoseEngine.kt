package com.devenlucaz.doscom.animation

import com.devenlucaz.doscom.character.AnimationState

enum class RobotPose {
    HANG_LEFT, HANG_RIGHT, GRIP_TOP, SIT_BOTTOM, FLOATING
}

object PoseEngine {

    fun detectPose(
        x: Int, y: Int,
        screenW: Int, screenH: Int,
        charW: Int, charH: Int
    ): RobotPose {
        return if (y < screenH * 0.15f) {
            RobotPose.GRIP_TOP
        } else if (y > screenH * 0.80f) {
            RobotPose.SIT_BOTTOM
        } else if (x < charW * 1.5f) {
            RobotPose.HANG_LEFT
        } else if (x > screenW - charW * 1.5f) {
            RobotPose.HANG_RIGHT
        } else {
            RobotPose.FLOATING
        }
    }

    fun getTargetState(pose: RobotPose): AnimationState {
        return when (pose) {
            RobotPose.HANG_LEFT -> AnimationState(
                leftArmAngle = -90f,
                rightArmAngle = 0f,
                leftLegAngle = 0f,
                rightLegAngle = 0f,
                bodyRotation = 5f,
                scaleX = 1f
            )
            RobotPose.HANG_RIGHT -> AnimationState(
                rightArmAngle = 90f,
                leftArmAngle = 0f,
                leftLegAngle = 0f,
                rightLegAngle = 0f,
                bodyRotation = -5f,
                scaleX = -1f
            )
            RobotPose.GRIP_TOP -> AnimationState(
                leftArmAngle = -160f,
                rightArmAngle = 160f,
                leftLegAngle = 20f,
                rightLegAngle = -20f,
                bodyOffsetY = -10f
            )
            RobotPose.SIT_BOTTOM -> AnimationState(
                leftLegAngle = -90f,
                rightLegAngle = -90f,
                leftArmAngle = -140f,
                rightArmAngle = 0f,
                bodyOffsetY = 15f
            )
            RobotPose.FLOATING -> AnimationState(
                bodyRotation = 360f,
                eyesWide = true,
                mouthExpression = 2
            )
        }
    }
}
