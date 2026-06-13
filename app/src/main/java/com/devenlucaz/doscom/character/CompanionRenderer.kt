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
        // WebGL requires HARDWARE acceleration to render 3D models.
        // SOFTWARE layer disables WebGL entirely, leaving the WebView blank.
        webView.setLayerType(LAYER_TYPE_HARDWARE, null)
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            // CRITICAL FOR OFFLINE WEBGL: Allow fetching .js modules and .glb files from file:///
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                android.util.Log.e("WebViewCompanion", "${message?.message()} -- From line ${message?.lineNumber()} of ${message?.sourceId()}")
                return true
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): android.webkit.WebResourceResponse? {
                val urlStr = request.url.toString()
                if (urlStr.startsWith("https://appassets.local/")) {
                    val filename = urlStr.removePrefix("https://appassets.local/")
                    val mime = when {
                        filename.endsWith(".html") -> "text/html"
                        filename.endsWith(".js") -> "application/javascript"
                        filename.endsWith(".glb") -> "model/gltf-binary"
                        else -> "application/octet-stream"
                    }
                    return try {
                        val stream = context.assets.open(filename)
                        android.webkit.WebResourceResponse(mime, "UTF-8", stream).apply {
                            responseHeaders = mapOf("Access-Control-Allow-Origin" to "*")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("WebViewCompanion", "Failed to load asset: $filename", e)
                        null
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                applyColors()
                update3DModelState()
            }
        }

        // Load via the virtual HTTPS domain to fully bypass Chromium file:// ES Module restrictions!
        webView.loadUrl("https://appassets.local/companion.html")
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
        val scaleX = targetScale * flipX
        val scaleY = targetScale
        
        val rotation = state.bodyRotation
        
        // Execute JS to update transforms in <model-viewer>
        webView.evaluateJavascript("window.updateTransforms($scaleX, $scaleY, $rotation);", null)
    }
}
