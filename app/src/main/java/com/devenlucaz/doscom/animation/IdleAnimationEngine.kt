package com.devenlucaz.doscom.animation

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import com.devenlucaz.doscom.character.AnimationState
import com.devenlucaz.doscom.character.AnimationQueue
import kotlin.random.Random

import android.content.Context

class IdleAnimationEngine(
    private val context: Context,
    private val queue: AnimationQueue,
    private val onUpdateState: (AnimationState) -> Unit,
    private val onDrawZzz: (List<ZzzParticle>) -> Unit
) : Choreographer.FrameCallback {
    
    var animSpeedMultiplier: Float = 1f
    var sleepTimerMs: Long = 5 * 60 * 1000L
    
    private var frameCount = 0L
    private var lastInteractionTime = System.currentTimeMillis()
    var isSleeping = false
    
    val currentState = AnimationState()
    val targetState = AnimationState()
    
    private val handler = Handler(Looper.getMainLooper())
    
    fun start() {
        Choreographer.getInstance().postFrameCallback(this)
        scheduleNextSubAnimation(Random.nextLong(15000, 30000))
    }
    
    fun stop() {
        Choreographer.getInstance().removeFrameCallback(this)
        handler.removeCallbacksAndMessages(null)
    }
    
    fun interact() {
        lastInteractionTime = System.currentTimeMillis()
        if (isSleeping) {
            wakeUp()
        }
    }
    
    override fun doFrame(frameTimeNanos: Long) {
        frameCount++
        
        var floatOffset = 0f
        var glowOffset = 0f
        if (!isSleeping) {
            floatOffset = (Math.sin(frameCount * 0.05) * 4.0).toFloat()
            glowOffset = (Math.sin(frameCount * 0.03) * 0.4).toFloat()
            
            if (Random.nextInt(100) < 2) {
                targetState.eyesClosed = true
                handler.postDelayed({ targetState.eyesClosed = false }, 150)
            }
        }
        
        val elapsed = System.currentTimeMillis() - lastInteractionTime
        if (elapsed > sleepTimerMs && !isSleeping) {
            fallAsleep()
        }
        
        val rate = 0.03f * animSpeedMultiplier
        lerpState(currentState, targetState, rate)
        
        val drawState = currentState.copy(
            bodyOffsetY = currentState.bodyOffsetY + floatOffset,
            antennaGlow = currentState.antennaGlow + glowOffset
        )
        onUpdateState(drawState)
        
        if (isSleeping) {
            updateZzzParticles()
        }
        
        Choreographer.getInstance().postFrameCallback(this)
    }
    
    private fun lerpState(current: AnimationState, target: AnimationState, rate: Float) {
        current.leftArmAngle += (target.leftArmAngle - current.leftArmAngle) * rate
        current.rightArmAngle += (target.rightArmAngle - current.rightArmAngle) * rate
        current.leftLegAngle += (target.leftLegAngle - current.leftLegAngle) * rate
        current.rightLegAngle += (target.rightLegAngle - current.rightLegAngle) * rate
        current.bodyOffsetY += (target.bodyOffsetY - current.bodyOffsetY) * rate
        current.bodyOffsetX += (target.bodyOffsetX - current.bodyOffsetX) * rate
        current.bodyRotation += (target.bodyRotation - current.bodyRotation) * rate
        current.bodyRotationY = target.bodyRotationY
        
        current.scaleX = target.scaleX
        current.eyesClosed = target.eyesClosed
        current.eyesHalf = target.eyesHalf
        current.eyesWide = target.eyesWide
        current.pupilOffsetX = target.pupilOffsetX
        current.pupilOffsetY = target.pupilOffsetY
        current.mouthExpression = target.mouthExpression
        current.mouthOpen = target.mouthOpen
        current.blushVisible = target.blushVisible
        current.tongueOut = target.tongueOut
        current.antennaGlow = target.antennaGlow
        current.scale = target.scale
        current.activeProp = target.activeProp
        current.animationName = target.animationName
        current.animationPlayOnce = target.animationPlayOnce
    }

    private val subAnimRunnable = Runnable {
        // Safety fallback: if we've been idle too long, the RoutineEngine callback
        // chain may have broken. Trigger a gentle nudge.
        if (!isSleeping && targetState.animationName == "Idle_A") {
            android.util.Log.w("IdleAnimationEngine", "Safety fallback: forcing wander")
            targetState.animationName = "Idle_B" // Brief variety
            handler.postDelayed({ targetState.animationName = "Idle_A" }, 3000)
        }
        scheduleNextSubAnimation(60000L) // Check again in 60s
    }

    private fun scheduleNextSubAnimation(delayMs: Long) {
        handler.removeCallbacks(subAnimRunnable)
        handler.postDelayed(subAnimRunnable, delayMs)
    }

    private fun playRandomSubAnimation() {
        // Disabled
    }
    
    fun playStretch() {
        queue.enqueue("STRETCH", AnimationQueue.PRIORITY_LOW, 1500)
        targetState.leftArmAngle = -160f
        targetState.rightArmAngle = -160f
        targetState.bodyOffsetY = -6f
        targetState.eyesHalf = true
        handler.postDelayed({
            targetState.leftArmAngle = 0f
            targetState.rightArmAngle = 0f
            targetState.bodyOffsetY = 0f
            targetState.eyesHalf = false
        }, 1500)
    }
    
    private fun playSneeze() {
        queue.enqueue("SNEEZE", AnimationQueue.PRIORITY_LOW, 2000)
        targetState.bodyOffsetY = -4f
        targetState.eyesClosed = true
        handler.postDelayed({
            targetState.bodyOffsetY = 8f
            targetState.leftArmAngle = -90f
            targetState.rightArmAngle = -90f
            targetState.blushVisible = true
            handler.postDelayed({
                targetState.bodyOffsetY = 0f
                targetState.leftArmAngle = 0f
                targetState.rightArmAngle = 0f
                targetState.eyesClosed = false
                targetState.blushVisible = false
            }, 500)
        }, 1000)
    }

    private fun playHiccup() {
        queue.enqueue("HICCUP", AnimationQueue.PRIORITY_LOW, 1500)
        targetState.bodyOffsetY = -5f
        handler.postDelayed({
            targetState.bodyOffsetY = 0f
            handler.postDelayed({
                targetState.bodyOffsetY = -5f
                targetState.leftArmAngle = -45f
                targetState.rightArmAngle = -45f
                handler.postDelayed({
                    targetState.bodyOffsetY = 0f
                    targetState.leftArmAngle = 0f
                    targetState.rightArmAngle = 0f
                }, 300)
            }, 500)
        }, 300)
    }

    private fun playYawn() {
        queue.enqueue("YAWN", AnimationQueue.PRIORITY_LOW, 2000)
        targetState.mouthOpen = true
        targetState.leftArmAngle = -90f
        targetState.rightArmAngle = 90f
        targetState.eyesHalf = true
        handler.postDelayed({
            targetState.mouthOpen = false
            targetState.leftArmAngle = 0f
            targetState.rightArmAngle = 0f
            targetState.eyesHalf = false
        }, 2000)
    }

    private fun playCoin() {
        queue.enqueue("COIN", AnimationQueue.PRIORITY_LOW, 1500)
        targetState.rightArmAngle = -90f
        handler.postDelayed({
            targetState.rightArmAngle = -135f
            targetState.pupilOffsetY = -10f
            targetState.mouthExpression = listOf(1, 2).random()
            handler.postDelayed({
                targetState.rightArmAngle = 0f
                targetState.pupilOffsetY = 0f
                targetState.mouthExpression = 0
            }, 800)
        }, 300)
    }

    private fun playPhoneCheck() {
        queue.enqueue("PHONE_CHECK", AnimationQueue.PRIORITY_LOW, 1800)
        targetState.leftArmAngle = -45f
        targetState.rightArmAngle = -45f
        targetState.eyesHalf = true
        handler.postDelayed({
            targetState.mouthExpression = 1
            targetState.bodyRotation = 5f
            handler.postDelayed({
                targetState.bodyRotation = -5f
                handler.postDelayed({
                    targetState.bodyRotation = 0f
                    targetState.leftArmAngle = 0f
                    targetState.rightArmAngle = 0f
                    targetState.eyesHalf = false
                    targetState.mouthExpression = 0
                }, 400)
            }, 400)
        }, 500)
    }

    private fun fallAsleep() {
        isSleeping = true
        targetState.animationName = "Lie_Down"
        targetState.animationPlayOnce = true
        handler.postDelayed({
            targetState.animationName = "Lie_Idle"
            targetState.animationPlayOnce = false
        }, 2500)
    }
    
    fun wakeUp() {
        if (!isSleeping) return
        isSleeping = false
        lastInteractionTime = System.currentTimeMillis()
        targetState.animationName = "Lie_StandUp"
        targetState.animationPlayOnce = true
        handler.postDelayed({
            targetState.animationName = "Idle_A"
            targetState.animationPlayOnce = false
        }, 2500)
    }
    
    val particles = mutableListOf<ZzzParticle>()
    
    private fun updateZzzParticles() {
        if (particles.size < 3 && Random.nextFloat() < 0.05f) {
            particles.add(ZzzParticle(0f, 0f, 255))
        }
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.y -= 1.5f
            p.alpha -= 2
            if (p.alpha <= 0) {
                it.remove()
            }
        }
        onDrawZzz(particles)
    }
}

data class ZzzParticle(var x: Float, var y: Float, var alpha: Int)
