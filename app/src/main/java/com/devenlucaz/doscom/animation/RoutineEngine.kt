package com.devenlucaz.doscom.animation

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.devenlucaz.doscom.brain.BrainInput
import com.devenlucaz.doscom.brain.BrainManager
import kotlin.random.Random

object RoutineEngine {
    private val handler = Handler(Looper.getMainLooper())
    
    // Define the animation clusters mapping to brain outputs
    private val chillAnimations = listOf(
        "Sit_Floor_Down", // 0
        "Lie_Down",       // 1
        "Push_Ups",       // 2
        "Sit_Ups",        // 3
        "Waving",         // 4
        "Cheering"        // 5
    )
    
    private val workAnimations = listOf(
        "Working_A",      // 6
        "Working_B",      // 7
        "Pickaxing",      // 8
        "Hammering",      // 9
        "Digging"         // 10
    )
    
    private val magicAnimations = listOf(
        "Ranged_Magic_Spellcasting", // 11
        "Throw",                     // 12
        "Melee_Unarmed_Attack_Punch_A", // 13
        "Dodge_Backward"             // 14
    )

    fun chooseNextActivity(context: Context, wanderEngine: WanderEngine, idleEngine: IdleAnimationEngine) {
        val inputs = BrainInput.buildInputs(context)
        val decisions = BrainManager.brain.think(inputs)
        val confidence = BrainManager.brain.lastConfidence
        
        // High confidence means use brain's choice, low confidence means random chaos
        val useBrain = confidence > (Random.nextFloat() * 50f)
        
        val category = if (useBrain) {
            // Pick the category that scored the highest across the decisions?
            // Actually, we can just randomly weight based on the best scores or just pick one
            Random.nextInt(3)
        } else {
            Random.nextInt(3)
        }

        var chosenAnimation = "Idle_A"
        var clusterId = 0
        var animId = 0
        
        when (category) {
            0 -> {
                animId = if (useBrain) decisions[0] else Random.nextInt(6)
                chosenAnimation = chillAnimations[animId]
                clusterId = 0
            }
            1 -> {
                animId = if (useBrain) decisions[1] - 6 else Random.nextInt(5)
                chosenAnimation = workAnimations[animId.coerceIn(0, 4)]
                clusterId = 1
            }
            2 -> {
                animId = if (useBrain) decisions[2] - 11 else Random.nextInt(4)
                chosenAnimation = magicAnimations[animId.coerceIn(0, 3)]
                clusterId = 2
            }
        }
        
        // Train the brain that this was a good choice (reinforcement)
        BrainManager.brain.learn(inputs, intArrayOf(
            if (clusterId == 0) animId else 0,
            if (clusterId == 1) animId + 6 else 6,
            if (clusterId == 2) animId + 11 else 11
        ), reward = 1.0f)
        
        executeActivity(chosenAnimation, wanderEngine, idleEngine)
    }
    
    private fun executeActivity(animName: String, wanderEngine: WanderEngine, idleEngine: IdleAnimationEngine) {
        // Some animations require a sequence (like Sit down -> Sit idle)
        when (animName) {
            "Sit_Floor_Down" -> {
                idleEngine.targetState.animationName = "Sit_Floor_Down"
                handler.postDelayed({
                    idleEngine.targetState.animationName = "Sit_Floor_Idle"
                    handler.postDelayed({
                        idleEngine.targetState.animationName = "Sit_Floor_StandUp"
                        handler.postDelayed({ wanderEngine.scheduleWander() }, 2000)
                    }, Random.nextLong(10000, 30000))
                }, 2000) // Assuming Sit_Floor_Down takes ~2 seconds
            }
            "Lie_Down" -> {
                idleEngine.targetState.animationName = "Lie_Down"
                handler.postDelayed({
                    idleEngine.targetState.animationName = "Lie_Idle"
                    handler.postDelayed({
                        idleEngine.targetState.animationName = "Lie_StandUp"
                        handler.postDelayed({ wanderEngine.scheduleWander() }, 2500)
                    }, Random.nextLong(15000, 40000))
                }, 2500)
            }
            else -> {
                // Play standard looping animation
                idleEngine.targetState.animationName = animName
                // Hold it for a random duration, then walk again
                handler.postDelayed({
                    idleEngine.targetState.animationName = "Idle_A"
                    wanderEngine.scheduleWander()
                }, Random.nextLong(5000, 15000))
            }
        }
    }
}
