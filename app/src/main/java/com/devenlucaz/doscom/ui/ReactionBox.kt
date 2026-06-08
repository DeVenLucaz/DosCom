package com.devenlucaz.doscom.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import com.devenlucaz.doscom.personality.EmotionalMemory
import com.devenlucaz.doscom.brain.BrainManager
import com.devenlucaz.doscom.brain.BrainInput

class ReactionBox(
    private val context: Context,
    private val windowManager: WindowManager,
    private val dosComX: Int,
    private val dosComY: Int,
    private val onChatClicked: () -> Unit,
    private val onReactedPositive: () -> Unit,
    private val onReactedNegative: () -> Unit
) {
    private var view: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable { dismiss() }

    fun show() {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#991A1A2E")) // Glass style
                cornerRadius = 32f
                setStroke(2, Color.parseColor("#40FFFFFF"))
            }
        }

        val reactions = listOf("♥", "😄", "👍", "😤", "💬")
        
        for (r in reactions) {
            val btn = Button(context).apply {
                text = r
                textSize = 24f
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnClickListener {
                    handleReaction(r)
                }
            }
            layout.addView(btn)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dosComY - 200
        }

        layout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismiss()
                true
            } else {
                false
            }
        }

        windowManager.addView(layout, params)
        view = layout

        handler.postDelayed(dismissRunnable, 5000)
    }

    private fun handleReaction(reaction: String) {
        val inputs = BrainInput.buildInputs(context)
        val targetOutputs = IntArray(7) 

        if (reaction == "💬") {
            android.widget.Toast.makeText(context, "chat", android.widget.Toast.LENGTH_SHORT).show()
            dismiss()
            onChatClicked()
            return
        }

        if (reaction == "😤") {
            android.widget.Toast.makeText(context, "negative", android.widget.Toast.LENGTH_SHORT).show()
            EmotionalMemory.recordNegative(context)
            BrainManager.brain.learn(inputs, targetOutputs, reward = -0.5f)
            onReactedNegative()
        } else {
            android.widget.Toast.makeText(context, "positive", android.widget.Toast.LENGTH_SHORT).show()
            EmotionalMemory.recordPositive(context)
            BrainManager.brain.learn(inputs, targetOutputs, reward = 1.0f)
            onReactedPositive()
        }
        
        BrainManager.brain.save(context)
        dismiss()
    }

    private fun dismiss() {
        view?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            view = null
        }
        handler.removeCallbacks(dismissRunnable)
    }
}
