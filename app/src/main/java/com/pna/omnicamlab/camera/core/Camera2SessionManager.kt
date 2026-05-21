package com.pna.omnicamlab.camera.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.pna.omnicamlab.camera.capabilities.CapabilityScanner
import com.pna.omnicamlab.camera.capabilities.CameraSize
import com.pna.omnicamlab.util.logging.OmniLogger
import com.pna.omnicamlab.camera.capabilities.CameraDeviceProfile
import com.pna.omnicamlab.data.media.MediaStorePhotoSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class Camera2SessionManager(private val context: Context) {

    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    // Zoom and orientation tracking fields
    private val deviceOrientationMonitor by lazy { DeviceOrientationMonitor(context) }
    val deviceOrientationDegrees: StateFlow<Int> = deviceOrientationMonitor.orientation

    private var zoomController: ZoomController? = null
    private val _zoomState = MutableStateFlow(ZoomState())
    val zoomState: StateFlow<ZoomState> = _zoomState.asStateFlow()

    @Volatile
    private var currentZoom = 1.0f

    private val _forcedJpegOrientation = MutableStateFlow<Int?>(null)
    val forcedJpegOrientation: StateFlow<Int?> = _forcedJpegOrientation.asStateFlow()

    private val _jpegOrientationSource = MutableStateFlow("DEVICE_ORIENTATION_AUTO")
    val jpegOrientationSource: StateFlow<String> = _jpegOrientationSource.asStateFlow()

    // Thread management
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    // Camera device and capture session
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSurface: Surface? = null

    // Still JPEG capture components
    private var imageReader: ImageReader? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var isCapturing = false

    @Volatile
    private var pendingImageBytes: ByteArray? = null

    @Volatile
    private var pendingCaptureResult: TotalCaptureResult? = null

    @Volatile
    private var isCaptureCompleted = false

    // Active session details saved to preserve viewfinder Compose UI during capture
    private var activeProfile: CameraDeviceProfile? = null
    private var activePreviewSize: CameraSize? = null

    // Synchronization lock to prevent race conditions during camera opening and closing
    private val cameraOpenCloseLock = Semaphore(1)

    // Current camera ID that we are trying to manage/open
    @Volatile
    private var currentCameraId: String? = null

    // Track active state change callback
    private var stateCallback: ((CameraSessionState) -> Unit)? = null

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
            OmniLogger.d(OmniLogger.Tag.CameraSession, "Background thread started")
        }
    }

    private fun stopBackgroundThread() {
        val thread = backgroundThread
        backgroundThread = null
        backgroundHandler = null

        thread?.let { t ->
            t.quitSafely()
            try {
                t.join(1000)
            } catch (e: InterruptedException) {
                OmniLogger.e(OmniLogger.Tag.Error, "Interrupted while stopping background thread", e)
            }
            OmniLogger.d(OmniLogger.Tag.CameraSession, "Background thread stopped")
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(
        cameraId: String,
        previewSize: CameraSize,
        surface: Surface,
        onStateChanged: (CameraSessionState) -> Unit
    ) {
        stateCallback = onStateChanged
        currentCameraId = cameraId
        previewSurface = surface
        activePreviewSize = previewSize

        deviceOrientationMonitor.start()

        startBackgroundThread()

        backgroundHandler?.post {
            var acquired = false
            try {
                acquired = cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)
                if (!acquired) {
                    OmniLogger.e(OmniLogger.Tag.Error, "Timeout waiting to lock camera for opening")
                    notifyError(CameraError.Unknown(RuntimeException("Timeout acquiring camera lock")), "Timeout acquiring camera lock")
                    return@post
                }

                // If the user requested another camera ID in the meantime, abort opening
                if (currentCameraId != cameraId) {
                    OmniLogger.w(OmniLogger.Tag.CameraSession, "Stale cameraId $cameraId requested to open, current is $currentCameraId. Aborting.")
                    return@post
                }

                OmniLogger.i(OmniLogger.Tag.CameraSession, "Opening camera: $cameraId")
                notifyState(CameraSessionState.Loading(cameraId))

                // Configure ImageReader for still JPEG capture
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val streamConfig = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val jpegSizes = streamConfig?.getOutputSizes(ImageFormat.JPEG)?.map { CameraSize(it.width, it.height) } ?: emptyList()
                val bestJpegSize = JpegSizeSelector.selectBestJpegSize(jpegSizes)

                OmniLogger.i(OmniLogger.Tag.CameraSession, "Configuring ImageReader with JPEG size: $bestJpegSize")
                imageReader = ImageReader.newInstance(bestJpegSize.width, bestJpegSize.height, ImageFormat.JPEG, 1).apply {
                    setOnImageAvailableListener({ reader ->
                        val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                        backgroundHandler?.post {
                            try {
                                val buffer = image.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                pendingImageBytes = bytes
                                checkCaptureReady()
                            } catch (e: Exception) {
                                OmniLogger.e(OmniLogger.Tag.Error, "Error reading JPEG image bytes", e)
                            } finally {
                                image.close()
                            }
                        }
                    }, backgroundHandler)
                }

                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        OmniLogger.i(OmniLogger.Tag.CameraSession, "Camera device opened: ${camera.id}")
                        
                        // Guard against stale callback
                        if (currentCameraId != camera.id) {
                            OmniLogger.w(OmniLogger.Tag.CameraSession, "Stale onOpened callback for camera ${camera.id}, closing immediately.")
                            camera.close()
                            return
                        }

                        cameraDevice = camera

                        // Setup ZoomController for this camera characteristics
                        val chars = cameraManager.getCameraCharacteristics(camera.id)
                        zoomController = ZoomController(chars)
                        _zoomState.value = zoomController?.zoomState?.copy(currentZoom = currentZoom) ?: ZoomState()
                        
                        // Proceed to setup preview session
                        createPreviewSession()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        OmniLogger.w(OmniLogger.Tag.CameraSession, "Camera device disconnected: ${camera.id}")
                        if (currentCameraId == camera.id) {
                            closeCamera()
                            notifyState(CameraSessionState.Idle)
                        } else {
                            camera.close()
                        }
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        OmniLogger.e(OmniLogger.Tag.Error, "Camera device error: ${camera.id}, code: $error")
                        val mappedError = when (error) {
                            ERROR_CAMERA_IN_USE -> CameraError.CameraInUse
                            ERROR_MAX_CAMERAS_IN_USE -> CameraError.MaxCamerasInUse
                            ERROR_CAMERA_DISABLED -> CameraError.CameraDisabled
                            ERROR_CAMERA_DEVICE -> CameraError.CameraFatalError
                            ERROR_CAMERA_SERVICE -> CameraError.CameraFatalError
                            else -> CameraError.Unknown(null)
                        }
                        
                        if (currentCameraId == camera.id) {
                            closeCamera()
                            notifyError(mappedError, "Camera device error code: $error")
                        } else {
                            camera.close()
                        }
                    }
                }, backgroundHandler)

            } catch (e: SecurityException) {
                OmniLogger.e(OmniLogger.Tag.Error, "Security exception opening camera $cameraId", e)
                notifyError(CameraError.PermissionMissing, e.message ?: "Permission missing")
            } catch (e: Exception) {
                OmniLogger.e(OmniLogger.Tag.Error, "Exception opening camera $cameraId", e)
                notifyError(CameraError.Unknown(e), e.message ?: "Failed to open camera")
            } finally {
                if (acquired) {
                    cameraOpenCloseLock.release()
                }
            }
        }
    }

    private fun createPreviewSession() {
        val device = cameraDevice
        val surface = previewSurface
        val handler = backgroundHandler
        val cameraId = currentCameraId

        if (device == null || surface == null || handler == null || cameraId == null) {
            OmniLogger.e(OmniLogger.Tag.Error, "Cannot create preview session: missing active resources")
            notifyError(CameraError.SessionConfigurationFailed, "Resources missing for session setup")
            return
        }

        handler.post {
            var acquired = false
            try {
                acquired = cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)
                if (!acquired) {
                    OmniLogger.e(OmniLogger.Tag.Error, "Timeout waiting to lock camera for session configuration")
                    notifyError(CameraError.SessionConfigurationFailed, "Timeout configuring preview capture session")
                    return@post
                }

                // Double check active camera matches
                if (cameraDevice?.id != cameraId || currentCameraId != cameraId) {
                    OmniLogger.w(OmniLogger.Tag.CameraSession, "Mismatch in camera device or ID during capture session setup, aborting.")
                    return@post
                }

                OmniLogger.i(OmniLogger.Tag.CameraSession, "Creating capture session for camera $cameraId")

                val readerSurface = imageReader?.surface
                val surfaces = if (readerSurface != null) {
                    listOf(surface, readerSurface)
                } else {
                    listOf(surface)
                }
                
                device.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        OmniLogger.i(OmniLogger.Tag.CameraSession, "Capture session configured: ${session.device.id}")

                        if (currentCameraId != session.device.id || cameraDevice == null) {
                            OmniLogger.w(OmniLogger.Tag.CameraSession, "Stale session configured callback, closing session.")
                            session.close()
                            return
                        }

                        captureSession = session

                        startPreviewRepeatingRequest()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        OmniLogger.e(OmniLogger.Tag.Error, "Capture session configuration failed for camera ${session.device.id}")
                        notifyError(CameraError.SessionConfigurationFailed, "Session configuration failed")
                    }
                }, backgroundHandler)

            } catch (e: Exception) {
                OmniLogger.e(OmniLogger.Tag.Error, "Exception creating capture session for $cameraId", e)
                notifyError(CameraError.Unknown(e), e.message ?: "Failed to configure capture session")
            } finally {
                if (acquired) {
                    cameraOpenCloseLock.release()
                }
            }
        }
    }

    private fun startPreviewRepeatingRequest() {
        val device = cameraDevice
        val session = captureSession
        val surface = previewSurface
        val handler = backgroundHandler
        val cameraId = currentCameraId

        if (device == null || session == null || surface == null || handler == null || cameraId == null) {
            OmniLogger.w(OmniLogger.Tag.CameraSession, "Cannot start repeating request: missing capture session dependencies")
            return
        }

        handler.post {
            try {
                OmniLogger.i(OmniLogger.Tag.CameraSession, "Starting preview repeating request for $cameraId")
                val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(surface)
                }

                // Disable any manual controls or special overrides for preview:
                // Set default auto focus, auto exposure, auto white balance
                requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

                // Apply zoom ratio if ZoomController is configured
                zoomController?.applyZoom(requestBuilder, currentZoom)

                session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)

                // Dynamic retrieval of profiles for status display
                val profiles = CapabilityScanner(context).scanCapabilities()
                val profile = profiles.find { it.cameraId == cameraId }

                if (profile != null) {
                    val size = activePreviewSize ?: CameraSize(1280, 720)
                    activeProfile = profile
                    notifyState(CameraSessionState.Previewing(cameraId, profile, size))
                } else {
                    notifyError(CameraError.Unknown(null), "Failed to match camera profile for ID: $cameraId")
                }
            } catch (e: Exception) {
                OmniLogger.e(OmniLogger.Tag.Error, "Exception starting preview repeating request", e)
                notifyError(CameraError.Unknown(e), e.message ?: "Failed to start preview repeating request")
            }
        }
    }

    /**
     * Fully idempotent closeCamera operation. Safely releases all Camera2 capture sessions,
     * devices, handles resource release, cancels repeating, and stops background threads.
     */
    fun closeCamera() {
        val threadToStop = backgroundThread
        val handler = backgroundHandler

        backgroundThread = null
        backgroundHandler = null

        val runnable = Runnable {
            try {
                cameraOpenCloseLock.acquire()
                OmniLogger.i(OmniLogger.Tag.CameraSession, "Closing camera resources idempotently...")

                // 1. Stop repeating capture requests
                try {
                    captureSession?.stopRepeating()
                    captureSession?.abortCaptures()
                } catch (e: Exception) {
                    OmniLogger.w(OmniLogger.Tag.CameraSession, "Warning when stopping repeating request: ${e.message}")
                }

                // 2. Close capture session
                try {
                    captureSession?.close()
                } catch (e: Exception) {
                    OmniLogger.w(OmniLogger.Tag.CameraSession, "Warning when closing capture session: ${e.message}")
                }
                captureSession = null

                // 3. Close camera device
                try {
                    cameraDevice?.close()
                } catch (e: Exception) {
                    OmniLogger.w(OmniLogger.Tag.CameraSession, "Warning when closing camera device: ${e.message}")
                }
                cameraDevice = null

                // 3.5 Close ImageReader
                try {
                    imageReader?.close()
                } catch (e: Exception) {
                    OmniLogger.w(OmniLogger.Tag.CameraSession, "Warning when closing ImageReader: ${e.message}")
                }
                imageReader = null

                // 4. Release preview surface and capture state
                previewSurface = null
                currentCameraId = null
                isCapturing = false
                pendingImageBytes = null
                pendingCaptureResult = null
                isCaptureCompleted = false
                activeProfile = null
                activePreviewSize = null
                zoomController = null
                deviceOrientationMonitor.stop()

                OmniLogger.i(OmniLogger.Tag.CameraSession, "Camera resources fully closed")
            } catch (e: Exception) {
                OmniLogger.e(OmniLogger.Tag.Error, "Exception closing camera", e)
            } finally {
                cameraOpenCloseLock.release()
            }
        }

        if (handler != null) {
            handler.post(runnable)
            // Wait for thread to finish remaining queue and exit safely
            threadToStop?.let { t ->
                t.quitSafely()
                try {
                    t.join(1000)
                } catch (e: InterruptedException) {
                    OmniLogger.e(OmniLogger.Tag.Error, "Interrupted while stopping background thread", e)
                }
                OmniLogger.d(OmniLogger.Tag.CameraSession, "Background thread stopped")
            }
        } else {
            // Background thread is already null, just execute synchronously
            runnable.run()
        }
    }

    private fun notifyState(state: CameraSessionState) {
        stateCallback?.let { callback ->
            callback(state)
        }
    }

    private fun notifyError(error: CameraError, msg: String) {
        notifyState(CameraSessionState.Error(error, msg))
    }

    fun setZoom(zoomVal: Float) {
        val controller = zoomController ?: return
        val clamped = controller.clampZoom(zoomVal)
        currentZoom = clamped
        _zoomState.value = _zoomState.value.copy(currentZoom = clamped)

        val session = captureSession
        val device = cameraDevice
        val surface = previewSurface
        val handler = backgroundHandler

        if (session != null && device != null && surface != null && handler != null) {
            handler.post {
                try {
                    val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(surface)
                    }
                    requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

                    // Apply zoom using ZoomController
                    controller.applyZoom(requestBuilder, clamped)

                    session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                } catch (e: Exception) {
                    OmniLogger.e(OmniLogger.Tag.Error, "Error applying zoom dynamically", e)
                }
            }
        }
    }

    fun setForcedJpegOrientation(override: Int?) {
        _forcedJpegOrientation.value = override
        _jpegOrientationSource.value = if (override == null) "DEVICE_ORIENTATION_AUTO" else "FORCED_${override}"
    }

    fun takePicture(deviceOrientation: Int) {
        val session = captureSession
        val device = cameraDevice
        val surface = previewSurface
        val reader = imageReader
        val cameraId = currentCameraId
        val profile = activeProfile
        val previewSize = activePreviewSize

        if (session == null || device == null || surface == null || reader == null || cameraId == null || profile == null || previewSize == null) {
            OmniLogger.e(OmniLogger.Tag.Error, "Cannot take picture: active camera resources are missing or session not fully configured.")
            return
        }

        if (isCapturing) {
            OmniLogger.w(OmniLogger.Tag.CameraSession, "Capture already in progress, ignoring duplicate shutter request.")
            return
        }
        isCapturing = true

        notifyState(CameraSessionState.Capturing(cameraId, profile, previewSize))

        backgroundHandler?.post {
            try {
                pendingImageBytes = null
                pendingCaptureResult = null
                isCaptureCompleted = false

                val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(reader.surface)
                    addTarget(surface)
                }

                // Calculate rotation using DeviceOrientationMonitor or forced override
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val isFrontCamera = lensFacing == CameraCharacteristics.LENS_FACING_FRONT

                val deviceOri = _forcedJpegOrientation.value ?: deviceOrientationMonitor.orientation.value
                val jpegRotation = CameraOrientationHelper.calculateJpegOrientation(sensorOrientation, deviceOri, isFrontCamera)
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegRotation)

                // High quality still auto modes
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

                // Apply active zoom to the still capture request
                zoomController?.applyZoom(captureBuilder, currentZoom)

                session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        OmniLogger.i(OmniLogger.Tag.CameraSession, "Still capture request completed for $cameraId")
                        pendingCaptureResult = result
                        isCaptureCompleted = true
                        checkCaptureReady()
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        OmniLogger.e(OmniLogger.Tag.Error, "Still capture request failed for $cameraId")
                        pendingCaptureResult = null
                        isCaptureCompleted = true
                        checkCaptureReady()
                    }
                }, backgroundHandler)

            } catch (e: Exception) {
                OmniLogger.e(OmniLogger.Tag.Error, "Exception during still capture", e)
                isCapturing = false
                startPreviewRepeatingRequest()
                notifyState(CameraSessionState.CaptureError(cameraId, profile, previewSize, e.message ?: "Capture request failed"))
            }
        }
    }

    private fun checkCaptureReady() {
        val bytes = pendingImageBytes
        val completed = isCaptureCompleted
        val cameraId = currentCameraId
        val profile = activeProfile
        val previewSize = activePreviewSize

        if (bytes != null && completed && cameraId != null && profile != null && previewSize != null) {
            val result = pendingCaptureResult

            // Reset pending capture state
            pendingImageBytes = null
            pendingCaptureResult = null
            isCaptureCompleted = false

            // Asynchronously save photo to MediaStore on background coroutine context
            scope.launch {
                try {
                    notifyState(CameraSessionState.Saving(cameraId, profile, previewSize))
                    val timestampMs = System.currentTimeMillis()
                    val saveResult = MediaStorePhotoSaver.savePhoto(context, bytes, cameraId, timestampMs)

                    isCapturing = false
                    startPreviewRepeatingRequest()

                    // Extract available capture metadata
                    val iso = result?.get(TotalCaptureResult.SENSOR_SENSITIVITY)
                    val exposureTimeNs = result?.get(TotalCaptureResult.SENSOR_EXPOSURE_TIME)
                    val focalLength = result?.get(TotalCaptureResult.LENS_FOCAL_LENGTH)
                    val aperture = result?.get(TotalCaptureResult.LENS_APERTURE)

                    notifyState(CameraSessionState.CaptureSuccess(
                        cameraId = cameraId,
                        profile = profile,
                        previewSize = previewSize,
                        savedUri = saveResult.uri.toString(),
                        iso = iso,
                        exposureTimeNs = exposureTimeNs,
                        focalLength = focalLength,
                        aperture = aperture
                    ))
                } catch (e: Exception) {
                    OmniLogger.e(OmniLogger.Tag.Error, "Failed to save captured photo", e)
                    isCapturing = false
                    startPreviewRepeatingRequest()
                    notifyState(CameraSessionState.CaptureError(cameraId, profile, previewSize, e.message ?: "Failed to save photo"))
                }
            }
        }
    }
}
