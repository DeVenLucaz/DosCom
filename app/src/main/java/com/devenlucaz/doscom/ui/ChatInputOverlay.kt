package com.devenlucaz.doscom.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout

class ChatInputOverlay(
    context: Context,
    private val windowManager: WindowManager,
    private val screenshot: Bitmap?,
    private val onQuerySubmitted: (String, Bitmap?) -> Unit,
    private val onClose: () -> Unit,
    private val onVoiceStart: () -> Unit,
    private val onVoiceError: () -> Unit
) : FrameLayout(context) {

    private val inputField: EditText
    private val voiceInputService = com.devenlucaz.doscom.service.VoiceInputService(context)

    init {
        val density = resources.displayMetrics.density
        val dp = { value: Int -> (value * density).toInt() }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
            }
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
                setMargins(dp(16), dp(16), dp(16), dp(16))
            }
            elevation = dp(8).toFloat()
        }

        val closeButton = Button(context).apply {
            text = "X"
            setTextColor(Color.GRAY)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { dismiss(false) }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        }

        inputField = EditText(context).apply {
            hint = "Ask DosCom..."
            setTextColor(Color.BLACK)
            setHintTextColor(Color.LTGRAY)
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setBackgroundColor(Color.TRANSPARENT)
            requestFocus()
        }

        val sendButton = Button(context).apply {
            text = "Send"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF6200EE"))
                cornerRadius = dp(8).toFloat()
            }
            setOnClickListener {
                val query = inputField.text.toString()
                if (query.isNotBlank()) {
                    dismiss(true, query)
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(40)
            ).apply {
                marginStart = dp(8)
            }
        }

        val micButton = Button(context).apply {
            text = "Mic"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF00BCD4"))
                cornerRadius = dp(8).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(40)
            ).apply {
                marginStart = dp(8)
            }
            setOnClickListener {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(inputField.windowToken, 0)
                
                onVoiceStart()
                
                voiceInputService.startListening(
                    onResult = { text ->
                        inputField.setText(text)
                        postDelayed({
                            val query = inputField.text.toString()
                            if (query.isNotBlank()) {
                                dismiss(true, query)
                            }
                        }, 800)
                    },
                    onError = {
                        onVoiceError()
                    }
                )
            }
        }

        container.addView(closeButton)
        container.addView(inputField)
        if (voiceInputService.isAvailable()) {
            container.addView(micButton)
        }
        container.addView(sendButton)

        addView(container)

        // Slide up animation
        post {
            val showAnimator = ObjectAnimator.ofFloat(container, "translationY", container.height.toFloat() + dp(32), 0f)
            showAnimator.duration = 250
            showAnimator.start()

            // Request keyboard
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun dismiss(submitted: Boolean, query: String = "") {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(inputField.windowToken, 0)

        val container = getChildAt(0)
        val hideAnimator = ObjectAnimator.ofFloat(container, "translationY", 0f, container.height.toFloat() + (32 * resources.displayMetrics.density))
        hideAnimator.duration = 250
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
        }, 250)
    }
}
