package com.devenlucaz.doscom.screen

import android.content.Context
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import com.devenlucaz.doscom.api.GeminiVisionClient

object ScreenReader {

    data class TargetResult(
        val x: Int,
        val y: Int,
        val explanation: String,
        val source: String
    )

    suspend fun findTarget(
        context: Context,
        query: String,
        rootNode: AccessibilityNodeInfo?,
        screenshot: Bitmap?,
        characterSizePx: Int
    ): TargetResult? {
        // Layer 1: Fast Accessibility Tree Scan
        val keywords = KeywordExtractor.extractKeywords(query).second
        if (keywords.isNotBlank() && rootNode != null) {
            val foundNode = AccessibilityScanner.findNodeByText(rootNode, keywords)
            if (foundNode != null) {
                val coords = AccessibilityScanner.getNodeCenterCoords(foundNode)
                val mapped = CoordinateMapper.fromNodeCoords(context, coords.first, coords.second, characterSizePx)
                return TargetResult(
                    x = mapped.first,
                    y = mapped.second,
                    explanation = "I found exactly what you're looking for natively.",
                    source = "Accessibility"
                )
            }
        }

        // Layer 2: Fallback to Gemini Vision API
        if (screenshot != null) {
            val visionResult = GeminiVisionClient.analyze(screenshot, query)
            if (visionResult != null) {
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
