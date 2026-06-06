package com.devenlucaz.doscom.screen

import android.content.Context
import com.devenlucaz.doscom.utils.ScreenMetrics
import kotlin.math.max
import kotlin.math.min

object CoordinateMapper {

    /**
     * Converts a raw center coordinate into top-left layout parameters for the character view,
     * centering it on the target and ensuring it doesn't render off-screen.
     */
    fun fromNodeCoords(context: Context, rawX: Int, rawY: Int, characterSizePx: Int): Pair<Int, Int> {
        val screenWidth = ScreenMetrics.getScreenWidth(context)
        val screenHeight = ScreenMetrics.getScreenHeight(context)
        val statusBarHeight = ScreenMetrics.getStatusBarHeight(context)

        val desiredX = rawX - (characterSizePx / 2)
        val desiredY = rawY - (characterSizePx / 2)

        val clampedX = max(0, min(desiredX, screenWidth - characterSizePx))
        val clampedY = max(0, min(desiredY, screenHeight - characterSizePx - statusBarHeight))

        return Pair(clampedX, clampedY)
    }

    /**
     * Converts percentage-based coordinates (e.g., from Gemini Vision, range 0.0-1.0 or 0-100) 
     * into clamped pixel coordinates for the character overlay.
     */
    fun fromPercent(context: Context, xPercent: Float, yPercent: Float, characterSizePx: Int): Pair<Int, Int> {
        val screenWidth = ScreenMetrics.getScreenWidth(context)
        val screenHeight = ScreenMetrics.getScreenHeight(context)
        
        // Ensure percent is between 0.0 and 1.0 (some APIs return 0-100 instead of 0.0-1.0)
        val normX = if (xPercent > 1f) xPercent / 100f else xPercent
        val normY = if (yPercent > 1f) yPercent / 100f else yPercent

        val rawX = (screenWidth * normX).toInt()
        val rawY = (screenHeight * normY).toInt()

        return fromNodeCoords(context, rawX, rawY, characterSizePx)
    }
}
