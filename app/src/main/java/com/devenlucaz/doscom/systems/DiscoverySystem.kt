package com.devenlucaz.doscom.systems

import android.os.Handler
import android.os.Looper
import com.devenlucaz.doscom.animation.IdleAnimationEngine
import kotlin.random.Random

data class Discovery(val name: String, val emoji: String)

object DiscoverySystem {
    val possibleDiscoveries = listOf(
        Discovery("Ancient Fossil", "🦴"),
        Discovery("Pirate Coin", "🪙"),
        Discovery("Strange Rock", "🪨"),
        Discovery("Mystery Artifact", "🏺"),
        Discovery("Tiny Bug", "🐛"),
        Discovery("Shiny Gem", "💎")
    )
    
    val collection = mutableListOf<Discovery>()
    private val handler = Handler(Looper.getMainLooper())

    fun triggerDiscovery(engine: IdleAnimationEngine, onUserReaction: (Boolean) -> Unit) {
        val discovery = possibleDiscoveries.random()
        
        engine.targetState.activeProp = com.devenlucaz.doscom.character.PropType.NONE
        engine.targetState.eyesWide = true
        engine.targetState.leftArmAngle = 0f
        engine.targetState.rightArmAngle = 0f
        
        handler.postDelayed({
            engine.targetState.bodyOffsetY = -15f
            engine.targetState.leftArmAngle = -160f
            engine.targetState.rightArmAngle = -160f
            
            handler.postDelayed({
                engine.targetState.bodyOffsetY = 0f
                var reacted = false
                
                // Usually we'd wait 4s and if !reacted, do disappointed animation
                handler.postDelayed({
                    if (!reacted) {
                        engine.targetState.eyesHalf = true
                        engine.targetState.bodyOffsetY = 5f
                        handler.postDelayed({
                            engine.targetState.eyesHalf = false
                            engine.targetState.bodyOffsetY = 0f
                        }, 2000)
                    }
                }, 4000)
            }, 1000)
        }, 500)
    }

    fun showCollection() {
        // Show items one by one
    }
}
