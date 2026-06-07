package com.devenlucaz.doscom.animation

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import com.devenlucaz.doscom.character.AnimationState
import com.devenlucaz.doscom.character.AnimationQueue
import kotlin.random.Random

class IdleAnimationEngine(
    private val queue: AnimationQueue,
    private val onUpdateState: (AnimationState) -> Unit,
    private val onDrawZzz: (List<ZzzParticle>) -> Unit
) : Choreographer.FrameCallback {
    
    var animSpeedMultiplier: Float = 1f
    var sleepTimerMs: Long = 5 * 60 * 1000L
    
    private var frameCount = 0L
    private var lastInteractionTime = System.currentTimeMillis()
    private var isSleeping = false
    
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
        
        if (!isSleeping) {
            targetState.bodyOffsetY = (Math.sin(frameCount * 0.05) * 4.0).toFloat()
            targetState.antennaGlow = 0.6f + (Math.sin(frameCount * 0.03) * 0.4).toFloat()
            
            if (Random.nextInt(100) < 2) {
                targetState.eyesClosed = true
                handler.postDelayed({ targetState.eyesClosed = false }, 150)
            }
        }
        
        val elapsed = System.currentTimeMillis() - lastInteractionTime
        if (elapsed > sleepTimerMs && !isSleeping) {
            fallAsleep()
        }
        
        val rate = 0.08f * animSpeedMultiplier
        lerpState(currentState, targetState, rate)
        
        onUpdateState(currentState)
        
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
    }

    private val subAnimRunnable = Runnable {
        if (queue.currentPriority() <= AnimationQueue.PRIORITY_LOW && !isSleeping) {
            playRandomSubAnimation()
        }
        scheduleNextSubAnimation(Random.nextLong(20000, 45000))
    }
    
    private fun scheduleNextSubAnimation(delayMs: Long) {
        handler.removeCallbacks(subAnimRunnable)
        handler.postDelayed(subAnimRunnable, delayMs)
    }
    
    private fun playRandomSubAnimation() {
        if (Random.nextInt(100) < 30) {
            val toy = com.devenlucaz.doscom.systems.ToyBoxSystem.selectToy()
            com.devenlucaz.doscom.systems.ToyBoxSystem.startToyActivity(toy, this)
        } else {
            val anims = listOf(::playStretch, ::playSneeze, ::playHiccup, ::playYawn, ::playCoin, ::playPhoneCheck)
            anims.random().invoke()
        }
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
        queue.enqueue("PRE_SLEEP", AnimationQueue.PRIORITY_MEDIUM, 3000)
        targetState.mouthOpen = true
        targetState.eyesHalf = true
        handler.postDelayed({
            targetState.mouthOpen = false
            targetState.eyesClosed = true
            targetState.bodyRotation = 10f 
            targetState.leftLegAngle = -45f
            targetState.rightLegAngle = -45f
            targetState.bodyOffsetY = 15f
        }, 3000)
    }
    
    private fun wakeUp() {
        isSleeping = false
        targetState.eyesClosed = false
        targetState.eyesWide = true
        targetState.bodyRotation = 0f
        targetState.leftLegAngle = 0f
        targetState.rightLegAngle = 0f
        targetState.bodyOffsetY = 0f
        targetState.eyesHalf = false
        queue.enqueue("WAKE", AnimationQueue.PRIORITY_HIGH, 1500)
        playStretch()
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
