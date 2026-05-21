package com.pna.omnicamlab.camera.core

import org.junit.Assert.assertEquals
import org.junit.Test

class JpegOrientationTest {

    @Test
    fun calculateJpegOrientation_backCamera_calculatesCorrectly() {
        val sensorOrientation = 90
        val isFrontCamera = false

        // Portrait: device orientation 0 -> JPEG 90
        assertEquals(90, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 0, isFrontCamera))

        // Landscape clockwise: device orientation 90 -> JPEG 180
        assertEquals(180, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 90, isFrontCamera))

        // Landscape upside down: device orientation 180 -> JPEG 270
        assertEquals(270, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 180, isFrontCamera))

        // Landscape counter-clockwise: device orientation 270 -> JPEG 0
        assertEquals(0, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 270, isFrontCamera))
    }

    @Test
    fun calculateJpegOrientation_frontCamera_calculatesCorrectly() {
        val sensorOrientation = 270
        val isFrontCamera = true

        // Portrait: device orientation 0 -> JPEG 270
        assertEquals(270, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 0, isFrontCamera))

        // Landscape clockwise: device orientation 90 -> JPEG 180
        assertEquals(180, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 90, isFrontCamera))

        // Landscape upside down: device orientation 180 -> JPEG 90
        assertEquals(90, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 180, isFrontCamera))

        // Landscape counter-clockwise: device orientation 270 -> JPEG 0
        assertEquals(0, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 270, isFrontCamera))
    }

    @Test
    fun calculateJpegOrientation_nullSensorOrientation_usesDefaultFallback() {
        val isFrontCamera = false
        // Should fallback to 90 degrees for back camera
        assertEquals(180, CameraOrientationHelper.calculateJpegOrientation(null, 90, isFrontCamera))

        val isFrontCameraFront = true
        // Should fallback to 270 degrees for front camera
        assertEquals(180, CameraOrientationHelper.calculateJpegOrientation(null, 90, isFrontCameraFront))
    }
}
