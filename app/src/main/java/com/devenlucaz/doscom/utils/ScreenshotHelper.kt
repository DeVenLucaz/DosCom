package com.devenlucaz.doscom.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Base64
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

object ScreenshotHelper {

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var projectionIntentData: Intent? = null
    private var projectionResultCode: Int = Activity.RESULT_CANCELED

    fun requestPermission(activity: Activity, requestCode: Int) {
        if (hasPermission()) return

        if (mediaProjectionManager == null) {
            mediaProjectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        }
        
        mediaProjectionManager?.let { manager ->
            activity.startActivityForResult(manager.createScreenCaptureIntent(), requestCode)
        }
    }

    fun hasPermission(): Boolean {
        return projectionIntentData != null && projectionResultCode == Activity.RESULT_OK
    }

    fun onPermissionResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            projectionResultCode = resultCode
            projectionIntentData = data
        }
    }

    suspend fun captureScreen(context: Context): Bitmap? = suspendCancellableCoroutine { continuation ->
        if (!hasPermission()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        if (mediaProjectionManager == null) {
            mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        }

        if (mediaProjection == null) {
            mediaProjection = mediaProjectionManager?.getMediaProjection(projectionResultCode, projectionIntentData!!)
        }

        val projection = mediaProjection ?: run {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val screenWidth = ScreenMetrics.getScreenWidth(context)
        val screenHeight = ScreenMetrics.getScreenHeight(context)
        val densityDpi = context.resources.displayMetrics.densityDpi

        val imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        
        var virtualDisplay = projection.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )

        imageReader.setOnImageAvailableListener({ reader ->
            var finalBitmap: Bitmap? = null
            try {
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * screenWidth
                    
                    val tmpBitmap = Bitmap.createBitmap(screenWidth + rowPadding / pixelStride, screenHeight, Bitmap.Config.ARGB_8888)
                    tmpBitmap.copyPixelsFromBuffer(buffer)
                    
                    val croppedBitmap = Bitmap.createBitmap(tmpBitmap, 0, 0, screenWidth, screenHeight)
                    
                    finalBitmap = if (croppedBitmap.width > 800) {
                        val ratio = 800f / croppedBitmap.width
                        val height = (croppedBitmap.height * ratio).toInt()
                        Bitmap.createScaledBitmap(croppedBitmap, 800, height, true)
                    } else {
                        croppedBitmap
                    }
                    
                    image.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                virtualDisplay?.release()
                virtualDisplay = null
                reader.setOnImageAvailableListener(null, null)
                reader.close()
                
                if (continuation.isActive) {
                    continuation.resume(finalBitmap)
                }
            }
        }, null)
        
        continuation.invokeOnCancellation {
            virtualDisplay?.release()
            imageReader.close()
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
