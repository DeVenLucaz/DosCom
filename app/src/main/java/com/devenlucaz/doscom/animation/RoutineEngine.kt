package com.devenlucaz.doscom.animation

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.devenlucaz.doscom.brain.BrainInput
import com.devenlucaz.doscom.brain.BrainManager
import kotlin.random.Random

object RoutineEngine {
    private val handler = Handler(Looper.getMainLooper())
    
    // Track the last activity for negative reinforcement on user interruption
    private var lastInputs: FloatArray? = null
    private var lastTargetOutputs: IntArray? = null

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

    /**
     * Called by the user tapping/dragging the character while it was doing an activity.
     * This sends negative reward to the brain so it learns to avoid the interrupted activity.
     */
    fun onUserInterrupted(context: Context) {
        val inputs = lastInputs ?: return
        val targets = lastTargetOutputs ?: return
        BrainManager.brain.learn(inputs, targets, reward = -0.5f)
        BrainManager.brain.save(context)
        lastInputs = null
        lastTargetOutputs = null
    }

    fun chooseNextActivity(context: Context, wanderEngine: WanderEngine, idleEngine: IdleAnimationEngine) {
        val inputs = BrainInput.buildInputs(context)
        val decisions = BrainManager.brain.think(inputs)
        val confidence = BrainManager.brain.lastConfidence
        
        // Confidence threshold: as the brain trains more, its scores grow.
        // A fresh brain has near-zero confidence → full random mode.
        val useBrain = confidence > 15f
        
        // Category selection: brain picks the strongest cluster, random picks any
        val category: Int
        var animId: Int
        var chosenAnimation: String
        var clusterId: Int
        
        if (useBrain) {
            // Compare the peak scores across the 3 clusters to pick the dominant one
            val idleScore = decisions[0].toFloat()   // 0..5 index
            val workScore = (decisions[1] - 6).toFloat()  // normalized
            val magicScore = (decisions[2] - 11).toFloat() // normalized
            
            // Use the raw output index values as a proxy for cluster strength
            // The brain's decision already reflects which cluster fires hardest
            val roll = Random.nextFloat()
            category = when {
                roll < 0.5f -> 0  // 50% chill
                roll < 0.8f -> 1  // 30% work
                else -> 2         // 20% magic
            }
        } else {
            // Fresh install: pure random
            category = Random.nextInt(3)
        }
        
        when (category) {
            0 -> {
                animId = if (useBrain) decisions[0] else Random.nextInt(6)
                chosenAnimation = chillAnimations[animId.coerceIn(0, 5)]
                clusterId = 0
            }
            1 -> {
                animId = if (useBrain) (decisions[1] - 6) else Random.nextInt(5)
                chosenAnimation = workAnimations[animId.coerceIn(0, 4)]
                clusterId = 1
            }
            else -> {
                animId = if (useBrain) (decisions[2] - 11) else Random.nextInt(4)
                chosenAnimation = magicAnimations[animId.coerceIn(0, 3)]
                clusterId = 2
            }
        }

        // Store for potential negative reinforcement if user interrupts
        val targetOutputs = intArrayOf(
            if (clusterId == 0) animId else 0,
            if (clusterId == 1) animId + 6 else 6,
            if (clusterId == 2) animId + 11 else 11
        )
        lastInputs = inputs.clone()
        lastTargetOutputs = targetOutputs.clone()

        // Positive reinforcement: small reward for completing the activity
        // (the real reward comes from NOT being interrupted)
        handler.postDelayed({
            if (lastInputs != null) {
                BrainManager.brain.learn(inputs, targetOutputs, reward = 0.3f)
                BrainManager.brain.save(context)
                lastInputs = null
                lastTargetOutputs = null
            }
        }, 8000) // Reward after 8 seconds if user didn't interrupt
        
        executeActivity(chosenAnimation, wanderEngine, idleEngine)
    }
    
    private fun executeActivity(animName: String, wanderEngine: WanderEngine, idleEngine: IdleAnimationEngine) {
        when (animName) {
            "Sit_Floor_Down" -> {
                idleEngine.targetState.animationName = "Sit_Floor_Down"
                handler.postDelayed({
                    idleEngine.targetState.animationName = "Sit_Floor_Idle"
                    handler.postDelayed({
                        idleEngine.targetState.animationName = "Sit_Floor_StandUp"
                        handler.postDelayed({ wanderEngine.scheduleWander() }, 2000)
                    }, Random.nextLong(10000, 30000))
                }, 2000)
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
                idleEngine.targetState.animationName = animName
                handler.postDelayed({
                    idleEngine.targetState.animationName = "Idle_A"
                    wanderEngine.scheduleWander()
                }, Random.nextLong(5000, 15000))
            }
        }
    }
}
