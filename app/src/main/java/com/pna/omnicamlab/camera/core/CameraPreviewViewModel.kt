package com.pna.omnicamlab.camera.core

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pna.omnicamlab.camera.capabilities.CameraDeviceProfile
import com.pna.omnicamlab.camera.capabilities.CameraFacing
import com.pna.omnicamlab.camera.capabilities.CameraSize
import com.pna.omnicamlab.camera.capabilities.CapabilityScanner
import com.pna.omnicamlab.util.logging.OmniLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CameraPreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = CapabilityScanner(application)
    private val sessionManager = Camera2SessionManager(application)

    private val _sessionState = MutableStateFlow<CameraSessionState>(CameraSessionState.Idle)
    val sessionState: StateFlow<CameraSessionState> = _sessionState.asStateFlow()

    private val _cameraProfiles = MutableStateFlow<List<CameraDeviceProfile>>(emptyList())
    val cameraProfiles: StateFlow<List<CameraDeviceProfile>> = _cameraProfiles.asStateFlow()

    private val _selectedCameraId = MutableStateFlow<String?>(null)
    val selectedCameraId: StateFlow<String?> = _selectedCameraId.asStateFlow()

    private var activeSurface: Surface? = null

    // Reactive states for debugging and lifecycle tracking
    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _lifecycleActive = MutableStateFlow(false)
    val lifecycleActive: StateFlow<Boolean> = _lifecycleActive.asStateFlow()

    private val _surfaceAvailable = MutableStateFlow(false)
    val surfaceAvailable: StateFlow<Boolean> = _surfaceAvailable.asStateFlow()

    private val _surfaceSize = MutableStateFlow("0x0")
    val surfaceSize: StateFlow<String> = _surfaceSize.asStateFlow()

    private val _selectedPreviewSize = MutableStateFlow<CameraSize>(CameraSize(1280, 720))
    val selectedPreviewSize: StateFlow<CameraSize> = _selectedPreviewSize.asStateFlow()

    private val _selectedPreviewSizeStr = MutableStateFlow("0x0")
    val selectedPreviewSizeStr: StateFlow<String> = _selectedPreviewSizeStr.asStateFlow()

    private val _cameraOpenRequestedCount = MutableStateFlow(0)
    val cameraOpenRequestedCount: StateFlow<Int> = _cameraOpenRequestedCount.asStateFlow()

    private val _cameraOpenedCallbackCount = MutableStateFlow(0)
    val cameraOpenedCallbackCount: StateFlow<Int> = _cameraOpenedCallbackCount.asStateFlow()

    private val _sessionConfiguredCount = MutableStateFlow(0)
    val sessionConfiguredCount: StateFlow<Int> = _sessionConfiguredCount.asStateFlow()

    private val _repeatingStarted = MutableStateFlow(false)
    val repeatingStarted: StateFlow<Boolean> = _repeatingStarted.asStateFlow()

    private val _frameUpdateCount = MutableStateFlow(0)
    val frameUpdateCount: StateFlow<Int> = _frameUpdateCount.asStateFlow()

    private val _lastBlockedOpenReason = MutableStateFlow("None")
    val lastBlockedOpenReason: StateFlow<String> = _lastBlockedOpenReason.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _sensorOrientationState = MutableStateFlow(0)
    val sensorOrientationState: StateFlow<Int> = _sensorOrientationState.asStateFlow()

    private val _displayRotationEnum = MutableStateFlow("ROTATION_0")
    val displayRotationEnum: StateFlow<String> = _displayRotationEnum.asStateFlow()

    private val _displayRotationDegrees = MutableStateFlow(0)
    val displayRotationDegrees: StateFlow<Int> = _displayRotationDegrees.asStateFlow()

    private val _computedPreviewRotationDegrees = MutableStateFlow(0)
    val computedPreviewRotationDegrees: StateFlow<Int> = _computedPreviewRotationDegrees.asStateFlow()

    private val _jpegOrientationDegrees = MutableStateFlow(0)
    val jpegOrientationDegrees: StateFlow<Int> = _jpegOrientationDegrees.asStateFlow()

    private val _forcedPreviewRotation = MutableStateFlow<Int?>(null)
    val forcedPreviewRotation: StateFlow<Int?> = _forcedPreviewRotation.asStateFlow()

    private val _forcedPreviewRotationDegrees = MutableStateFlow(-1)
    val forcedPreviewRotationDegrees: StateFlow<Int> = _forcedPreviewRotationDegrees.asStateFlow()

    private val _autoPreviewRotationDegrees = MutableStateFlow(0)
    val autoPreviewRotationDegrees: StateFlow<Int> = _autoPreviewRotationDegrees.asStateFlow()

    private val _activePreviewRotationDegrees = MutableStateFlow(0)
    val activePreviewRotationDegrees: StateFlow<Int> = _activePreviewRotationDegrees.asStateFlow()

    private val _transformSource = MutableStateFlow("AUTO_CAMERA2_BASIC")
    val transformSource: StateFlow<String> = _transformSource.asStateFlow()

    private val _mirrorForFrontCamera = MutableStateFlow(false)
    val mirrorForFrontCamera: StateFlow<Boolean> = _mirrorForFrontCamera.asStateFlow()

    // New states for Zoom, Aspect Ratio, and Rotation Refresh telemetry
    val zoomState: StateFlow<ZoomState> = sessionManager.zoomState
    
    private val _previewAspectMode = MutableStateFlow(PreviewAspectMode.PHOTO_WYSIWYG)
    val previewAspectMode: StateFlow<PreviewAspectMode> = _previewAspectMode.asStateFlow()

    private val _screenAspectRatio = MutableStateFlow(16f / 9f)
    val screenAspectRatio: StateFlow<Float> = _screenAspectRatio.asStateFlow()

    private val _jpegCaptureSize = MutableStateFlow(CameraSize(4096, 3072))
    val jpegCaptureSize: StateFlow<CameraSize> = _jpegCaptureSize.asStateFlow()

    private val _cameraOpenReason = MutableStateFlow("First open after boot")
    val cameraOpenReason: StateFlow<String> = _cameraOpenReason.asStateFlow()

    private val _sessionRecreateReason = MutableStateFlow("None")
    val sessionRecreateReason: StateFlow<String> = _sessionRecreateReason.asStateFlow()

    private val _transformOnlyUpdateCount = MutableStateFlow(0)
    val transformOnlyUpdateCount: StateFlow<Int> = _transformOnlyUpdateCount.asStateFlow()

    private val _cameraReopenCount = MutableStateFlow(0)
    val cameraReopenCount: StateFlow<Int> = _cameraReopenCount.asStateFlow()

    private val _transformUpdateReason = MutableStateFlow("INITIALIZED")
    val transformUpdateReason: StateFlow<String> = _transformUpdateReason.asStateFlow()

    val deviceOrientationDegrees: StateFlow<Int> = sessionManager.deviceOrientationDegrees
    val forcedJpegOrientation: StateFlow<Int?> = sessionManager.forcedJpegOrientation
    val jpegOrientationSource: StateFlow<String> = sessionManager.jpegOrientationSource

    private val _transformAppliedCount = MutableStateFlow(0)
    val transformAppliedCount: StateFlow<Int> = _transformAppliedCount.asStateFlow()

    private val _transformViewSize = MutableStateFlow("0x0")
    val transformViewSize: StateFlow<String> = _transformViewSize.asStateFlow()

    private val _transformBufferSize = MutableStateFlow("0x0")
    val transformBufferSize: StateFlow<String> = _transformBufferSize.asStateFlow()

    private val _transformScale = MutableStateFlow("1.0")
    val transformScale: StateFlow<String> = _transformScale.asStateFlow()

    private val _transformMode = MutableStateFlow("CENTER_CROP")
    val transformMode: StateFlow<String> = _transformMode.asStateFlow()

    private val _scaleMode = MutableStateFlow(PreviewScaleType.CENTER_CROP)
    val scaleMode: StateFlow<PreviewScaleType> = _scaleMode.asStateFlow()

    private val _scaleX = MutableStateFlow(1f)
    val scaleX: StateFlow<Float> = _scaleX.asStateFlow()

    private val _scaleY = MutableStateFlow(1f)
    val scaleY: StateFlow<Float> = _scaleY.asStateFlow()

    private val _uniformScale = MutableStateFlow(1f)
    val uniformScale: StateFlow<Float> = _uniformScale.asStateFlow()

    private val _effectiveBufferSize = MutableStateFlow("0x0")
    val effectiveBufferSize: StateFlow<String> = _effectiveBufferSize.asStateFlow()

    private val _scaledBufferSize = MutableStateFlow("0x0")
    val scaledBufferSize: StateFlow<String> = _scaledBufferSize.asStateFlow()

    private val _cropOrLetterboxDxDy = MutableStateFlow("0.0,0.0")
    val cropOrLetterboxDxDy: StateFlow<String> = _cropOrLetterboxDxDy.asStateFlow()

    private val _aspectRatioPreserved = MutableStateFlow(true)
    val aspectRatioPreserved: StateFlow<Boolean> = _aspectRatioPreserved.asStateFlow()

    private val _scaleModeState = MutableStateFlow("CENTER_CROP")
    val scaleModeState: StateFlow<String> = _scaleModeState.asStateFlow()


    @Volatile
    private var isOpening = false

    @Volatile
    private var isClosing = false

    init {
        loadCameraProfiles()
    }

    private fun loadCameraProfiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val profiles = scanner.scanCapabilities()
            withContext(Dispatchers.Main) {
                _cameraProfiles.value = profiles
                if (profiles.isNotEmpty()) {
                    // Default camera selection:
                    // 1. First BACK camera if available
                    // 2. Otherwise first available camera
                    val defaultProfile = profiles.find { it.facing == CameraFacing.BACK } ?: profiles.first()
                    _selectedCameraId.value = defaultProfile.cameraId
                    updateSelectedPreviewSize(defaultProfile.cameraId)
                    openIfReady()
                }
            }
        }
    }

    fun updateSelectedPreviewSize(
        cameraId: String,
        mode: PreviewAspectMode = _previewAspectMode.value,
        screenRatio: Float = _screenAspectRatio.value
    ) {
        try {
            val cameraManager = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfig = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            
            val privSizes = streamConfig?.getOutputSizes(android.graphics.ImageFormat.PRIVATE)?.map { CameraSize(it.width, it.height) } ?: emptyList()
            val jpegSizes = streamConfig?.getOutputSizes(android.graphics.ImageFormat.JPEG)?.map { CameraSize(it.width, it.height) } ?: emptyList()
            val bestJpegSize = JpegSizeSelector.selectBestJpegSize(jpegSizes)
            _jpegCaptureSize.value = bestJpegSize

            val selectedSize = PreviewSizeSelector.selectPreviewSize(
                availableSizes = privSizes,
                stillCaptureSize = bestJpegSize,
                aspectMode = mode,
                screenAspectRatio = screenRatio
            )
            _selectedPreviewSize.value = selectedSize
            _selectedPreviewSizeStr.value = "${selectedSize.width}x${selectedSize.height}"
        } catch (e: Exception) {
            OmniLogger.e(OmniLogger.Tag.Error, "Error determining preview size for $cameraId", e)
        }
    }

    fun setZoom(zoomVal: Float) {
        sessionManager.setZoom(zoomVal)
    }

    fun setPreviewAspectMode(mode: PreviewAspectMode, screenRatio: Float) {
        if (_previewAspectMode.value != mode || _screenAspectRatio.value != screenRatio) {
            val oldMode = _previewAspectMode.value
            _previewAspectMode.value = mode
            _screenAspectRatio.value = screenRatio
            
            val cameraId = _selectedCameraId.value
            if (cameraId != null) {
                val oldSize = _selectedPreviewSize.value
                updateSelectedPreviewSize(cameraId, mode, screenRatio)
                val newSize = _selectedPreviewSize.value
                
                if (oldSize != newSize) {
                    _sessionRecreateReason.value = "Aspect mode changed: $oldMode -> $mode (${oldSize.width}x${oldSize.height} -> ${newSize.width}x${newSize.height})"
                    _cameraReopenCount.value += 1
                    _cameraOpenReason.value = "Aspect mode size change"
                    
                    isClosing = true
                    viewModelScope.launch(Dispatchers.IO) {
                        sessionManager.closeCamera()
                        withContext(Dispatchers.Main) {
                            isClosing = false
                            openIfReady()
                        }
                    }
                } else {
                    _transformOnlyUpdateCount.value += 1
                }
            }
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        if (_permissionGranted.value != granted) {
            _permissionGranted.value = granted
            openIfReady()
        }
    }

    fun setLifecycleActive(active: Boolean) {
        if (_lifecycleActive.value != active) {
            _lifecycleActive.value = active
            if (active) {
                openIfReady()
            } else {
                closeCamera()
            }
        }
    }

    private fun setSurfaceAvailable(available: Boolean, width: Int, height: Int) {
        if (_surfaceAvailable.value != available || _surfaceSize.value != "${width}x${height}") {
            _surfaceAvailable.value = available
            _surfaceSize.value = "${width}x${height}"
            if (available) {
                openIfReady()
            } else {
                closeCamera()
            }
        }
    }

    fun setSurface(surface: Surface?, width: Int = 0, height: Int = 0) {
        activeSurface = surface
        setSurfaceAvailable(surface != null, width, height)
    }

    fun incrementFrameUpdateCount() {
        _frameUpdateCount.value += 1
    }

    fun updateTransformDebugInfo(
        sensorOri: Int,
        dispRotEnum: String,
        dispRotDeg: Int,
        autoPrevRot: Int,
        forcedPrevRot: Int,
        activePrevRot: Int,
        source: String,
        mirror: Boolean,
        viewWidth: Int,
        viewHeight: Int,
        bufferWidth: Int,
        bufferHeight: Int,
        scaleXVal: Float,
        scaleYVal: Float,
        uniformScaleVal: Float,
        effectiveW: Float,
        effectiveH: Float,
        scaledW: Float,
        scaledH: Float,
        dx: Float,
        dy: Float,
        aspectRatioOk: Boolean,
        mode: String,
        jpegOri: Int,
        reason: String = "UNKNOWN"
    ) {
        _transformUpdateReason.value = reason
        _sensorOrientationState.value = sensorOri
        _displayRotationEnum.value = dispRotEnum
        _displayRotationDegrees.value = dispRotDeg
        _autoPreviewRotationDegrees.value = autoPrevRot
        _forcedPreviewRotationDegrees.value = forcedPrevRot
        _activePreviewRotationDegrees.value = activePrevRot
        _computedPreviewRotationDegrees.value = activePrevRot
        _transformSource.value = source
        _jpegOrientationDegrees.value = jpegOri
        _mirrorForFrontCamera.value = mirror
        _transformAppliedCount.value += 1
        _transformViewSize.value = "${viewWidth}x${viewHeight}"
        _transformBufferSize.value = "${bufferWidth}x${bufferHeight}"

        _scaleX.value = scaleXVal
        _scaleY.value = scaleYVal
        _uniformScale.value = uniformScaleVal
        _effectiveBufferSize.value = "${effectiveW.toInt()}x${effectiveH.toInt()}"
        _scaledBufferSize.value = "${scaledW.toInt()}x${scaledH.toInt()}"
        _cropOrLetterboxDxDy.value = String.format(java.util.Locale.US, "%.1f,%.1f", dx, dy)
        _aspectRatioPreserved.value = aspectRatioOk
        _scaleModeState.value = mode

        _transformScale.value = String.format(java.util.Locale.US, "%.4f", uniformScaleVal)
        _transformMode.value = mode
    }

    fun setForcedPreviewRotation(rotation: Int?) {
        _forcedPreviewRotation.value = rotation
        _forcedPreviewRotationDegrees.value = rotation ?: -1
    }

    fun setForcedJpegOrientation(override: Int?) {
        sessionManager.setForcedJpegOrientation(override)
    }

    fun setScaleMode(mode: PreviewScaleType) {
        _scaleMode.value = mode
        _scaleModeState.value = mode.name
    }


    fun openIfReady() {
        val permission = _permissionGranted.value
        val lifecycle = _lifecycleActive.value
        val cameraId = _selectedCameraId.value
        val surfaceOk = _surfaceAvailable.value
        val opening = isOpening
        val closing = isClosing
        val state = _sessionState.value
        val capturing = state is CameraSessionState.Capturing || state is CameraSessionState.Saving
        val alreadyOpenOrOpening = state is CameraSessionState.Previewing || state is CameraSessionState.Loading

        val canOpen = permission &&
                      lifecycle &&
                      cameraId != null &&
                      surfaceOk &&
                      !opening &&
                      !closing &&
                      !alreadyOpenOrOpening &&
                      !capturing

        val surfaceSizeVal = _surfaceSize.value
        val previewSizeVal = _selectedPreviewSizeStr.value

        android.util.Log.i("OmniCamPreview", "openIfReady()\n" +
                "permissionGranted=$permission\n" +
                "lifecycleActive=$lifecycle\n" +
                "selectedCameraId=$cameraId\n" +
                "surfaceAvailable=$surfaceOk\n" +
                "surfaceSize=$surfaceSizeVal\n" +
                "previewSize=$previewSizeVal\n" +
                "isOpening=$opening\n" +
                "isClosing=$closing\n" +
                "currentState=$state\n" +
                "isCapturing=$capturing\n" +
                "alreadyOpenOrOpening=$alreadyOpenOrOpening")

        if (canOpen && cameraId != null) {
            val surface = activeSurface
            if (surface == null) {
                _lastBlockedOpenReason.value = "Surface is null even though surfaceAvailable is true"
                android.util.Log.w("OmniCamPreview", "ACTION=blocked, reason=surface is null")
                return
            }
            android.util.Log.i("OmniCamPreview", "ACTION=openCamera($cameraId)")
            _cameraOpenRequestedCount.value += 1
            isOpening = true
            _lastBlockedOpenReason.value = "None"
            if (_cameraOpenReason.value == "None" || _cameraOpenReason.value.isEmpty()) {
                _cameraOpenReason.value = "Camera start/Engine boot"
            }
            
            viewModelScope.launch(Dispatchers.IO) {
                sessionManager.openCamera(cameraId, _selectedPreviewSize.value, surface) { sessionState ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _sessionState.value = sessionState
                        when (sessionState) {
                            is CameraSessionState.Previewing -> {
                                isOpening = false
                                _cameraOpenedCallbackCount.value += 1
                                _sessionConfiguredCount.value += 1
                                _repeatingStarted.value = true
                                android.util.Log.i("OmniCamPreview", "CameraDevice.onOpened and CaptureSession onConfigured & setRepeatingRequest succeeded")
                            }
                            is CameraSessionState.Error -> {
                                isOpening = false
                                _lastError.value = sessionState.message
                                android.util.Log.e("OmniCamPreview", "Camera open/session error: ${sessionState.message}")
                            }
                            is CameraSessionState.Loading -> {
                                // Still loading
                            }
                            else -> {
                                // Other intermediate states
                            }
                        }
                    }
                }
            }
        } else {
            val blockedReason = when {
                !permission -> "permissionGranted=false"
                !lifecycle -> "lifecycleActive=false"
                cameraId == null -> "selectedCameraId=null"
                !surfaceOk -> "surfaceAvailable=false"
                opening -> "isOpening=true"
                closing -> "isClosing=true"
                alreadyOpenOrOpening -> "alreadyOpenOrOpening=true"
                capturing -> "isCapturing=true"
                else -> "Unknown reason"
            }
            _lastBlockedOpenReason.value = blockedReason
            android.util.Log.i("OmniCamPreview", "ACTION=blocked, reason=$blockedReason")
        }
    }

    fun startCameraPreview() {
        openIfReady()
    }

    fun closeCamera() {
        android.util.Log.i("OmniCamPreview", "closeCamera() requested")
        isClosing = true
        _repeatingStarted.value = false
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.closeCamera()
            withContext(Dispatchers.Main) {
                isClosing = false
                _sessionState.value = CameraSessionState.Idle
                android.util.Log.i("OmniCamPreview", "closeCamera() completed, state set to Idle")
            }
        }
    }

    fun stopCameraPreview() {
        closeCamera()
    }

    fun switchCamera() {
        val profiles = _cameraProfiles.value
        val currentId = _selectedCameraId.value
        val surface = activeSurface

        if (profiles.size <= 1 || currentId == null || surface == null) return

        val currentIndex = profiles.indexOfFirst { it.cameraId == currentId }
        val nextIndex = (currentIndex + 1) % profiles.size
        val nextId = profiles[nextIndex].cameraId

        OmniLogger.i(OmniLogger.Tag.CameraSession, "Switching camera from $currentId to $nextId")
        android.util.Log.i("OmniCamPreview", "switchCamera() requested from $currentId to $nextId")

        // 1. Transition state to Switching
        _sessionState.value = CameraSessionState.Switching(currentId, nextId)
        _selectedCameraId.value = nextId
        updateSelectedPreviewSize(nextId)

        // 2. Perform switch safely: fully close current session, then open new camera
        isClosing = true
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.closeCamera()
            withContext(Dispatchers.Main) {
                isClosing = false
                openIfReady()
            }
        }
    }

    fun takePicture(deviceOrientation: Int) {
        OmniLogger.i(OmniLogger.Tag.CameraSession, "Shutter triggered with device orientation: $deviceOrientation")
        android.util.Log.i("OmniCamPreview", "takePicture() called with orientation: $deviceOrientation")
        sessionManager.takePicture(deviceOrientation)
    }

    override fun onCleared() {
        super.onCleared()
        // Guarantee release on ViewModel destruction
        sessionManager.closeCamera()
    }
}
