package com.devenlucaz.doscom.systems

import android.os.Handler
import android.os.Looper
import com.devenlucaz.doscom.character.PropType
import com.devenlucaz.doscom.animation.IdleAnimationEngine
import kotlin.random.Random

import android.content.Context
import com.devenlucaz.doscom.brain.BrainInput
import com.devenlucaz.doscom.brain.BrainManager

object ToyBoxSystem {
    fun selectToy(context: Context): PropType {
        // Pick a toy at random — brain influence is handled by RoutineEngine now
        val r = Random.nextInt(100)
        return when {
            r < 20 -> PropType.FISHING_ROD
            r < 35 -> PropType.TREASURE_MAP
            r < 55 -> PropType.MAGNIFYING_GLASS
            r < 65 -> PropType.CARDBOARD_SWORD
            r < 80 -> PropType.BINOCULARS
            else -> PropType.BOOK
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    fun startToyActivity(toy: PropType, engine: IdleAnimationEngine) {
        when (toy) {
            PropType.BOOK -> {
                engine.targetState.activeProp = PropType.OVERSIZED_GLASSES
                handler.postDelayed({
                    engine.targetState.activeProp = PropType.BOOK
                    engine.targetState.pupilOffsetX = -5f
                    handler.postDelayed({
                        engine.targetState.pupilOffsetX = 5f
                        handler.postDelayed({
                            if (Random.nextBoolean()) {
                                engine.targetState.eyesHalf = true
                            } else {
                                engine.targetState.activeProp = PropType.NONE
                            }
                        }, 2000)
                    }, 1000)
                }, 500)
            }
            PropType.MAGNIFYING_GLASS -> {
                engine.targetState.activeProp = PropType.MAGNIFYING_GLASS
                DiscoverySystem.triggerDiscovery(engine) { reacted -> }
            }
            PropType.TREASURE_MAP -> {
                engine.targetState.activeProp = PropType.TREASURE_MAP
                engine.targetState.bodyRotation = 10f
                handler.postDelayed({
                    engine.targetState.bodyRotation = -10f
                    handler.postDelayed({
                        engine.targetState.bodyRotation = 0f
                        engine.targetState.bodyOffsetY = -5f // digging
                    }, 2000)
                }, 2000)
            }
            PropType.FISHING_ROD -> {
                engine.targetState.activeProp = PropType.FISHING_ROD
                engine.targetState.leftLegAngle = -90f
                engine.targetState.rightLegAngle = -90f
                handler.postDelayed({
                    if (Random.nextFloat() < 0.2f) {
                        engine.targetState.bodyOffsetY = -10f // nibble
                        handler.postDelayed({ engine.targetState.bodyOffsetY = 0f }, 300)
                    }
                }, 3000)
            }
            else -> {
                engine.targetState.activeProp = toy
                handler.postDelayed({ engine.targetState.activeProp = PropType.NONE }, 5000)
            }
        }
    }
}
