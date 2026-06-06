package com.devenlucaz.doscom.screen

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

object AccessibilityScanner {
    private const val TAG = "DosComAccessibilityScanner"

    val APP_PACKAGE_MAP = mapOf(
        "camera" to listOf("com.android.camera", "com.oppo.camera", "com.coloros.camera"),
        "youtube" to listOf("com.google.android.youtube"),
        "chrome" to listOf("com.android.chrome"),
        "whatsapp" to listOf("com.whatsapp"),
        "instagram" to listOf("com.instagram.android"),
        "settings" to listOf("com.android.settings"),
        "phone" to listOf("com.android.phone"),
        "messages" to listOf("com.android.mms"),
        "photos" to listOf("com.google.android.apps.photos"),
        "maps" to listOf("com.google.android.apps.maps"),
        "calculator" to listOf("com.android.calculator2"),
        "clock" to listOf("com.android.deskclock"),
        "recorder" to listOf("com.android.soundrecorder")
    )

    fun findNodeByText(rootNode: AccessibilityNodeInfo?, targetText: String): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        val text = rootNode.text?.toString()
        val contentDesc = rootNode.contentDescription?.toString()
        val viewId = rootNode.viewIdResourceName?.toString()

        // Log everything we see on the screen to help debug ColorOS specific issues
        if (!text.isNullOrBlank() || !contentDesc.isNullOrBlank() || !viewId.isNullOrBlank()) {
            Log.d(TAG, "Scanning Node -> Text: '$text', ContentDesc: '$contentDesc', ViewId: '$viewId'")
        }

        // 1. Direct partial match (case-insensitive)
        val containsText = text?.contains(targetText, ignoreCase = true) == true
        val containsContentDesc = contentDesc?.contains(targetText, ignoreCase = true) == true
        val containsViewId = viewId?.contains(targetText, ignoreCase = true) == true

        if (containsText || containsContentDesc || containsViewId) {
            Log.d(TAG, "MATCH FOUND (Direct)! Target '$targetText' matched in node -> Text: '$text', ContentDesc: '$contentDesc', ViewId: '$viewId'")
            return rootNode
        }

        // 2. Word-by-word fuzzy match (e.g. target "chrome" matches node "Google Chrome", target "google chrome" matches node "Chrome")
        val targetWords = targetText.lowercase().split("\\s+".toRegex()).filter { it.length > 1 }
        val textWords = text?.lowercase()?.split("\\s+".toRegex()) ?: emptyList()
        val descWords = contentDesc?.lowercase()?.split("\\s+".toRegex()) ?: emptyList()

        for (word in targetWords) {
            if (text?.contains(word, ignoreCase = true) == true || contentDesc?.contains(word, ignoreCase = true) == true) {
                Log.d(TAG, "MATCH FOUND (Word Contains)! Target word '$word' matched in node")
                return rootNode
            }
            if (textWords.any { it == word } || descWords.any { it == word }) {
                Log.d(TAG, "MATCH FOUND (Word Exact)! Target word '$word' matched in node")
                return rootNode
            }
        }

        for (i in 0 until rootNode.childCount) {
            val childNode = rootNode.getChild(i)
            if (childNode != null) {
                val result = findNodeByText(childNode, targetText)
                if (result != null) {
                    if (result != childNode) {
                        childNode.recycle()
                    }
                    return result
                }
                childNode.recycle()
            }
        }

        return null
    }

    fun findNodeByPackageName(rootNode: AccessibilityNodeInfo?, targetPackages: List<String>): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        val nodePackage = rootNode.packageName?.toString()
        val viewId = rootNode.viewIdResourceName?.toString()
        val contentDesc = rootNode.contentDescription?.toString()

        for (pkg in targetPackages) {
            if (nodePackage?.contains(pkg, ignoreCase = true) == true ||
                viewId?.contains(pkg, ignoreCase = true) == true ||
                contentDesc?.contains(pkg, ignoreCase = true) == true) {
                Log.d(TAG, "MATCH FOUND (PackageName)! Target package '$pkg' matched in node -> Package: '$nodePackage', ViewId: '$viewId', ContentDesc: '$contentDesc'")
                return rootNode
            }
        }

        for (i in 0 until rootNode.childCount) {
            val childNode = rootNode.getChild(i)
            if (childNode != null) {
                val result = findNodeByPackageName(childNode, targetPackages)
                if (result != null) {
                    if (result != childNode) {
                        childNode.recycle()
                    }
                    return result
                }
                childNode.recycle()
            }
        }

        return null
    }

    fun getNodeCenterCoords(node: AccessibilityNodeInfo): Pair<Int, Int> {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return Pair(rect.centerX(), rect.centerY())
    }
}
