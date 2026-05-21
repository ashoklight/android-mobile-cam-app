package com.pna.omnicamlab.camera.capabilities

data class CameraDeviceProfile(
    val cameraId: String,
    val facing: CameraFacing,
    val hardwareLevel: HardwareLevel,
    val capabilities: Set<CameraCapability>,
    val lensProfile: LensProfile,
    val sensorProfile: SensorProfile,
    val photoProfile: PhotoProfile,
    val videoProfile: VideoProfile,
    val extensionProfile: ExtensionProfile,
    val supportWarnings: List<String>,
    val isLogicalMultiCamera: Boolean,
    val physicalCameraIds: List<String>,
    val openableCameraId: Boolean = true
)

data class LensProfile(
    val focalLengths: List<Float>,
    val apertures: List<Float>,
    val minFocusDistance: Float?,
    val focusDistanceCalibration: String?,
    val opticalStabilizationModes: List<String>,
    val availableAfModes: List<String>,
    val lensFacingRaw: Int?,
    val zoomRatioRange: String? = null
)

data class SensorProfile(
    val isoRange: String?,
    val exposureTimeRangeNs: String?,
    val maxFrameDurationNs: Long?,
    val activeArraySize: String?,
    val pixelArraySize: String?,
    val sensorOrientation: Int?,
    val colorFilterArrangement: String?,
    val timestampSource: String?
)

data class PhotoProfile(
    val jpegSizes: List<String>,
    val rawSizes: List<String>,
    val yuvSizes: List<String>,
    val privateSizes: List<String>,
    val supportsRaw: Boolean,
    val supportsBurst: Boolean,
    val largestJpeg: String?,
    val largestRaw: String?,
    val largestYuv: String?
)

data class VideoProfile(
    val fpsRanges: List<String>,
    val highSpeedVideoSizes: List<String>,
    val highSpeedFpsRanges: List<String>,
    val stabilizationModes: List<String>,
    val supportsHighSpeedVideo: Boolean
)

data class ExtensionProfile(
    val supportsAuto: Boolean,
    val supportsHdr: Boolean,
    val supportsNight: Boolean,
    val supportsBokeh: Boolean,
    val supportsFaceRetouch: Boolean,
    val notes: List<String>
)
