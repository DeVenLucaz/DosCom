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

    // Animation clusters mapped to brain outputs 0-14
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
     * Called when the user taps/drags the character during an activity.
     * Sends negative reward so the brain learns to avoid the interrupted activity.
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
        
        val useBrain = confidence > 15f
        
        val category: Int
        if (useBrain) {
            val roll = Random.nextFloat()
            category = when {
                roll < 0.5f -> 0  // 50% chill
                roll < 0.8f -> 1  // 30% work
                else -> 2         // 20% magic
            }
        } else {
            category = Random.nextInt(3)
        }
        
        var animId: Int
        var chosenAnimation: String
        var clusterId: Int
        
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

        // Store for potential negative reinforcement
        val targetOutputs = intArrayOf(
            if (clusterId == 0) animId else 0,
            if (clusterId == 1) animId + 6 else 6,
            if (clusterId == 2) animId + 11 else 11
        )
        lastInputs = inputs.clone()
        lastTargetOutputs = targetOutputs.clone()

        // Delayed positive reward — cancelled if user interrupts
        handler.postDelayed({
            if (lastInputs != null) {
                BrainManager.brain.learn(inputs, targetOutputs, reward = 0.3f)
                BrainManager.brain.save(context)
                lastInputs = null
                lastTargetOutputs = null
            }
        }, 8000)
        
        executeActivity(chosenAnimation, wanderEngine, idleEngine)
    }

    // ---------- Helper: set animation with proper loop/once mode ----------

    private fun setAnim(engine: IdleAnimationEngine, name: String, once: Boolean) {
        engine.targetState.animationName = name
        engine.targetState.animationPlayOnce = once
    }

    private fun idleThenWander(wanderEngine: WanderEngine, idleEngine: IdleAnimationEngine) {
        // Return to Idle_A and breathe for 30-120 seconds before next wander
        setAnim(idleEngine, "Idle_A", false)
        val breatheMs = Random.nextLong(30000, 120000)
        handler.postDelayed({
            wanderEngine.scheduleWander()
        }, breatheMs)
    }

    // ---------- Activity execution with proper timing ----------

    private fun executeActivity(animName: String, wanderEngine: WanderEngine, idleEngine: IdleAnimationEngine) {
        when (animName) {

            // ── SIT SEQUENCE ──
            "Sit_Floor_Down" -> {
                setAnim(idleEngine, "Sit_Floor_Down", true)         // one-shot: sit down
                handler.postDelayed({
                    setAnim(idleEngine, "Sit_Floor_Idle", false)    // loop: sit idle
                    val holdMs = Random.nextLong(60000, 180000)     // sit for 1-3 minutes
                    handler.postDelayed({
                        setAnim(idleEngine, "Sit_Floor_StandUp", true) // one-shot: stand up
                        handler.postDelayed({
                            idleThenWander(wanderEngine, idleEngine)
                        }, 2000)
                    }, holdMs)
                }, 2000)
            }

            // ── LIE DOWN / SLEEP SEQUENCE ──
            "Lie_Down" -> {
                setAnim(idleEngine, "Lie_Down", true)               // one-shot: lie down
                handler.postDelayed({
                    setAnim(idleEngine, "Lie_Idle", false)          // loop: lying idle
                    val holdMs = Random.nextLong(90000, 300000)     // sleep for 1.5-5 minutes
                    handler.postDelayed({
                        setAnim(idleEngine, "Lie_StandUp", true)   // one-shot: stand up
                        handler.postDelayed({
                            idleThenWander(wanderEngine, idleEngine)
                        }, 2500)
                    }, holdMs)
                }, 2500)
            }

            // ── EXERCISE (looping) ──
            "Push_Ups", "Sit_Ups" -> {
                setAnim(idleEngine, animName, false)                // loop: exercise
                val holdMs = Random.nextLong(15000, 40000)          // exercise for 15-40s
                handler.postDelayed({
                    idleThenWander(wanderEngine, idleEngine)
                }, holdMs)
            }

            // ── SOCIAL (one-shot) ──
            "Waving", "Cheering" -> {
                setAnim(idleEngine, animName, true)                 // one-shot: wave/cheer
                handler.postDelayed({
                    idleThenWander(wanderEngine, idleEngine)
                }, 4000) // Hold final pose for ~4s
            }

            // ── WORK (looping) ──
            "Working_A", "Working_B", "Pickaxing", "Hammering", "Digging" -> {
                setAnim(idleEngine, animName, false)                // loop: work
                val holdMs = Random.nextLong(20000, 60000)          // work for 20-60s
                handler.postDelayed({
                    idleThenWander(wanderEngine, idleEngine)
                }, holdMs)
            }

            // ── MAGIC / COMBAT (one-shot) ──
            "Ranged_Magic_Spellcasting", "Throw", "Melee_Unarmed_Attack_Punch_A", "Dodge_Backward" -> {
                setAnim(idleEngine, animName, true)                 // one-shot: magic/combat
                handler.postDelayed({
                    idleThenWander(wanderEngine, idleEngine)
                }, 4000) // Hold final pose for ~4s
            }

            // ── FALLBACK ──
            else -> {
                setAnim(idleEngine, animName, false)
                handler.postDelayed({
                    idleThenWander(wanderEngine, idleEngine)
                }, Random.nextLong(10000, 30000))
            }
        }
    }
}
