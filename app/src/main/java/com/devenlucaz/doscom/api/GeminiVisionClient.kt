package com.devenlucaz.doscom.api

import android.graphics.Bitmap

object GeminiVisionClient {

    data class VisionResponse(
        val xPercent: Float,
        val yPercent: Float,
        val explanation: String
    )

    suspend fun analyze(screenshot: Bitmap, query: String): VisionResponse? {
        // To be implemented in Phase 6
        return null
    }
}
