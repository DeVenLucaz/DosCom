package com.devenlucaz.doscom.animation

enum class ClimbState {
    IDLE, PREP, CLIMBING, MID_REST, PULLING_OVER, SLIDING_DOWN, FALLING, LANDING
}

object ClimbEngine {
    
    var animSpeedMultiplier: Float = 1.0f
    var currentState = ClimbState.IDLE

    fun updateState(newState: ClimbState) {
        currentState = newState
    }
}
