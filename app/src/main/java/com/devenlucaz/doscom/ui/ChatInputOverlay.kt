package com.devenlucaz.doscom.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.devenlucaz.doscom.personality.EmotionalMemory
import com.devenlucaz.doscom.brain.BrainManager
import com.devenlucaz.doscom.brain.BrainInput

class ChatInputOverlay(
    context: Context,
    private val windowManager: WindowManager,
    private val screenshot: Bitmap?,
    private val onQuerySubmitted: (String, Bitmap?) -> Unit,
    private val onClose: () -> Unit,
    private val onVoiceStart: () -> Unit,
    private val onVoiceError: () -> Unit,
    private val onReactedPositive: () -> Unit = {},
    private val onReactedNegative: () -> Unit = {}
) : FrameLayout(context) {

    private val inputField: EditText
    private val voiceInputService = com.devenlucaz.doscom.service.VoiceInputService(context)
    private val container: LinearLayout

    init {
        val density = resources.displayMetrics.density
        val dp = { value: Int -> (value * density).toInt() }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(Color.TRANSPARENT)

        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#BF1A1A2E")) // 75% opacity
                cornerRadius = dp(28).toFloat()
                setStroke(1, Color.parseColor("#26FFFFFF")) // 15% opacity
            }
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
                setMargins(dp(16), dp(16), dp(16), dp(16) + dp(48)) // Bottom margin
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        // Reaction Row
        val reactionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }

        val reactions = listOf("♥", "😄", "👍", "😤")
        for (r in reactions) {
            val btn = android.widget.Button(context).apply {
                text = r
                textSize = 24f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setPadding(0, dp(8), 0, dp(8))
                setOnClickListener {
                    handleReaction(r)
                }
            }
            reactionRow.addView(btn)
        }

        // Input Row
        val inputRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(40)
            )
        }

        val closeButton = CloseButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                marginEnd = dp(8)
            }
            setOnClickListener { dismiss(false) }
        }

        inputField = EditText(context).apply {
            hint = "Ask DosCom..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#66FFFFFF"))
            textSize = 14f
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            requestFocus()
        }

        val sendButton = SendButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                marginStart = dp(8)
            }
            setOnClickListener {
                val query = inputField.text.toString()
                if (query.isNotBlank()) {
                    dismiss(true, query)
                }
            }
        }

        val micButton = MicButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                marginStart = dp(8)
            }
            var isListening = false
            var pulseAnimator: ObjectAnimator? = null

            setOnClickListener {
                if (isListening) return@setOnClickListener

                isListening = true
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(inputField.windowToken, 0)
                
                onVoiceStart()
                inputField.hint = "Listening..."
                inputField.setText("")
                
                setBgColor(Color.RED)
                pulseAnimator = ObjectAnimator.ofFloat(this, "alpha", 1f, 0.3f, 1f).apply {
                    duration = 800
                    repeatCount = ObjectAnimator.INFINITE
                    start()
                }
                
                voiceInputService.startListening(
                    onPartial = { text ->
                        inputField.setText(text)
                        inputField.setSelection(text.length)
                    },
                    onFinal = { text ->
                        isListening = false
                        pulseAnimator?.cancel()
                        alpha = 1f
                        setBgColor(Color.parseColor("#C81A1A2E"))
                        inputField.hint = "Ask DosCom..."
                        inputField.setText(text)
                        inputField.setSelection(text.length)
                    },
                    onError = {
                        isListening = false
                        pulseAnimator?.cancel()
                        alpha = 1f
                        setBgColor(Color.parseColor("#C81A1A2E"))
                        inputField.hint = "Ask DosCom..."
                        onVoiceError()
                    }
                )
            }
        }

        inputRow.addView(closeButton)
        inputRow.addView(inputField)
        if (voiceInputService.isAvailable()) {
            inputRow.addView(micButton)
        }
        inputRow.addView(sendButton)

        container.addView(reactionRow)
        container.addView(inputRow)
        addView(container)

        // Slide in animation
        post {
            val showAnimator = ObjectAnimator.ofFloat(container, "translationY", container.height.toFloat() + dp(100), 0f)
            showAnimator.duration = 200
            showAnimator.interpolator = DecelerateInterpolator()
            showAnimator.start()

            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun handleReaction(reaction: String) {
        val inputs = BrainInput.buildInputs(context)
        val targetOutputs = IntArray(7) 

        if (reaction == "😤") {
            EmotionalMemory.recordNegative(context)
            BrainManager.brain.learn(inputs, targetOutputs, reward = -0.5f)
            onReactedNegative()
        } else {
            EmotionalMemory.recordPositive(context)
            BrainManager.brain.learn(inputs, targetOutputs, reward = 1.0f)
            onReactedPositive()
        }
        BrainManager.brain.save(context)
        
        dismiss(submitted = false)
    }

    private fun dismiss(submitted: Boolean, query: String = "", reactionCallback: (() -> Unit)? = null) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(inputField.windowToken, 0)

        val hideAnimator = ObjectAnimator.ofFloat(container, "translationY", 0f, container.height.toFloat() + (100 * resources.displayMetrics.density))
        hideAnimator.duration = 150
        hideAnimator.start()

        postDelayed({
            try {
                windowManager.removeView(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (submitted) {
                onQuerySubmitted(query, screenshot)
            } else {
                onClose()
            }
            reactionCallback?.invoke()
        }, 150)
    }

    class MicButton(context: Context) : View(context) {
        private var bgColor = Color.parseColor("#C81A1A2E")
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            color = Color.parseColor("#00B4FF")
            strokeWidth = 2f * resources.displayMetrics.density
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        
        fun setBgColor(color: Int) {
            bgColor = color
            bgPaint.color = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = width / 2f
            canvas.drawCircle(cx, cy, r, bgPaint)
            
            val p = Path()
            val d = resources.displayMetrics.density
            val rectW = 6f * d
            val rectH = 10f * d
            val baseCy = cy - 2f * d
            
            canvas.drawRoundRect(cx - rectW/2, baseCy - rectH/2, cx + rectW/2, baseCy + rectH/2, rectW/2, rectW/2, iconPaint)
            
            p.moveTo(cx - rectW/2 - 3f*d, baseCy + 2f*d)
            p.arcTo(cx - rectW/2 - 3f*d, baseCy - rectH/2 + 2f*d, cx + rectW/2 + 3f*d, baseCy + rectH/2 + 4f*d, 180f, -180f, false)
            p.moveTo(cx, baseCy + rectH/2 + 4f*d)
            p.lineTo(cx, baseCy + rectH/2 + 8f*d)
            canvas.drawPath(p, iconPaint)
        }
    }

    class SendButton(context: Context) : View(context) {
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C81A1A2E") }
        private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            color = Color.parseColor("#A855F7")
            strokeWidth = 2f * resources.displayMetrics.density
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = width / 2f
            canvas.drawCircle(cx, cy, r, bgPaint)
            
            val p = Path()
            val d = resources.displayMetrics.density
            p.moveTo(cx, cy + 6f*d)
            p.lineTo(cx, cy - 6f*d)
            p.moveTo(cx - 4f*d, cy - 2f*d)
            p.lineTo(cx, cy - 6f*d)
            p.lineTo(cx + 4f*d, cy - 2f*d)
            canvas.drawPath(p, iconPaint)
        }
    }

    class CloseButton(context: Context) : View(context) {
        private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            color = Color.parseColor("#99FFFFFF")
            strokeWidth = 2f * resources.displayMetrics.density
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        override fun onDraw(canvas: Canvas) {
            val d = resources.displayMetrics.density
            val padding = 6f * d
            canvas.drawLine(padding, padding, width - padding, height - padding, iconPaint)
            canvas.drawLine(width - padding, padding, padding, height - padding, iconPaint)
        }
    }
}
