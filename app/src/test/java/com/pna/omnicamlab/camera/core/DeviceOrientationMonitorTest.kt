package com.pna.omnicamlab.camera.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceOrientationMonitorTest {

    @Test
    fun quantizeOrientation_validDegrees_mapsToCorrectBuckets() {
        // Test near 0 degrees (Portrait)
        assertEquals(0, DeviceOrientationMonitor.quantizeOrientation(0))
        assertEquals(0, DeviceOrientationMonitor.quantizeOrientation(350))
        assertEquals(0, DeviceOrientationMonitor.quantizeOrientation(10))
        assertEquals(0, DeviceOrientationMonitor.quantizeOrientation(315))
        assertEquals(0, DeviceOrientationMonitor.quantizeOrientation(44))

        // Test near 90 degrees (Landscape left-ish)
        assertEquals(90, DeviceOrientationMonitor.quantizeOrientation(90))
        assertEquals(90, DeviceOrientationMonitor.quantizeOrientation(45))
        assertEquals(90, DeviceOrientationMonitor.quantizeOrientation(134))

        // Test near 180 degrees (Reverse portrait)
        assertEquals(180, DeviceOrientationMonitor.quantizeOrientation(180))
        assertEquals(180, DeviceOrientationMonitor.quantizeOrientation(135))
        assertEquals(180, DeviceOrientationMonitor.quantizeOrientation(224))

        // Test near 270 degrees (Landscape right-ish)
        assertEquals(270, DeviceOrientationMonitor.quantizeOrientation(270))
        assertEquals(270, DeviceOrientationMonitor.quantizeOrientation(225))
        assertEquals(270, DeviceOrientationMonitor.quantizeOrientation(314))
    }

    @Test
    fun quantizeOrientation_unknownValue_returnsSentinel() {
        assertEquals(-1, DeviceOrientationMonitor.quantizeOrientation(DeviceOrientationMonitor.ORIENTATION_UNKNOWN))
        assertEquals(-1, DeviceOrientationMonitor.quantizeOrientation(-1))
    }

    @Test
    fun quantizeOrientation_moduloMath_handlesOutOfRangeDegrees() {
        // 370 degrees is 10 degrees, which is 0 quantized
        assertEquals(0, DeviceOrientationMonitor.quantizeOrientation(370))
        // -90 degrees is 270 degrees, which is 270 quantized
        assertEquals(270, DeviceOrientationMonitor.quantizeOrientation(-90))
    }
}
