package com.devenlucaz.doscom.character

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class CompanionRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var state = AnimationState()
        set(value) {
            field = value
            update3DModelState()
        }

    var zzzParticles = listOf<com.devenlucaz.doscom.animation.ZzzParticle>()
    var antennaColor: Int = Color.WHITE

    val webView: WebView = WebView(context)

    init {
        setupWebView()
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.setBackgroundColor(Color.TRANSPARENT)
        // Ensure transparency works on older Android versions by using software layer
        webView.setLayerType(LAYER_TYPE_SOFTWARE, null)
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                applyColors()
                update3DModelState()
            }
        }

        // Load the local HTML file containing <model-viewer>
        webView.loadUrl("file:///android_asset/companion.html")
    }

    private fun applyColors() {
        val bodyColor = "#0A0E1A"
        val eyeColor = "#00FFFF"
        
        // Execute JS to update materials in <model-viewer>
        webView.evaluateJavascript("window.applyColors('$bodyColor', '$eyeColor');", null)
    }

    private fun update3DModelState() {
        val targetScale = state.scale * 1.5f 
        val flipX = if (state.scaleX < 0) -1f else 1f
        val finalScale = targetScale * flipX
        
        val rotation = state.bodyRotation
        
        // Execute JS to update transforms in <model-viewer>
        webView.evaluateJavascript("window.updateTransforms($finalScale, $rotation);", null)
    }
}
