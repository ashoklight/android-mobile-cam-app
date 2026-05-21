package com.pna.omnicamlab.camera.capabilities

import org.junit.Assert.assertTrue
import org.junit.Test

class JSONExportTest {

    @Test
    fun testExportToJsonString_structure() {
        val mockProfile = CameraDeviceProfile(
            cameraId = "0",
            facing = CameraFacing.BACK,
            hardwareLevel = HardwareLevel.LEVEL_3,
            capabilities = setOf(CameraCapability.MANUAL_SENSOR, CameraCapability.RAW_CAPTURE, CameraCapability.BURST_CAPTURE),
            lensProfile = LensProfile(
                focalLengths = listOf(4.73f),
                apertures = listOf(1.8f),
                minFocusDistance = 0.1f,
                focusDistanceCalibration = "CALIBRATED",
                opticalStabilizationModes = listOf("ON"),
                availableAfModes = listOf("AUTO", "MACRO"),
                lensFacingRaw = 1,
                zoomRatioRange = "[1.0x - 5.0x]"
            ),
            sensorProfile = SensorProfile(
                isoRange = "50 - 3200",
                exposureTimeRangeNs = "125000 - 30000000000",
                maxFrameDurationNs = 33333333L,
                activeArraySize = "0, 0, 4000, 3000",
                pixelArraySize = "4000x3000",
                sensorOrientation = 90,
                colorFilterArrangement = "RGGB (Bayer)",
                timestampSource = "REALTIME (CLOCK_BOOTTIME)"
            ),
            photoProfile = PhotoProfile(
                jpegSizes = listOf("4000x3000", "1920x1080"),
                rawSizes = listOf("4000x3000"),
                yuvSizes = listOf("4000x3000", "1920x1080"),
                privateSizes = listOf("4000x3000", "1920x1080"),
                supportsRaw = true,
                supportsBurst = true,
                largestJpeg = "4000x3000",
                largestRaw = "4000x3000",
                largestYuv = "4000x3000"
            ),
            videoProfile = VideoProfile(
                fpsRanges = listOf("[30 - 30]", "[60 - 60]"),
                highSpeedVideoSizes = listOf("1920x1080", "1280x720"),
                highSpeedFpsRanges = listOf("[120 - 120]", "[240 - 240]"),
                stabilizationModes = listOf("OFF", "ON"),
                supportsHighSpeedVideo = true
            ),
            extensionProfile = ExtensionProfile(
                supportsAuto = true,
                supportsHdr = true,
                supportsNight = true,
                supportsBokeh = false,
                supportsFaceRetouch = false,
                notes = listOf("Mock notes")
            ),
            supportWarnings = listOf("This is a mock hardware warning"),
            isLogicalMultiCamera = false,
            physicalCameraIds = emptyList(),
            openableCameraId = true
        )

        val json = CameraCapabilityJsonExporter.exportToJsonString(listOf(mockProfile))
        
        // Assert top-level keys
        assertTrue(json.contains("\"cameraId\": \"0\""))
        assertTrue(json.contains("\"facing\": \"BACK\""))
        assertTrue(json.contains("\"hardwareLevel\": \"LEVEL_3\""))
        assertTrue(json.contains("\"isLogicalMultiCamera\": false"))
        assertTrue(json.contains("\"openableCameraId\": true"))

        // Assert capabilities list
        assertTrue(json.contains("\"capabilities\": [\"MANUAL_SENSOR\", \"RAW_CAPTURE\", \"BURST_CAPTURE\"]"))

        // Assert nested lensProfile
        assertTrue(json.contains("\"lensProfile\": {"))
        assertTrue(json.contains("\"focalLengths\": [4.73]"))
        assertTrue(json.contains("\"apertures\": [1.8]"))
        assertTrue(json.contains("\"minFocusDistance\": 0.1"))
        assertTrue(json.contains("\"focusDistanceCalibration\": \"CALIBRATED\""))
        assertTrue(json.contains("\"zoomRatioRange\": \"[1.0x - 5.0x]\""))

        // Assert nested sensorProfile
        assertTrue(json.contains("\"sensorProfile\": {"))
        assertTrue(json.contains("\"isoRange\": \"50 - 3200\""))
        assertTrue(json.contains("\"exposureTimeRangeNs\": \"125000 - 30000000000\""))
        assertTrue(json.contains("\"activeArraySize\": \"0, 0, 4000, 3000\""))
        assertTrue(json.contains("\"colorFilterArrangement\": \"RGGB (Bayer)\""))

        // Assert nested photoProfile
        assertTrue(json.contains("\"photoProfile\": {"))
        assertTrue(json.contains("\"jpegSizes\": [\"4000x3000\", \"1920x1080\"]"))
        assertTrue(json.contains("\"supportsRaw\": true"))

        // Assert nested videoProfile
        assertTrue(json.contains("\"videoProfile\": {"))
        assertTrue(json.contains("\"highSpeedVideoSizes\": [\"1920x1080\", \"1280x720\"]"))
        assertTrue(json.contains("\"supportsHighSpeedVideo\": true"))

        // Assert extensionProfile
        assertTrue(json.contains("\"extensionProfile\": {"))
        assertTrue(json.contains("\"supportsHdr\": true"))
        assertTrue(json.contains("\"supportsBokeh\": false"))

        // Assert warnings list
        assertTrue(json.contains("\"supportWarnings\": [\"This is a mock hardware warning\"]"))
    }
}
