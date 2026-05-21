package com.pna.omnicamlab.camera.core

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Range

class ZoomController(private val characteristics: CameraCharacteristics) {

    val zoomState: ZoomState

    init {
        var minZoom = 1.0f
        var maxZoom = 1.0f
        var isSupported = false
        var backend = "UNSUPPORTED"

        // 1. Check for API 30+ CONTROL_ZOOM_RATIO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val ratioRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            if (ratioRange != null && ratioRange.lower < ratioRange.upper) {
                minZoom = ratioRange.lower
                maxZoom = ratioRange.upper
                isSupported = true
                backend = "CONTROL_ZOOM_RATIO"
            }
        }

        // 2. Fallback to SCALER_CROP_REGION if CONTROL_ZOOM_RATIO is not supported or older API
        if (!isSupported) {
            val maxDigitalZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
            if (maxDigitalZoom != null && maxDigitalZoom > 1.0f) {
                minZoom = 1.0f
                maxZoom = maxDigitalZoom
                isSupported = true
                backend = "SCALER_CROP_REGION"
            }
        }

        zoomState = ZoomState(
            minZoom = minZoom,
            maxZoom = maxZoom,
            currentZoom = 1.0f,
            isZoomSupported = isSupported,
            zoomBackend = backend
        )
    }

    /**
     * Clamps the target zoom factor to the supported range.
     */
    fun clampZoom(zoom: Float): Float {
        if (!zoomState.isZoomSupported) return 1.0f
        return zoom.coerceIn(zoomState.minZoom, zoomState.maxZoom)
    }

    /**
     * Calculates the SCALER_CROP_REGION Rect for the given zoom factor based on SENSOR_INFO_ACTIVE_ARRAY_SIZE.
     */
    fun calculateCropRegion(zoom: Float): Rect? {
        val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return null
        if (zoom <= 1.0f) return activeArray

        val centerX = activeArray.centerX()
        val centerY = activeArray.centerY()
        val deltaX = (activeArray.width() / (2.0f * zoom)).toInt()
        val deltaY = (activeArray.height() / (2.0f * zoom)).toInt()

        return Rect(
            centerX - deltaX,
            centerY - deltaY,
            centerX + deltaX,
            centerY + deltaY
        )
    }

    /**
     * Applies the appropriate zoom capture request parameters to the given builder.
     */
    fun applyZoom(builder: CaptureRequest.Builder, zoom: Float) {
        if (!zoomState.isZoomSupported) return

        val clampedZoom = clampZoom(zoom)

        if (zoomState.zoomBackend == "CONTROL_ZOOM_RATIO" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, clampedZoom)
        } else if (zoomState.zoomBackend == "SCALER_CROP_REGION") {
            val cropRect = calculateCropRegion(clampedZoom)
            if (cropRect != null) {
                builder.set(CaptureRequest.SCALER_CROP_REGION, cropRect)
            }
        }
    }
}
