package com.devenlucaz.doscom.screen

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

object AccessibilityScanner {
    private const val TAG = "DosComAccessibilityScanner"

    fun findNodeByText(rootNode: AccessibilityNodeInfo?, targetText: String): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        val text = rootNode.text?.toString()
        val contentDesc = rootNode.contentDescription?.toString()
        val viewId = rootNode.viewIdResourceName?.toString()

        val containsText = text?.contains(targetText, ignoreCase = true) == true
        val containsContentDesc = contentDesc?.contains(targetText, ignoreCase = true) == true
        val containsViewId = viewId?.contains(targetText, ignoreCase = true) == true

        // Log everything we see on the screen to help debug ColorOS specific issues
        if (!text.isNullOrBlank() || !contentDesc.isNullOrBlank() || !viewId.isNullOrBlank()) {
            Log.d(TAG, "Scanning Node -> Text: '$text', ContentDesc: '$contentDesc', ViewId: '$viewId'")
        }

        if (containsText || containsContentDesc || containsViewId) {
            Log.d(TAG, "MATCH FOUND! Target '$targetText' matched in node -> Text: '$text', ContentDesc: '$contentDesc', ViewId: '$viewId'")
            return rootNode
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

    fun getNodeCenterCoords(node: AccessibilityNodeInfo): Pair<Int, Int> {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return Pair(rect.centerX(), rect.centerY())
    }
}
