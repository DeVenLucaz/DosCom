package com.devenlucaz.doscom.animation

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import kotlin.random.Random
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

class WanderEngine(
    private val getX: () -> Int,
    private val getY: () -> Int,
    private val setPos: (Int, Int) -> Unit,
    private val getBounds: () -> Pair<Int, Int>, // screenWidth, floorY
    private val setAnimation: (String) -> Unit,
    private val setRotationY: (Float) -> Unit
) : Choreographer.FrameCallback {

    private val handler = Handler(Looper.getMainLooper())
    private var isWandering = false
    private var targetX = -1
    private var targetY = -1
    private var speed = 3f

    fun start() {
        Choreographer.getInstance().postFrameCallback(this)
        scheduleWander()
    }

    fun stop() {
        Choreographer.getInstance().removeFrameCallback(this)
        handler.removeCallbacksAndMessages(null)
        isWandering = false
    }

    private fun scheduleWander() {
        handler.postDelayed({
            pickDestination()
        }, Random.nextLong(5000L, 15000L))
    }

    private fun pickDestination() {
        // Simple locomotion: pick a random spot along the floor and walk/run there.
        val bounds = getBounds()
        val curX = getX()
        val curY = getY()
        
        targetX = Random.nextInt(0, max(1, bounds.first - 200))
        targetY = bounds.second // Exact floor level passed from OverlayService
        
        speed = if (Random.nextBoolean()) 2f else 5f // walk or run
        val anim = if (speed > 3f) "Running_A" else "Walking_A"
        
        setAnimation(anim)
        isWandering = true
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (isWandering && targetX != -1) {
            val curX = getX()
            val curY = getY()
            
            val dx = targetX - curX
            val dy = targetY - curY
            
            if (Math.abs(dx) < speed && Math.abs(dy) < speed) {
                // Reached destination
                isWandering = false
                setAnimation("Idle_A")
                setRotationY(0f) // Face forward again
                scheduleWander()
            } else {
                val newX = curX + (sign(dx.toFloat()) * speed).toInt()
                val newY = curY + (sign(dy.toFloat()) * speed).toInt()
                setPos(newX, newY)
                setRotationY(if (dx < 0) 270f else 90f) // Face left or right
            }
        }
        Choreographer.getInstance().postFrameCallback(this)
    }
}
