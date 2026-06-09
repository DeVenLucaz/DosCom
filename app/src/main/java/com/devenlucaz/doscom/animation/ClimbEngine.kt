package com.devenlucaz.doscom.animation

import android.os.Handler

enum class ClimbState {
    IDLE, PREP, CLIMBING, MID_REST, PULLING_OVER, SLIDING_DOWN, FALLING, LANDING
}

object ClimbEngine {
    
    var animSpeedMultiplier: Float = 1.0f
    var currentState = ClimbState.IDLE

    fun updateState(newState: ClimbState) {
        currentState = newState
    }

    fun startClimb(engine: IdleAnimationEngine, handler: Handler) {
        if (currentState != ClimbState.IDLE) return
        
        // 1. PREP
        updateState(ClimbState.PREP)
        engine.targetState.bodyRotation = 15f
        engine.targetState.leftArmAngle = -45f
        engine.targetState.rightArmAngle = -45f
        
        handler.postDelayed({
            // 2. CLIMBING (Pulse 1)
            updateState(ClimbState.CLIMBING)
            engine.targetState.bodyOffsetY = -8f
            engine.targetState.leftArmAngle = -160f
            engine.targetState.rightArmAngle = 0f
            
            handler.postDelayed({
                // CLIMBING (Pulse 2)
                engine.targetState.bodyOffsetY = -4f
                engine.targetState.leftArmAngle = 0f
                engine.targetState.rightArmAngle = -160f
                
                handler.postDelayed({
                    // 3. MID_REST
                    updateState(ClimbState.MID_REST)
                    engine.targetState.leftArmAngle = -90f
                    engine.targetState.rightArmAngle = -90f
                    engine.targetState.eyesHalf = true
                    
                    handler.postDelayed({
                        // 4. PULLING_OVER
                        updateState(ClimbState.PULLING_OVER)
                        engine.targetState.bodyOffsetY = -20f
                        engine.targetState.bodyRotation = 0f
                        engine.targetState.leftArmAngle = -160f
                        engine.targetState.rightArmAngle = -160f
                        engine.targetState.eyesHalf = false
                        
                        handler.postDelayed({
                            // 5. SLIDING_DOWN
                            updateState(ClimbState.SLIDING_DOWN)
                            engine.targetState.bodyOffsetY = 0f
                            engine.targetState.scaleX *= -1f
                            
                            handler.postDelayed({
                                // 6. LANDING
                                updateState(ClimbState.LANDING)
                                engine.targetState.bodyOffsetY = -8f
                                engine.targetState.eyesWide = true
                                
                                handler.postDelayed({
                                    engine.targetState.bodyOffsetY = 0f
                                }, (150 * animSpeedMultiplier).toLong())
                                
                                handler.postDelayed({
                                    engine.targetState.eyesWide = false
                                    updateState(ClimbState.IDLE)
                                }, (300 * animSpeedMultiplier).toLong())
                                
                            }, (400 * animSpeedMultiplier).toLong())
                        }, (500 * animSpeedMultiplier).toLong())
                    }, (600 * animSpeedMultiplier).toLong())
                }, (300 * animSpeedMultiplier).toLong())
            }, (300 * animSpeedMultiplier).toLong())
        }, (400 * animSpeedMultiplier).toLong())
    }
}
