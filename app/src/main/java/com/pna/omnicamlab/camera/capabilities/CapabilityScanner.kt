package com.pna.omnicamlab.camera.capabilities

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Range
import android.util.Size
import com.pna.omnicamlab.util.logging.OmniLogger
import java.util.Locale

class CapabilityScanner(private val context: Context) {

    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /**
     * Scans all available cameras and returns their profiles.
     */
    fun scanCapabilities(): List<CameraDeviceProfile> {
        val profiles = mutableListOf<CameraDeviceProfile>()
        try {
            val cameraIds = cameraManager.cameraIdList
            OmniLogger.i(OmniLogger.Tag.CapabilityScanner, "Discovered ${cameraIds.size} physical/logical camera IDs.")
            
            for (id in cameraIds) {
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val profile = parseProfile(id, characteristics)
                    profiles.add(profile)
                } catch (e: Exception) {
                    OmniLogger.e(OmniLogger.Tag.CapabilityScanner, "Failed to scan camera ID $id safely", e)
                }
            }
        } catch (e: Exception) {
            OmniLogger.e(OmniLogger.Tag.CapabilityScanner, "Failed to query camera ID list", e)
        }
        return profiles
    }

    private fun parseProfile(cameraId: String, c: CameraCharacteristics): CameraDeviceProfile {
        // 1. Hardware level mapping
        val levelRaw = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val hardwareLevel = mapHardwareLevel(levelRaw)

        // 2. Facing mapping
        val facingRaw = c.get(CameraCharacteristics.LENS_FACING)
        val facing = mapFacing(facingRaw)

        // 3. Set of Camera capabilities
        val capabilitiesRaw = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val capabilities = mapCapabilities(capabilitiesRaw)

        // 4. Logical multi-camera characteristics
        val isLogical = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            capabilities.contains(CameraCapability.LOGICAL_MULTI_CAMERA)
        } else {
            false
        }
        val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isLogical) {
            try {
                c.physicalCameraIds.toList()
            } catch (e: NoSuchMethodError) {
                emptyList()
            }
        } else {
            emptyList()
        }

        // 5. Lens profiles
        val focalLengths = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
        val apertures = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList() ?: emptyList()
        val minFocusDistance = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val focusDistanceCalibrationRaw = c.get(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION)
        val focusDistanceCalibration = focusDistanceCalibrationRaw?.let { CameraCapabilityFormatter.formatFocusCalibration(it) }

        val opticalStabilizationModes = parseOisModes(c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION))
        val availableAfModes = parseAfModes(c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES))

        val zoomRatioRange = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            c.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)?.let {
                "[${it.lower}x - ${it.upper}x]"
            }
        } else {
            c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)?.let { maxZoom ->
                if (maxZoom > 1.0f) "[1.0x - ${maxZoom}x]" else "[1.0x]"
            }
        }

        val lensProfile = LensProfile(
            focalLengths = focalLengths,
            apertures = apertures,
            minFocusDistance = minFocusDistance,
            focusDistanceCalibration = focusDistanceCalibration,
            opticalStabilizationModes = opticalStabilizationModes,
            availableAfModes = availableAfModes,
            lensFacingRaw = facingRaw,
            zoomRatioRange = zoomRatioRange
        )

        // 6. Sensor profiles
        val isoRangeRaw = c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val isoRange = isoRangeRaw?.let { "${it.lower} - ${it.upper}" }

        val exposureTimeRangeRaw = c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val exposureTimeRangeNs = exposureTimeRangeRaw?.let { "${it.lower} - ${it.upper}" }

        val maxFrameDurationNs = c.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION)
        val activeArraySize = c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)?.toShortString()
        val pixelArraySize = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)?.let { "${it.width}x${it.height}" }
        val sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION)

        val colorFilterRaw = c.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)
        val colorFilterArrangement = mapColorFilter(colorFilterRaw)

        val timestampSourceRaw = c.get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE)
        val timestampSource = mapTimestampSource(timestampSourceRaw)

        val sensorProfile = SensorProfile(
            isoRange = isoRange,
            exposureTimeRangeNs = exposureTimeRangeNs,
            maxFrameDurationNs = maxFrameDurationNs,
            activeArraySize = activeArraySize,
            pixelArraySize = pixelArraySize,
            sensorOrientation = sensorOrientation,
            colorFilterArrangement = colorFilterArrangement,
            timestampSource = timestampSource
        )

        // 7. Output stream profiles
        val map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val photoProfile = parsePhotoProfile(map, capabilities)
        val videoProfile = parseVideoProfile(c, map)

        // 8. Dynamic extensions
        val extensionProfile = queryExtensions(cameraId)

        // 9. Warnings compilation
        val warnings = generateWarnings(
            hardwareLevel,
            facing,
            capabilities,
            lensProfile,
            sensorProfile,
            photoProfile,
            videoProfile
        )

        return CameraDeviceProfile(
            cameraId = cameraId,
            facing = facing,
            hardwareLevel = hardwareLevel,
            capabilities = capabilities,
            lensProfile = lensProfile,
            sensorProfile = sensorProfile,
            photoProfile = photoProfile,
            videoProfile = videoProfile,
            extensionProfile = extensionProfile,
            supportWarnings = warnings,
            isLogicalMultiCamera = isLogical,
            physicalCameraIds = physicalIds,
            openableCameraId = true
        )
    }

    private fun mapHardwareLevel(raw: Int?): HardwareLevel {
        if (raw == null) return HardwareLevel.UNKNOWN
        return when (raw) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> HardwareLevel.LEGACY
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> HardwareLevel.LIMITED
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> HardwareLevel.FULL
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> HardwareLevel.LEVEL_3
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> HardwareLevel.EXTERNAL
            else -> HardwareLevel.UNKNOWN
        }
    }

    private fun mapFacing(raw: Int?): CameraFacing {
        if (raw == null) return CameraFacing.UNKNOWN
        return when (raw) {
            CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.FRONT
            CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.BACK
            CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
            else -> CameraFacing.UNKNOWN
        }
    }

    private fun mapCapabilities(raw: IntArray?): Set<CameraCapability> {
        val set = mutableSetOf<CameraCapability>()
        if (raw == null) return set
        for (cap in raw) {
            val mapped = when (cap) {
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> CameraCapability.MANUAL_SENSOR
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> CameraCapability.MANUAL_POST_PROCESSING
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW -> CameraCapability.RAW_CAPTURE
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> CameraCapability.BURST_CAPTURE
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> CameraCapability.DEPTH_OUTPUT
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> CameraCapability.PRIVATE_REPROCESSING
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> CameraCapability.YUV_REPROCESSING
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> CameraCapability.BACKWARD_COMPATIBLE
                else -> {
                    // Constant mapping for APIs added in later SDK levels
                    val isLogicalMulti = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && 
                            cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
                    val isHighSpeed = cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
                    
                    val isUltraHighRes = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
                            cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
                    val isMonochrome = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && 
                            cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME
                    val isSecure = cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA
                    
                    val isReadSensor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                            cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS

                    when {
                        isLogicalMulti -> CameraCapability.LOGICAL_MULTI_CAMERA
                        isHighSpeed -> CameraCapability.CONSTRAINED_HIGH_SPEED_VIDEO
                        isUltraHighRes -> CameraCapability.ULTRA_HIGH_RESOLUTION_SENSOR
                        isMonochrome -> CameraCapability.MONOCHROME
                        isSecure -> CameraCapability.SECURE_IMAGE_DATA
                        isReadSensor -> CameraCapability.READ_SENSOR_SETTINGS
                        else -> null
                    }
                }
            }
            if (mapped != null) {
                set.add(mapped)
            }
        }
        return set
    }

    private fun mapColorFilter(raw: Int?): String? {
        if (raw == null) return null
        return when (raw) {
            0 -> "RGGB (Bayer)"
            1 -> "GRBG"
            2 -> "GBRG"
            3 -> "BGGR"
            4 -> "RGB"
            5 -> "MONOCHROME"
            else -> "UNKNOWN ($raw)"
        }
    }

    private fun mapTimestampSource(raw: Int?): String? {
        if (raw == null) return null
        return when (raw) {
            CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN -> "UNKNOWN"
            CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME -> "REALTIME (CLOCK_BOOTTIME)"
            else -> "UNKNOWN ($raw)"
        }
    }

    private fun parseOisModes(modes: IntArray?): List<String> {
        if (modes == null) return emptyList()
        val list = mutableListOf<String>()
        for (m in modes) {
            val s = when (m) {
                CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF -> "OFF"
                CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON -> "ON"
                else -> "UNKNOWN ($m)"
            }
            list.add(s)
        }
        return list
    }

    private fun parseAfModes(modes: IntArray?): List<String> {
        if (modes == null) return emptyList()
        val list = mutableListOf<String>()
        for (m in modes) {
            val s = when (m) {
                CameraCharacteristics.CONTROL_AF_MODE_OFF -> "OFF"
                CameraCharacteristics.CONTROL_AF_MODE_AUTO -> "AUTO"
                CameraCharacteristics.CONTROL_AF_MODE_MACRO -> "MACRO"
                CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "CONTINUOUS_VIDEO"
                CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "CONTINUOUS_PICTURE"
                CameraCharacteristics.CONTROL_AF_MODE_EDOF -> "EDOF"
                else -> "UNKNOWN ($m)"
            }
            list.add(s)
        }
        return list
    }

    private fun parsePhotoProfile(map: StreamConfigurationMap?, caps: Set<CameraCapability>): PhotoProfile {
        if (map == null) {
            return PhotoProfile(emptyList(), emptyList(), emptyList(), emptyList(), false, false, null, null, null)
        }

        val jpegs = map.getOutputSizes(ImageFormat.JPEG)?.map { CameraSize(it.width, it.height) } ?: emptyList()
        val sortedJpegs = SizeUtils.sortSizesDescending(jpegs)
        val jpegStr = sortedJpegs.map { it.toString() }

        val raws = mutableListOf<CameraSize>()
        map.getOutputSizes(ImageFormat.RAW_SENSOR)?.forEach { raws.add(CameraSize(it.width, it.height)) }
        if (raws.isEmpty()) {
            map.getOutputSizes(ImageFormat.RAW10)?.forEach { raws.add(CameraSize(it.width, it.height)) }
            map.getOutputSizes(ImageFormat.RAW12)?.forEach { raws.add(CameraSize(it.width, it.height)) }
        }
        val sortedRaws = SizeUtils.sortSizesDescending(raws)
        val rawStr = sortedRaws.map { it.toString() }

        val yuvs = map.getOutputSizes(ImageFormat.YUV_420_888)?.map { CameraSize(it.width, it.height) } ?: emptyList()
        val sortedYuvs = SizeUtils.sortSizesDescending(yuvs)
        val yuvStr = sortedYuvs.map { it.toString() }

        val privs = map.getOutputSizes(ImageFormat.PRIVATE)?.map { CameraSize(it.width, it.height) } ?: emptyList()
        val sortedPrivs = SizeUtils.sortSizesDescending(privs)
        val privStr = sortedPrivs.map { it.toString() }

        val supportsRaw = caps.contains(CameraCapability.RAW_CAPTURE) && raws.isNotEmpty()
        val supportsBurst = caps.contains(CameraCapability.BURST_CAPTURE)

        val largestJpeg = sortedJpegs.firstOrNull()?.toString()
        val largestRaw = sortedRaws.firstOrNull()?.toString()
        val largestYuv = sortedYuvs.firstOrNull()?.toString()

        return PhotoProfile(
            jpegSizes = jpegStr,
            rawSizes = rawStr,
            yuvSizes = yuvStr,
            privateSizes = privStr,
            supportsRaw = supportsRaw,
            supportsBurst = supportsBurst,
            largestJpeg = largestJpeg,
            largestRaw = largestRaw,
            largestYuv = largestYuv
        )
    }

    private fun parseVideoProfile(c: CameraCharacteristics, map: StreamConfigurationMap?): VideoProfile {
        if (map == null) {
            return VideoProfile(emptyList(), emptyList(), emptyList(), emptyList(), false)
        }

        // FPS Ranges
        val fpsRangesRaw = c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        val fpsRanges = fpsRangesRaw?.map { "[${it.lower} - ${it.upper}]" } ?: emptyList()

        // Stabilization
        val stabRaw = c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
        val stab = mutableListOf<String>()
        stabRaw?.forEach { m ->
            val s = when (m) {
                CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF -> "OFF"
                CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON -> "ON"
                else -> "UNKNOWN ($m)"
            }
            stab.add(s)
        }

        // High speed video support
        val supportsHighSpeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                map.highSpeedVideoSizes?.isNotEmpty() ?: false
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }

        val hsSizes = mutableListOf<String>()
        val hsFpsRanges = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && supportsHighSpeed) {
            try {
                map.highSpeedVideoSizes?.forEach { size ->
                    hsSizes.add("${size.width}x${size.height}")
                    map.getHighSpeedVideoFpsRangesFor(size)?.forEach { range ->
                        val rStr = "[${range.lower} - ${range.upper}]"
                        if (!hsFpsRanges.contains(rStr)) {
                            hsFpsRanges.add(rStr)
                        }
                    }
                }
            } catch (e: Exception) {
                OmniLogger.w(OmniLogger.Tag.CapabilityScanner, "Failed to query high-speed configuration safely: ${e.message}")
            }
        }

        return VideoProfile(
            fpsRanges = fpsRanges,
            highSpeedVideoSizes = hsSizes,
            highSpeedFpsRanges = hsFpsRanges,
            stabilizationModes = stab,
            supportsHighSpeedVideo = supportsHighSpeed
        )
    }

    private fun queryExtensions(cameraId: String): ExtensionProfile {
        var auto = false
        var hdr = false
        var night = false
        var bokeh = false
        var retouch = false
        val notes = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val extChars = cameraManager.getCameraExtensionCharacteristics(cameraId)
                val extList = extChars.supportedExtensions
                
                auto = extList.contains(CameraExtensionCharacteristics.EXTENSION_AUTOMATIC)
                hdr = extList.contains(CameraExtensionCharacteristics.EXTENSION_HDR)
                night = extList.contains(CameraExtensionCharacteristics.EXTENSION_NIGHT)
                bokeh = extList.contains(CameraExtensionCharacteristics.EXTENSION_BOKEH)
                retouch = extList.contains(CameraExtensionCharacteristics.EXTENSION_BEAUTY)
                
                notes.add("Query completed on API ${Build.VERSION.SDK_INT} dynamically.")
            } catch (e: Throwable) {
                notes.add("CameraExtensionCharacteristics querying returned exception: ${e.message}")
            }
        } else {
            notes.add("Camera extension detection not supported below Android 12 (API 31).")
        }

        return ExtensionProfile(
            supportsAuto = auto,
            supportsHdr = hdr,
            supportsNight = night,
            supportsBokeh = bokeh,
            supportsFaceRetouch = retouch,
            notes = notes
        )
    }

    private fun generateWarnings(
        level: HardwareLevel,
        facing: CameraFacing,
        caps: Set<CameraCapability>,
        lens: LensProfile,
        sensor: SensorProfile,
        photo: PhotoProfile,
        video: VideoProfile
    ): List<String> {
        val list = mutableListOf<String>()

        if (level == HardwareLevel.LEGACY) {
            list.add("Device runs in LEGACY hardware mode. Exposure configuration override capabilities are restricted by Android OS.")
        }
        if (level == HardwareLevel.LIMITED) {
            list.add("Device runs in LIMITED hardware level. Core capture performance and exposure flexibility are constrained.")
        }
        if (!caps.contains(CameraCapability.MANUAL_SENSOR)) {
            list.add("Manual sensor control (manual ISO overrides and exposure time overrides) is unsupported by hardware.")
        }
        if (!photo.supportsRaw) {
            list.add("RAW still capture (DNG representation) is unsupported on this lens pipeline.")
        }
        if (lens.minFocusDistance == null || lens.minFocusDistance == 0.0f) {
            list.add("Lens focus distance range not reported or fixed-focus lens configuration is active.")
        }
        if (sensor.isoRange == null) {
            list.add("Hardware sensor sensitivity range (ISO) was not returned by driver.")
        }
        if (sensor.exposureTimeRangeNs == null) {
            list.add("Hardware exposure time range was not returned by driver.")
        }
        if (!video.supportsHighSpeedVideo) {
            list.add("Constrained high-speed video (120/240 FPS slow-motion) is unsupported on this lens.")
        }

        return list
    }
}
