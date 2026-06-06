package com.devenlucaz.doscom.screen

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object AccessibilityScanner {

    fun findNodeByText(rootNode: AccessibilityNodeInfo?, targetText: String): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        val text = rootNode.text?.toString()
        val contentDesc = rootNode.contentDescription?.toString()

        val containsText = text?.contains(targetText, ignoreCase = true) == true
        val containsContentDesc = contentDesc?.contains(targetText, ignoreCase = true) == true

        if (containsText || containsContentDesc) {
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
