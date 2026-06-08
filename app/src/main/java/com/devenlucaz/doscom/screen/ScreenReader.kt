package com.devenlucaz.doscom.screen

import android.content.Context
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import com.devenlucaz.doscom.api.GeminiVisionClient

import android.accessibilityservice.AccessibilityService
import android.util.Log

object ScreenReader {
    private const val TAG = "DosComScreenReader"

    data class TargetResult(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val explanation: String,
        val source: String
    )

    suspend fun findTarget(
        context: Context,
        query: String,
        accessibilityService: AccessibilityService?,
        screenshot: Bitmap?,
        characterSizePx: Int
    ): TargetResult? {
        // Layer 1: Fast Accessibility Tree Scan
        val keywords = KeywordExtractor.extractKeywords(query).second
        if (keywords.isNotBlank() && accessibilityService != null) {
            var foundNode: AccessibilityNodeInfo? = null
            
            // First check all windows since homescreens are often not the "active" window
            val windows = accessibilityService.windows
            Log.d(TAG, "Scanning ${windows.size} windows for keyword: '$keywords'")
            
            for (window in windows) {
                val rootNode = window.root
                if (rootNode != null) {
                    foundNode = AccessibilityScanner.findNodeByText(rootNode, keywords)
                    if (foundNode != null) {
                        Log.d(TAG, "Found target in window: ${window.title ?: window.javaClass.simpleName}")
                        break
                    }
                }
            }
            
            // Fallback to active window if not found in windows list
            if (foundNode == null) {
                val rootNode = accessibilityService.rootInActiveWindow
                if (rootNode != null) {
                    Log.d(TAG, "Scanning fallback rootInActiveWindow")
                    foundNode = AccessibilityScanner.findNodeByText(rootNode, keywords)
                }
            }

            // Fallback 2: Check common package names mapping
            if (foundNode == null) {
                val targetPackages = AccessibilityScanner.APP_PACKAGE_MAP[keywords.lowercase()]
                if (targetPackages != null) {
                    Log.d(TAG, "Falling back to package name search for: $targetPackages")
                    for (window in windows) {
                        val rootNode = window.root
                        if (rootNode != null) {
                            foundNode = AccessibilityScanner.findNodeByPackageName(rootNode, targetPackages)
                            if (foundNode != null) {
                                break
                            }
                        }
                    }
                    if (foundNode == null) {
                        val rootNode = accessibilityService.rootInActiveWindow
                        if (rootNode != null) {
                            foundNode = AccessibilityScanner.findNodeByPackageName(rootNode, targetPackages)
                        }
                    }
                }
            }

            if (foundNode != null) {
                val rect = android.graphics.Rect()
                foundNode.getBoundsInScreen(rect)
                return TargetResult(
                    x = rect.centerX(),
                    y = rect.centerY(),
                    width = rect.width(),
                    height = rect.height(),
                    explanation = "I found exactly what you're looking for natively.",
                    source = "Accessibility"
                )
            } else {
                Log.d(TAG, "Accessibility scan found no matches for '$keywords'")
            }
        }

        // Layer 2: Fallback to Gemini Vision API
        if (screenshot != null && GeminiVisionClient.isConfigured()) {
            val visionResult = GeminiVisionClient.analyze(screenshot, query)
            if (visionResult != null && visionResult.found) {
                val normX = if (visionResult.xPercent > 1f) visionResult.xPercent / 100f else visionResult.xPercent
                val normY = if (visionResult.yPercent > 1f) visionResult.yPercent / 100f else visionResult.yPercent
                val rawX = (com.devenlucaz.doscom.utils.ScreenMetrics.getScreenWidth(context) * normX).toInt()
                val rawY = (com.devenlucaz.doscom.utils.ScreenMetrics.getScreenHeight(context) * normY).toInt()

                return TargetResult(
                    x = rawX,
                    y = rawY,
                    width = 0,
                    height = 0,
                    explanation = visionResult.explanation,
                    source = "GeminiVision"
                )
            }
        }

        return null
    }

    fun buildScreenContext(service: AccessibilityService?): String {
        if (service == null) return ""
        val rootNode = service.rootInActiveWindow ?: return ""
        val texts = mutableListOf<String>()
        val pkgName = rootNode.packageName?.toString() ?: "Unknown"

        fun traverse(node: android.view.accessibility.AccessibilityNodeInfo?) {
            if (node == null) return
            val text = node.text?.toString() ?: node.contentDescription?.toString()
            if (!text.isNullOrBlank()) {
                texts.add(text)
            }
            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }
        traverse(rootNode)
        
        var contextStr = "App: $pkgName, Visible: ${texts.joinToString(", ")}"
        if (contextStr.length > 500) {
            contextStr = contextStr.substring(0, 497) + "..."
        }
        return contextStr
    }
}
