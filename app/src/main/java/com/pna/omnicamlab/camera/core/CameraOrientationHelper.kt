package com.pna.omnicamlab.camera.core

object CameraOrientationHelper {
    /**
     * Calculates the JPEG orientation based on sensor orientation, device rotation, and camera facing.
     * Math complies with official Camera2 recommendation:
     * - Back camera: (sensorOrientation - deviceRotation + 360) % 360
     * - Front camera: (sensorOrientation + deviceRotation) % 360
     *
     * @param sensorOrientation The sensor orientation degrees (usually 90 or 270)
     * @param deviceRotationDegrees The device rotation in degrees (0, 90, 180, 270)
     * @param isFrontCamera True if using the front/selfie camera
     * @return Calculated orientation in degrees (0, 90, 180, 270)
     */
    fun calculateJpegOrientation(
        sensorOrientation: Int?,
        deviceRotationDegrees: Int,
        isFrontCamera: Boolean
    ): Int {
        val sensorOri = sensorOrientation ?: if (isFrontCamera) 270 else 90
        return if (isFrontCamera) {
            (sensorOri - deviceRotationDegrees + 360) % 360
        } else {
            (sensorOri + deviceRotationDegrees) % 360
        }
    }
}
