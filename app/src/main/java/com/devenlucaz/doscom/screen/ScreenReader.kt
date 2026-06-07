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
                val coords = AccessibilityScanner.getNodeCenterCoords(foundNode)
                val mapped = CoordinateMapper.fromNodeCoords(context, coords.first, coords.second, characterSizePx)
                return TargetResult(
                    x = mapped.first,
                    y = mapped.second,
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
                val mapped = CoordinateMapper.fromPercent(
                    context, 
                    visionResult.xPercent, 
                    visionResult.yPercent, 
                    characterSizePx
                )
                return TargetResult(
                    x = mapped.first,
                    y = mapped.second,
                    explanation = visionResult.explanation,
                    source = "GeminiVision"
                )
            }
        }

        return null
    }
}
