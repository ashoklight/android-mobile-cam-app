package com.pna.omnicamlab.camera.capabilities

import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilityMappingTest {

    @Test
    fun testFormatFocalLengths() {
        assertEquals("N/A", CameraCapabilityFormatter.formatFocalLengths(emptyList()))
        assertEquals("4.73mm", CameraCapabilityFormatter.formatFocalLengths(listOf(4.73f)))
        assertEquals("4.73mm, 1.85mm", CameraCapabilityFormatter.formatFocalLengths(listOf(4.73f, 1.85f)))
    }

    @Test
    fun testFormatApertures() {
        assertEquals("N/A", CameraCapabilityFormatter.formatApertures(emptyList()))
        assertEquals("f/1.80", CameraCapabilityFormatter.formatApertures(listOf(1.8f)))
        assertEquals("f/1.80, f/2.20", CameraCapabilityFormatter.formatApertures(listOf(1.8f, 2.2f)))
    }

    @Test
    fun testFormatIsoRange() {
        assertEquals("Unknown Range", CameraCapabilityFormatter.formatIsoRange(null, null))
        assertEquals("ISO 50 - 3200", CameraCapabilityFormatter.formatIsoRange(50, 3200))
    }

    @Test
    fun testFormatExposureTime_nanoseconds() {
        // Fast speeds (fractional seconds)
        assertEquals("1/8000s", CameraCapabilityFormatter.formatExposureTime(125_000L))      // 125 us -> 1/8000s
        assertEquals("1/1000s", CameraCapabilityFormatter.formatExposureTime(1_000_000L))     // 1 ms -> 1/1000s
        assertEquals("1/250s", CameraCapabilityFormatter.formatExposureTime(4_000_000L))      // 4 ms -> 1/250s
        assertEquals("1/125s", CameraCapabilityFormatter.formatExposureTime(8_000_000L))      // 8 ms -> 1/125s
        assertEquals("1/30s", CameraCapabilityFormatter.formatExposureTime(33_333_333L))     // 33.3 ms -> 1/30s

        // Slow speeds (seconds or decimals)
        assertEquals("1s", CameraCapabilityFormatter.formatExposureTime(1_000_000_000L))      // 1.0s
        assertEquals("2s", CameraCapabilityFormatter.formatExposureTime(2_000_000_000L))      // 2.0s
        assertEquals("1.5s", CameraCapabilityFormatter.formatExposureTime(1_500_000_000L))    // 1.5s
        assertEquals("30s", CameraCapabilityFormatter.formatExposureTime(30_000_000_000L))    // 30s
    }

    @Test
    fun testFormatFocusCalibration() {
        assertEquals("UNCALIBRATED", CameraCapabilityFormatter.formatFocusCalibration(0))
        assertEquals("APPROXIMATE", CameraCapabilityFormatter.formatFocusCalibration(1))
        assertEquals("CALIBRATED", CameraCapabilityFormatter.formatFocusCalibration(2))
        assertEquals("UNKNOWN", CameraCapabilityFormatter.formatFocusCalibration(99))
    }

    @Test
    fun testFormatHardwareLevel() {
        assertEquals("Legacy (LEGACY)", CameraCapabilityFormatter.formatHardwareLevel(HardwareLevel.LEGACY))
        assertEquals("Limited (LIMITED)", CameraCapabilityFormatter.formatHardwareLevel(HardwareLevel.LIMITED))
        assertEquals("Full (FULL)", CameraCapabilityFormatter.formatHardwareLevel(HardwareLevel.FULL))
        assertEquals("Level 3 (LEVEL_3)", CameraCapabilityFormatter.formatHardwareLevel(HardwareLevel.LEVEL_3))
        assertEquals("External (EXTERNAL)", CameraCapabilityFormatter.formatHardwareLevel(HardwareLevel.EXTERNAL))
        assertEquals("Unknown", CameraCapabilityFormatter.formatHardwareLevel(HardwareLevel.UNKNOWN))
    }

    @Test
    fun testFormatAfMode() {
        assertEquals("Manual Focus (OFF)", CameraCapabilityFormatter.formatAfMode("OFF"))
        assertEquals("Single AF (AUTO)", CameraCapabilityFormatter.formatAfMode("AUTO"))
        assertEquals("Macro AF (MACRO)", CameraCapabilityFormatter.formatAfMode("MACRO"))
        assertEquals("Continuous Picture AF", CameraCapabilityFormatter.formatAfMode("CONTINUOUS_PICTURE"))
        assertEquals("Continuous Video AF", CameraCapabilityFormatter.formatAfMode("CONTINUOUS_VIDEO"))
    }
}
