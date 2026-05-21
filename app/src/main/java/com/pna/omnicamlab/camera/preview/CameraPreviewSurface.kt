package com.pna.omnicamlab.camera.preview

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.pna.omnicamlab.camera.capabilities.CameraSize
import com.pna.omnicamlab.camera.core.CameraPreviewViewModel
import com.pna.omnicamlab.camera.core.PreviewTransformHelper
import com.pna.omnicamlab.camera.core.PreviewScaleType
import com.pna.omnicamlab.util.logging.OmniLogger

@Composable
fun CameraPreviewSurface(
    previewSize: CameraSize,
    sensorOrientation: Int?,
    isFrontCamera: Boolean,
    viewModel: CameraPreviewViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val windowManager = remember(context) { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    val configuration = LocalConfiguration.current
    val currentOrientation = configuration.orientation
    val forcedRotation by viewModel.forcedPreviewRotation.collectAsState()
    val scaleMode by viewModel.scaleMode.collectAsState()
    val selectedCameraId by viewModel.selectedCameraId.collectAsState()

    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var viewWidth by remember { mutableStateOf(0) }
    var viewHeight by remember { mutableStateOf(0) }

    // Remember previous structural states to detect exact change reasons
    var prevOrientation by remember { mutableStateOf(currentOrientation) }
    var prevCameraId by remember { mutableStateOf(selectedCameraId) }
    var prevScaleMode by remember { mutableStateOf(scaleMode) }
    var prevForcedRotation by remember { mutableStateOf(forcedRotation) }

    // LaunchedEffect to detect structural configuration / layout changes with debounce
    LaunchedEffect(
        viewWidth,
        viewHeight,
        currentOrientation,
        selectedCameraId,
        scaleMode,
        forcedRotation,
        previewSize,
        textureViewRef
    ) {
        val tv = textureViewRef ?: return@LaunchedEffect
        if (viewWidth <= 0 || viewHeight <= 0) return@LaunchedEffect

        // Determine transition reason
        val reason = when {
            prevCameraId != selectedCameraId -> {
                prevCameraId = selectedCameraId
                "CAMERA_SWITCHED"
            }
            prevScaleMode != scaleMode -> {
                prevScaleMode = scaleMode
                "SCALE_MODE_CHANGED"
            }
            prevForcedRotation != forcedRotation -> {
                prevForcedRotation = forcedRotation
                "FORCED_ROTATION_CHANGED"
            }
            prevOrientation != currentOrientation -> {
                prevOrientation = currentOrientation
                "CONFIG_ORIENTATION_CHANGED"
            }
            else -> "DISPLAY_ROTATION_CHANGED"
        }

        // Apply a 150ms debounce to avoid transient warping during rotation transitions
        if (reason == "CONFIG_ORIENTATION_CHANGED" || reason == "DISPLAY_ROTATION_CHANGED" || reason == "SURFACE_SIZE_CHANGED") {
            kotlinx.coroutines.delay(150)
        }

        val stableRotation = windowManager.defaultDisplay.rotation
        applyTransform(
            textureView = tv,
            previewSize = previewSize,
            sensorOrientation = sensorOrientation,
            isFrontCamera = isFrontCamera,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            viewModel = viewModel,
            forcedRotation = forcedRotation,
            scaleMode = scaleMode,
            reason = reason,
            displayRotOverride = stableRotation
        )
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                textureViewRef = this
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        OmniLogger.i(OmniLogger.Tag.CameraSession, "SurfaceTexture available: ${width}x${height}")
                        android.util.Log.i("OmniCamPreview", "SurfaceTexture available: ${width}x${height}")
                        
                        viewWidth = width
                        viewHeight = height
                        
                        applyTransform(
                            textureView = this@apply, 
                            previewSize = previewSize, 
                            sensorOrientation = sensorOrientation, 
                            isFrontCamera = isFrontCamera, 
                            viewWidth = width, 
                            viewHeight = height, 
                            viewModel = viewModel, 
                            forcedRotation = forcedRotation,
                            scaleMode = scaleMode,
                            reason = "SURFACE_AVAILABLE",
                            displayRotOverride = windowManager.defaultDisplay.rotation
                        )
                        
                        viewModel.setSurface(Surface(st), width, height)
                        viewModel.openIfReady()
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                        viewWidth = width
                        viewHeight = height
                        
                        applyTransform(
                            textureView = this@apply, 
                            previewSize = previewSize, 
                            sensorOrientation = sensorOrientation, 
                            isFrontCamera = isFrontCamera, 
                            viewWidth = width, 
                            viewHeight = height, 
                            viewModel = viewModel, 
                            forcedRotation = forcedRotation,
                            scaleMode = scaleMode,
                            reason = "SURFACE_SIZE_CHANGED",
                            displayRotOverride = windowManager.defaultDisplay.rotation
                        )
                    }

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        OmniLogger.i(OmniLogger.Tag.CameraSession, "SurfaceTexture destroyed")
                        android.util.Log.i("OmniCamPreview", "SurfaceTexture destroyed")
                        textureViewRef = null
                        viewModel.setSurface(null, 0, 0)
                        viewModel.closeCamera()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {
                        viewModel.incrementFrameUpdateCount()
                    }
                }
            }
        },
        update = { textureView ->
            // Layout is managed by onGloballyPositioned and LaunchedEffect.
            // AndroidView update runs on recomposition, but LaunchedEffect handles all structural changes.
        },
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size
                if (size.width > 0 && size.height > 0) {
                    viewWidth = size.width
                    viewHeight = size.height
                }
            }
    )
}

private fun applyTransform(
    textureView: TextureView,
    previewSize: CameraSize,
    sensorOrientation: Int?,
    isFrontCamera: Boolean,
    viewWidth: Int,
    viewHeight: Int,
    viewModel: CameraPreviewViewModel,
    forcedRotation: Int?,
    scaleMode: PreviewScaleType,
    reason: String,
    displayRotOverride: Int
) {
    if (viewWidth <= 0 || viewHeight <= 0) return
    val context = textureView.context

    val displayRotationEnum = when (displayRotOverride) {
        Surface.ROTATION_0 -> "ROTATION_0"
        Surface.ROTATION_90 -> "ROTATION_90"
        Surface.ROTATION_180 -> "ROTATION_180"
        Surface.ROTATION_270 -> "ROTATION_270"
        else -> "UNKNOWN"
    }

    val displayRotationDegrees = when (displayRotOverride) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }

    val sensorOrient = sensorOrientation ?: 90

    val autoPrevRot = PreviewTransformHelper.getAutoPreviewRotationDegrees(displayRotOverride)
    val activePrevRot = forcedRotation ?: autoPrevRot
    val source = if (forcedRotation != null) "FORCED_$forcedRotation" else "AUTO_CAMERA2_BASIC"

    val transformResult = PreviewTransformHelper.buildTransform(
        viewWidth = viewWidth,
        viewHeight = viewHeight,
        previewWidth = previewSize.width,
        previewHeight = previewSize.height,
        displayRotation = displayRotOverride,
        forcedRotationDegrees = forcedRotation,
        mirrorForFrontCamera = isFrontCamera,
        scaleType = scaleMode
    )

    textureView.setTransform(transformResult.matrix)

    val jpegOrientation = com.pna.omnicamlab.camera.core.CameraOrientationHelper.calculateJpegOrientation(
        sensorOrientation = sensorOrient,
        deviceRotationDegrees = displayRotationDegrees,
        isFrontCamera = isFrontCamera
    )

    viewModel.updateTransformDebugInfo(
        sensorOri = sensorOrient,
        dispRotEnum = displayRotationEnum,
        dispRotDeg = displayRotationDegrees,
        autoPrevRot = autoPrevRot,
        forcedPrevRot = forcedRotation ?: -1,
        activePrevRot = activePrevRot,
        source = source,
        mirror = isFrontCamera,
        viewWidth = viewWidth,
        viewHeight = viewHeight,
        bufferWidth = previewSize.width,
        bufferHeight = previewSize.height,
        scaleXVal = transformResult.scaleX,
        scaleYVal = transformResult.scaleY,
        uniformScaleVal = transformResult.uniformScale,
        effectiveW = transformResult.effectiveW,
        effectiveH = transformResult.effectiveH,
        scaledW = transformResult.scaledW,
        scaledH = transformResult.scaledH,
        dx = transformResult.dx,
        dy = transformResult.dy,
        aspectRatioOk = transformResult.aspectRatioOk,
        mode = scaleMode.name,
        jpegOri = jpegOrientation,
        reason = reason
    )
}
