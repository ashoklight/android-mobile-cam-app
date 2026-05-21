package com.pna.omnicamlab.camera.core

import com.pna.omnicamlab.camera.capabilities.CameraSize
import kotlin.math.abs

object PreviewSizeSelector {

    /**
     * Selects the most optimal preview size from the available sizes based on PreviewAspectMode.
     * Normalizes all aspect ratios to landscape (ratio >= 1.0).
     */
    fun selectPreviewSize(
        availableSizes: List<CameraSize>,
        stillCaptureSize: CameraSize,
        aspectMode: PreviewAspectMode,
        screenAspectRatio: Float
    ): CameraSize {
        if (availableSizes.isEmpty()) {
            return CameraSize(1280, 720) // Safe hardcoded fallback
        }

        val stillRatio = if (stillCaptureSize.width > 0 && stillCaptureSize.height > 0) {
            val maxDim = maxOf(stillCaptureSize.width, stillCaptureSize.height).toFloat()
            val minDim = minOf(stillCaptureSize.width, stillCaptureSize.height).toFloat()
            maxDim / minDim
        } else {
            4f / 3f
        }

        val screenRatio = if (screenAspectRatio < 1.0f) 1.0f / screenAspectRatio else screenAspectRatio

        val targetRatio = when (aspectMode) {
            PreviewAspectMode.PHOTO_WYSIWYG -> stillRatio
            PreviewAspectMode.SCREEN_FILL -> screenRatio
            PreviewAspectMode.VIDEO_16_9 -> 16f / 9f
        }

        // Filter out very large sizes (like 4K preview) to prevent memory issues.
        // Camera preview doesn't need to be larger than 1920x1080 in landscape format.
        val safeSizes = availableSizes.filter { size ->
            val maxDim = maxOf(size.width, size.height)
            val minDim = minOf(size.width, size.height)
            maxDim <= 1920 && minDim <= 1080
        }.ifEmpty { availableSizes }

        // Find sizes with closest aspect ratio to target ratio, preferring larger safe areas to preserve quality.
        val sortedByAspectDiff = safeSizes.sortedWith(
            compareBy<CameraSize> { size ->
                val sizeMax = maxOf(size.width, size.height).toFloat()
                val sizeMin = minOf(size.width, size.height).toFloat()
                val sizeRatio = sizeMax / sizeMin
                abs(sizeRatio - targetRatio)
            }.thenByDescending { size ->
                size.area
            }
        )

        return sortedByAspectDiff.firstOrNull() ?: availableSizes.first()
    }

    /**
     * Backward-compatible selector signature.
     */
    fun selectPreviewSize(
        availableSizes: List<CameraSize>,
        screenAspectRatio: Float
    ): CameraSize {
        return selectPreviewSize(
            availableSizes = availableSizes,
            stillCaptureSize = CameraSize(4096, 3072),
            aspectMode = PreviewAspectMode.SCREEN_FILL,
            screenAspectRatio = screenAspectRatio
        )
    }
}
