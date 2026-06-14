package com.devenlucaz.doscom.character

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
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
        set(value) {
            field = value
            invalidate()
        }
        
    var antennaColor: Int = Color.WHITE

    private val zzzTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4000E5FF")
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    val webView: WebView = WebView(context)

    init {
        setWillNotDraw(false)
        setupWebView()
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // Fix 1: Instantly intercept ALL touches. The WebView gets ZERO touches.
        // This flawlessly restores Drag, Double-Tap, and Triple-Tap to the OverlayService!
        return true
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        // Fix 3: Restore the cute Zzz sleep particles on a transparent canvas over the 3D model!
        zzzParticles.forEach { p ->
            zzzTextPaint.alpha = p.alpha
            canvas.drawText("Z", p.x, p.y, zzzTextPaint)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.setLayerType(LAYER_TYPE_HARDWARE, null)
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
        }

        webView.webChromeClient = object : WebChromeClient() {}
        
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

        webView.loadUrl("https://appassets.local/companion.html")
    }

    private fun applyColors() {
        val bodyColor = "#0A0E1A"
        val eyeColor = "#00FFFF"
        webView.evaluateJavascript("window.applyColors('$bodyColor', '$eyeColor');", null)
    }

    private fun update3DModelState() {
        val targetScale = state.scale * 1.5f 
        val flipX = if (state.scaleX < 0) -1f else 1f
        val scaleX = targetScale * flipX
        val scaleY = targetScale
        val rotation = state.bodyRotation
        val animationName = state.animationName
        
        webView.evaluateJavascript("window.updateTransforms($scaleX, $scaleY, $rotation); window.updateAnimation('$animationName');", null)
    }
}
