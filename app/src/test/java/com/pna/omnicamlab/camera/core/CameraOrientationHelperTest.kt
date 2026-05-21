package com.pna.omnicamlab.camera.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraOrientationHelperTest {

    @Test
    fun calculateJpegOrientation_backCamera_sensor90() {
        val sensorOrientation = 90
        
        // (sensorOrientation + deviceRotation) % 360
        // Device 0 -> (90 + 0) % 360 = 90
        assertEquals(90, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 0, false))
        
        // Device 90 -> (90 + 90) % 360 = 180
        assertEquals(180, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 90, false))
        
        // Device 180 -> (90 + 180) % 360 = 270
        assertEquals(270, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 180, false))
        
        // Device 270 -> (90 + 270) % 360 = 0
        assertEquals(0, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 270, false))
    }

    @Test
    fun calculateJpegOrientation_backCamera_sensor270() {
        val sensorOrientation = 270
        
        // (sensorOrientation + deviceRotation) % 360
        // Device 0 -> (270 + 0) % 360 = 270
        assertEquals(270, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 0, false))
        
        // Device 90 -> (270 + 90) % 360 = 0
        assertEquals(0, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 90, false))
        
        // Device 180 -> (270 + 180) % 360 = 90
        assertEquals(90, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 180, false))
        
        // Device 270 -> (270 + 270) % 360 = 180
        assertEquals(180, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 270, false))
    }

    @Test
    fun calculateJpegOrientation_frontCamera_sensor270() {
        val sensorOrientation = 270
        
        // (sensorOrientation - deviceRotation + 360) % 360
        // Device 0 -> (270 - 0) % 360 = 270
        assertEquals(270, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 0, true))
        
        // Device 90 -> (270 - 90 + 360) % 360 = 180
        assertEquals(180, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 90, true))
        
        // Device 180 -> (270 - 180 + 360) % 360 = 90
        assertEquals(90, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 180, true))
        
        // Device 270 -> (270 - 270 + 360) % 360 = 0
        assertEquals(0, CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, 270, true))
    }

    @Test
    fun calculateJpegOrientation_nullSensorOrientation_usesFacingFallbacks() {
        // Back camera defaults to 90
        assertEquals(90, CameraOrientationHelper.calculateJpegOrientation(null, 0, false))
        assertEquals(180, CameraOrientationHelper.calculateJpegOrientation(null, 90, false))
        
        // Front camera defaults to 270
        assertEquals(270, CameraOrientationHelper.calculateJpegOrientation(null, 0, true))
        assertEquals(180, CameraOrientationHelper.calculateJpegOrientation(null, 90, true))
    }
}

