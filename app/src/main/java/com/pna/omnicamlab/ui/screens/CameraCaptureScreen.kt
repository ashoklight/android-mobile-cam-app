package com.pna.omnicamlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pna.omnicamlab.camera.core.CameraPreviewViewModel
import com.pna.omnicamlab.camera.core.CameraSessionState
import com.pna.omnicamlab.camera.preview.CameraPreviewSurface
import android.view.Surface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import com.pna.omnicamlab.camera.core.PreviewAspectMode
import com.pna.omnicamlab.camera.core.PreviewScaleType
import com.pna.omnicamlab.camera.core.PreviewTransformHelper

enum class FrameGuideMode { NONE, STILL_FRAME, ASPECT_4_3, ASPECT_16_9 }

@Composable
fun CameraCaptureScreen(
  onNavigateBack: () -> Unit,
  onNavigateToResult: (com.pna.omnicamlab.CaptureResult) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: CameraPreviewViewModel = viewModel()
) {
  val sessionState by viewModel.sessionState.collectAsState()
  val cameraProfiles by viewModel.cameraProfiles.collectAsState()
  val selectedCameraId by viewModel.selectedCameraId.collectAsState()

  // Collect debugging and reactive states
  val permissionGranted by viewModel.permissionGranted.collectAsState()
  val lifecycleActive by viewModel.lifecycleActive.collectAsState()
  val textureAvailable by viewModel.surfaceAvailable.collectAsState()
  val textureSize by viewModel.surfaceSize.collectAsState()
  val selectedPreviewSizeStr by viewModel.selectedPreviewSizeStr.collectAsState()
  val cameraOpenRequestedCount by viewModel.cameraOpenRequestedCount.collectAsState()
  val cameraOpenedCallbackCount by viewModel.cameraOpenedCallbackCount.collectAsState()
  val sessionConfiguredCount by viewModel.sessionConfiguredCount.collectAsState()
  val repeatingStarted by viewModel.repeatingStarted.collectAsState()
  val frameUpdateCount by viewModel.frameUpdateCount.collectAsState()
  val lastBlockedOpenReason by viewModel.lastBlockedOpenReason.collectAsState()
  val lastError by viewModel.lastError.collectAsState()

  // Preview transform debug states
  val sensorOrientationState by viewModel.sensorOrientationState.collectAsState()
  val displayRotationEnum by viewModel.displayRotationEnum.collectAsState()
  val displayRotationDegrees by viewModel.displayRotationDegrees.collectAsState()
  val computedPreviewRotationDegrees by viewModel.computedPreviewRotationDegrees.collectAsState()
  val jpegOrientationDegrees by viewModel.jpegOrientationDegrees.collectAsState()
  val mirrorForFrontCamera by viewModel.mirrorForFrontCamera.collectAsState()
  val transformAppliedCount by viewModel.transformAppliedCount.collectAsState()
  val transformViewSize by viewModel.transformViewSize.collectAsState()
  val transformBufferSize by viewModel.transformBufferSize.collectAsState()
  val transformScale by viewModel.transformScale.collectAsState()
  val transformMode by viewModel.transformMode.collectAsState()
  
  val forcedPreviewRotation by viewModel.forcedPreviewRotation.collectAsState()
  val forcedPreviewRotationDegrees by viewModel.forcedPreviewRotationDegrees.collectAsState()
  val autoPreviewRotationDegrees by viewModel.autoPreviewRotationDegrees.collectAsState()
  val activePreviewRotationDegrees by viewModel.activePreviewRotationDegrees.collectAsState()
  val transformSource by viewModel.transformSource.collectAsState()

  val selectedPreviewSize by viewModel.selectedPreviewSize.collectAsState()

  // Collect scale mode and uniform scaling telemetry states
  val scaleMode by viewModel.scaleMode.collectAsState()
  val scaleX by viewModel.scaleX.collectAsState()
  val scaleY by viewModel.scaleY.collectAsState()
  val uniformScale by viewModel.uniformScale.collectAsState()
  val effectiveBufferSize by viewModel.effectiveBufferSize.collectAsState()
  val scaledBufferSize by viewModel.scaledBufferSize.collectAsState()
  val cropOrLetterboxDxDy by viewModel.cropOrLetterboxDxDy.collectAsState()
  val aspectRatioPreserved by viewModel.aspectRatioPreserved.collectAsState()
  val scaleModeState by viewModel.scaleModeState.collectAsState()

  // Zoom, Aspect Ratio, and Rotation Refresh telemetry states
  val zoomState by viewModel.zoomState.collectAsState()
  val previewAspectMode by viewModel.previewAspectMode.collectAsState()
  val screenAspectRatio by viewModel.screenAspectRatio.collectAsState()
  val jpegCaptureSize by viewModel.jpegCaptureSize.collectAsState()
  val cameraOpenReason by viewModel.cameraOpenReason.collectAsState()
  val sessionRecreateReason by viewModel.sessionRecreateReason.collectAsState()
  val transformOnlyUpdateCount by viewModel.transformOnlyUpdateCount.collectAsState()
  val cameraReopenCount by viewModel.cameraReopenCount.collectAsState()
  val transformUpdateReason by viewModel.transformUpdateReason.collectAsState()

  val deviceOrientationDegrees by viewModel.deviceOrientationDegrees.collectAsState()
  val forcedJpegOrientation by viewModel.forcedJpegOrientation.collectAsState()
  val jpegOrientationSource by viewModel.jpegOrientationSource.collectAsState()

  // Compose Viewport Dimensions
  var containerWidth by remember { mutableStateOf(0) }
  var containerHeight by remember { mutableStateOf(0) }

  // Overlay Guides states
  var showGrid by remember { mutableStateOf(false) }
  var showCenterCross by remember { mutableStateOf(false) }
  var frameGuideMode by remember { mutableStateOf(FrameGuideMode.NONE) }

  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  val context = androidx.compose.ui.platform.LocalContext.current

  // Enforce camera permissions check dynamically and feed it to ViewModel
  val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.CAMERA
  ) == android.content.pm.PackageManager.PERMISSION_GRANTED

  LaunchedEffect(hasPermission) {
    viewModel.setPermissionGranted(hasPermission)
  }

  // Safely manage camera lifecycle using a stable observer key
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
          android.util.Log.i("OmniCamPreview", "LifecycleObserver: ON_START/ON_RESUME")
          viewModel.setLifecycleActive(true)
        }
        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
          android.util.Log.i("OmniCamPreview", "LifecycleObserver: ON_PAUSE/ON_STOP")
          viewModel.setLifecycleActive(false)
        }
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      viewModel.setLifecycleActive(false)
      viewModel.closeCamera()
    }
  }

  // React to successful photo capture by navigating safely with encoded URI
  LaunchedEffect(sessionState) {
    val state = sessionState
    if (state is CameraSessionState.CaptureSuccess) {
      val encodedUri = try {
        java.net.URLEncoder.encode(state.savedUri, "UTF-8")
      } catch (e: Exception) {
        state.savedUri
      }

      val jpegSizes = state.profile.photoProfile.jpegSizes.mapNotNull { sizeStr ->
        val parts = sizeStr.split("x")
        if (parts.size == 2) {
          val w = parts[0].toIntOrNull()
          val h = parts[1].toIntOrNull()
          if (w != null && h != null) {
            com.pna.omnicamlab.camera.capabilities.CameraSize(w, h)
          } else null
        } else null
      }
      val bestJpegSize = com.pna.omnicamlab.camera.core.JpegSizeSelector.selectBestJpegSize(jpegSizes)
      val jpegSizeStr = "${bestJpegSize.width}x${bestJpegSize.height}"

      val resultKey = com.pna.omnicamlab.CaptureResult(
        savedUri = encodedUri,
        cameraId = state.cameraId,
        facing = state.profile.facing.name,
        jpegSize = jpegSizeStr,
        timestamp = System.currentTimeMillis(),
        orientation = deviceOrientationDegrees,
        iso = state.iso,
        exposureTimeNs = state.exposureTimeNs,
        focalLength = state.focalLength,
        aperture = state.aperture
      )
      onNavigateToResult(resultKey)
    }
  }

  val activeProfile = cameraProfiles.find { it.cameraId == selectedCameraId }
  val sensorOrientation = activeProfile?.sensorProfile?.sensorOrientation ?: 90
  val isFrontCamera = activeProfile?.facing == com.pna.omnicamlab.camera.capabilities.CameraFacing.FRONT

  val activePreviewSize = when (val state = sessionState) {
    is CameraSessionState.Previewing -> state.previewSize
    is CameraSessionState.Capturing -> state.previewSize
    is CameraSessionState.Saving -> state.previewSize
    is CameraSessionState.CaptureSuccess -> state.previewSize
    is CameraSessionState.CaptureError -> state.previewSize
    else -> null
  }

  val stateText = when (sessionState) {
    is CameraSessionState.Idle -> "IDLE"
    is CameraSessionState.Loading -> "LOADING"
    is CameraSessionState.Previewing -> "LIVE PREVIEW"
    is CameraSessionState.Switching -> "SWITCHING"
    is CameraSessionState.Capturing -> "CAPTURING"
    is CameraSessionState.Saving -> "SAVING"
    is CameraSessionState.CaptureSuccess -> "SUCCESS"
    is CameraSessionState.CaptureError -> "CAPTURE ERROR"
    is CameraSessionState.Error -> "ERROR"
  }

  val previewAspect = selectedPreviewSize.width.toFloat() / selectedPreviewSize.height.toFloat()
  val stillAspect = jpegCaptureSize.width.toFloat() / jpegCaptureSize.height.toFloat()
  val aspectMismatchPercent = Math.abs(previewAspect - stillAspect) / stillAspect * 100f
  val wysiwygMatch = aspectMismatchPercent < 1.0f

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .onGloballyPositioned { coords ->
        containerWidth = coords.size.width
        containerHeight = coords.size.height
      }
  ) {
    // 1. Live Viewfinder Preview (Always created and stable)
    CameraPreviewSurface(
      previewSize = selectedPreviewSize,
      sensorOrientation = sensorOrientation,
      isFrontCamera = isFrontCamera,
      viewModel = viewModel,
      modifier = Modifier.fillMaxSize()
    )

    // Synchronized Bounding Guide Box Math
    val displayRotation = when (displayRotationEnum) {
      "ROTATION_90" -> Surface.ROTATION_90
      "ROTATION_180" -> Surface.ROTATION_180
      "ROTATION_270" -> Surface.ROTATION_270
      else -> Surface.ROTATION_0
    }

    val transformResult = remember(
      containerWidth,
      containerHeight,
      selectedPreviewSize,
      displayRotation,
      forcedPreviewRotation,
      isFrontCamera,
      scaleMode
    ) {
      PreviewTransformHelper.buildTransform(
        viewWidth = containerWidth,
        viewHeight = containerHeight,
        previewWidth = selectedPreviewSize.width,
        previewHeight = selectedPreviewSize.height,
        displayRotation = displayRotation,
        forcedRotationDegrees = forcedPreviewRotation,
        mirrorForFrontCamera = isFrontCamera,
        scaleType = scaleMode
      )
    }

    val isPortrait = containerHeight > containerWidth
    val activeAspect = when (frameGuideMode) {
      FrameGuideMode.NONE -> {
        if (isPortrait) {
          jpegCaptureSize.height.toFloat() / jpegCaptureSize.width.toFloat()
        } else {
          jpegCaptureSize.width.toFloat() / jpegCaptureSize.height.toFloat()
        }
      }
      FrameGuideMode.STILL_FRAME -> {
        if (isPortrait) {
          jpegCaptureSize.height.toFloat() / jpegCaptureSize.width.toFloat()
        } else {
          jpegCaptureSize.width.toFloat() / jpegCaptureSize.height.toFloat()
        }
      }
      FrameGuideMode.ASPECT_4_3 -> {
        if (isPortrait) 3f / 4f else 4f / 3f
      }
      FrameGuideMode.ASPECT_16_9 -> {
        if (isPortrait) 9f / 16f else 16f / 9f
      }
    }

    val pw = transformResult.scaledW
    val ph = transformResult.scaledH
    val dx = transformResult.dx
    val dy = transformResult.dy

    val guideWidth: Float
    val guideHeight: Float
    if (pw / ph > activeAspect) {
      guideHeight = ph
      guideWidth = ph * activeAspect
    } else {
      guideWidth = pw
      guideHeight = pw / activeAspect
    }

    val gLeft = dx + (pw - guideWidth) / 2f
    val gTop = dy + (ph - guideHeight) / 2f
    val gRight = gLeft + guideWidth
    val gBottom = gTop + guideHeight

    // 2. Custom Guides, Safe Frames, and Grid Overlay
    if (containerWidth > 0 && containerHeight > 0) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTransformGestures { _, _, zoomFactor, _ ->
              if (zoomState.isZoomSupported) {
                val newZoom = zoomState.currentZoom * zoomFactor
                viewModel.setZoom(newZoom)
              }
            }
          }
      ) {
        // Draw dimming layer outside the active guide box
        if (frameGuideMode != FrameGuideMode.NONE) {
          // Top dimming
          drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = androidx.compose.ui.geometry.Size(size.width, maxOf(0f, gTop))
          )
          // Bottom dimming
          drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, minOf(size.height, gBottom)),
            size = androidx.compose.ui.geometry.Size(size.width, maxOf(0f, size.height - gBottom))
          )
          // Left dimming
          drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, maxOf(0f, gTop)),
            size = androidx.compose.ui.geometry.Size(maxOf(0f, gLeft), maxOf(0f, minOf(size.height, gBottom) - gTop))
          )
          // Right dimming
          drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(minOf(size.width, gRight), maxOf(0f, gTop)),
            size = androidx.compose.ui.geometry.Size(maxOf(0f, size.width - gRight), maxOf(0f, minOf(size.height, gBottom) - gTop))
          )

          // Thin border around the active guide box
          drawRect(
            color = Color.White.copy(alpha = 0.45f),
            topLeft = Offset(gLeft, gTop),
            size = androidx.compose.ui.geometry.Size(guideWidth, guideHeight),
            style = Stroke(width = 1.dp.toPx())
          )
        }

        // Draw 3x3 Grid inside the active guide box
        if (showGrid) {
          val strokeW = 1.dp.toPx()
          val gridColor = Color.White.copy(alpha = 0.35f)
          
          // Vertical lines
          drawLine(
            color = gridColor,
            start = Offset(gLeft + guideWidth / 3f, gTop),
            end = Offset(gLeft + guideWidth / 3f, gBottom),
            strokeWidth = strokeW
          )
          drawLine(
            color = gridColor,
            start = Offset(gLeft + 2f * guideWidth / 3f, gTop),
            end = Offset(gLeft + 2f * guideWidth / 3f, gBottom),
            strokeWidth = strokeW
          )
          // Horizontal lines
          drawLine(
            color = gridColor,
            start = Offset(gLeft, gTop + guideHeight / 3f),
            end = Offset(gRight, gTop + guideHeight / 3f),
            strokeWidth = strokeW
          )
          drawLine(
            color = gridColor,
            start = Offset(gLeft, gTop + 2f * guideHeight / 3f),
            end = Offset(gRight, gTop + 2f * guideHeight / 3f),
            strokeWidth = strokeW
          )
        }

        // Draw Center Cross inside the active guide box
        if (showCenterCross) {
          val strokeW = 1.5.dp.toPx()
          val crossSize = 12.dp.toPx()
          val cX = gLeft + guideWidth / 2f
          val cY = gTop + guideHeight / 2f
          val crossColor = Color.White.copy(alpha = 0.5f)
          
          // Horizontal cross line
          drawLine(
            color = crossColor,
            start = Offset(cX - crossSize, cY),
            end = Offset(cX + crossSize, cY),
            strokeWidth = strokeW
          )
          // Vertical cross line
          drawLine(
            color = crossColor,
            start = Offset(cX, cY - crossSize),
            end = Offset(cX, cY + crossSize),
            strokeWidth = strokeW
          )
        }
      }
    }

    // Small "TOP" label near the top of preview UI
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 110.dp),
      contentAlignment = Alignment.TopCenter
    ) {
      Text(
        text = "TOP",
        color = Color.White.copy(alpha = 0.8f),
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier
          .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
          .padding(horizontal = 8.dp, vertical = 2.dp)
      )
    }

    // Left Control Rail for Viewfinder Guides (Elegant Toggles)
    Column(
      modifier = Modifier
        .align(Alignment.CenterStart)
        .padding(start = 16.dp)
        .width(48.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color.Black.copy(alpha = 0.5f))
        .padding(vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Grid Toggle
      IconButton(
        onClick = { showGrid = !showGrid },
        modifier = Modifier.size(36.dp)
      ) {
        Text(
          text = "GRID",
          color = if (showGrid) Color(0xFF00FFCC) else Color.White,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold
        )
      }
      
      // Center Cross Toggle
      IconButton(
        onClick = { showCenterCross = !showCenterCross },
        modifier = Modifier.size(36.dp)
      ) {
        Text(
          text = "CRS",
          color = if (showCenterCross) Color(0xFF00FFCC) else Color.White,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold
        )
      }

      // Frame Guide Toggle
      IconButton(
        onClick = {
          frameGuideMode = when (frameGuideMode) {
            FrameGuideMode.NONE -> FrameGuideMode.STILL_FRAME
            FrameGuideMode.STILL_FRAME -> FrameGuideMode.ASPECT_4_3
            FrameGuideMode.ASPECT_4_3 -> FrameGuideMode.ASPECT_16_9
            FrameGuideMode.ASPECT_16_9 -> FrameGuideMode.NONE
          }
        },
        modifier = Modifier.size(36.dp)
      ) {
        Text(
          text = when (frameGuideMode) {
            FrameGuideMode.NONE -> "NONE"
            FrameGuideMode.STILL_FRAME -> "STILL"
            FrameGuideMode.ASPECT_4_3 -> "4:3"
            FrameGuideMode.ASPECT_16_9 -> "16:9"
          },
          color = if (frameGuideMode != FrameGuideMode.NONE) Color(0xFF00FFCC) else Color.White,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    // 3. Debug Overlay
    DebugOverlay(
      permissionGranted = permissionGranted,
      selectedCameraId = selectedCameraId,
      lifecycleActive = lifecycleActive,
      textureAvailable = textureAvailable,
      textureSize = textureSize,
      selectedPreviewSize = selectedPreviewSizeStr,
      cameraOpenRequestedCount = cameraOpenRequestedCount,
      cameraOpenedCallbackCount = cameraOpenedCallbackCount,
      sessionConfiguredCount = sessionConfiguredCount,
      repeatingStarted = repeatingStarted,
      frameUpdateCount = frameUpdateCount,
      currentState = stateText,
      lastBlockedOpenReason = lastBlockedOpenReason,
      lastError = lastError,
      sensorOrientation = sensorOrientationState,
      displayRotationEnum = displayRotationEnum,
      displayRotationDegrees = displayRotationDegrees,
      jpegOrientationDegrees = jpegOrientationDegrees,
      mirrorForFrontCamera = mirrorForFrontCamera,
      transformAppliedCount = transformAppliedCount,
      transformViewSize = transformViewSize,
      transformBufferSize = transformBufferSize,
      transformScale = transformScale,
      transformMode = transformMode,
      forcedPreviewRotation = forcedPreviewRotation,
      forcedPreviewRotationDegrees = forcedPreviewRotationDegrees,
      autoPreviewRotationDegrees = autoPreviewRotationDegrees,
      activePreviewRotationDegrees = activePreviewRotationDegrees,
      transformSource = transformSource,
      scaleMode = scaleMode,
      scaleX = scaleX,
      scaleY = scaleY,
      uniformScale = uniformScale,
      effectiveBufferSize = effectiveBufferSize,
      scaledBufferSize = scaledBufferSize,
      cropOrLetterboxDxDy = cropOrLetterboxDxDy,
      aspectRatioPreserved = aspectRatioPreserved,
      scaleModeState = scaleModeState,
      cameraOpenReason = cameraOpenReason,
      sessionRecreateReason = sessionRecreateReason,
      transformOnlyUpdateCount = transformOnlyUpdateCount,
      cameraReopenCount = cameraReopenCount,
      transformUpdateReason = transformUpdateReason,
      previewAspectMode = previewAspectMode.name,
      jpegCaptureSize = "${jpegCaptureSize.width}x${jpegCaptureSize.height}",
      wysiwygMatch = wysiwygMatch,
      aspectMismatchPercent = aspectMismatchPercent,
      zoomSupported = zoomState.isZoomSupported,
      zoomBackend = zoomState.zoomBackend,
      appliedZoom = zoomState.currentZoom,
      deviceOrientationDegrees = deviceOrientationDegrees,
      jpegOrientationSource = jpegOrientationSource,
      forcedJpegOrientation = forcedJpegOrientation,
      onForcedRotationSelected = { rotation -> viewModel.setForcedPreviewRotation(rotation) },
      onScaleModeSelected = { mode -> viewModel.setScaleMode(mode) },
      onForcedJpegOrientationSelected = { orientation -> viewModel.setForcedJpegOrientation(orientation) },
      onPreviewAspectModeSelected = { mode -> viewModel.setPreviewAspectMode(mode, screenAspectRatio) }
    )

    // 4. Top Status Bar (Overlay)
    val isBackEnabled = sessionState !is CameraSessionState.Capturing && sessionState !is CameraSessionState.Saving

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .background(Color.Black.copy(alpha = 0.6f))
        .padding(top = 28.dp, bottom = 16.dp, start = 12.dp, end = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { if (isBackEnabled) onNavigateBack() },
          enabled = isBackEnabled
        ) {
          Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = if (isBackEnabled) Color.White else Color.DarkGray
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column {
          val headerLabel = if (activeProfile != null) {
            when (activeProfile.facing) {
              com.pna.omnicamlab.camera.capabilities.CameraFacing.BACK -> "Rear logical camera ${selectedCameraId ?: "N/A"}"
              com.pna.omnicamlab.camera.capabilities.CameraFacing.FRONT -> "Front camera ${selectedCameraId ?: "N/A"}"
              com.pna.omnicamlab.camera.capabilities.CameraFacing.EXTERNAL -> "External camera ${selectedCameraId ?: "N/A"}"
              else -> "Camera ${selectedCameraId ?: "N/A"}"
            }
          } else {
            "Camera ${selectedCameraId ?: "N/A"}"
          }
          Text(
            text = headerLabel,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          if (activeProfile != null) {
            val subsSuffix = if (activeProfile.physicalCameraIds.isNotEmpty()) {
              " | Subs: ${activeProfile.physicalCameraIds.joinToString(", ")}"
            } else ""
            Text(
              text = "${activeProfile.facing} | HW: ${activeProfile.hardwareLevel}$subsSuffix",
              color = Color.LightGray,
              fontSize = 10.sp
            )
          }
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (activePreviewSize != null) {
          Badge(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 4.dp)
          ) {
            Text("${activePreviewSize.width}x${activePreviewSize.height}", fontWeight = FontWeight.Bold, fontSize = 10.sp)
          }
        }

        Badge(
          containerColor = if (sessionState is CameraSessionState.Error || sessionState is CameraSessionState.CaptureError) MaterialTheme.colorScheme.error else Color.DarkGray,
          contentColor = Color.White,
          modifier = Modifier.padding(horizontal = 4.dp)
        ) {
          Text(stateText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
      }
    }

    // 5. Right Manual Control Rail (Overlay) - Kept as elegant, disabled placeholders
    Column(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 16.dp)
        .width(64.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.Black.copy(alpha = 0.5f))
        .padding(vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      ManualControlItem(label = "ISO", value = "AUTO", color = Color.Gray)
      ManualControlItem(label = "SHT", value = "AUTO", color = Color.Gray)
      ManualControlItem(label = "FOC", value = "AUTO", color = Color.Gray)
      ManualControlItem(label = "WB", value = "AUTO", color = Color.Gray)
      ManualControlItem(label = "EV", value = "0.0", color = Color.Gray)
    }

    // 6. Bottom Capture, Zoom & Mode Controls (Overlay)
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .background(Color.Black.copy(alpha = 0.7f))
        .padding(bottom = 24.dp, top = 12.dp)
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Zoom Controls Foundation UI
      if (zoomState.isZoomSupported) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Floating Quick Zoom Buttons
          val quickZooms = listOf(1.0f, 2.0f, minOf(5.0f, zoomState.maxZoom))
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            quickZooms.forEach { zVal ->
              val isSelected = Math.abs(zoomState.currentZoom - zVal) < 0.05f
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(if (isSelected) Color(0xFF00FFCC) else Color.DarkGray.copy(alpha = 0.6f))
                  .clickable { viewModel.setZoom(zVal) },
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "${zVal.toInt()}x",
                  color = if (isSelected) Color.Black else Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Premium Interactive Zoom Slider
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = String.format(java.util.Locale.US, "%.1fx", zoomState.currentZoom),
            color = Color(0xFF00FFCC),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp)
          )
          Slider(
            value = zoomState.currentZoom,
            onValueChange = { viewModel.setZoom(it) },
            valueRange = zoomState.minZoom..zoomState.maxZoom,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
              thumbColor = Color(0xFF00FFCC),
              activeTrackColor = Color(0xFF00FFCC),
              inactiveTrackColor = Color.DarkGray
            )
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      Text(
        text = "PRO PHOTO PREVIEW",
        color = Color.LightGray,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 1.5.sp
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Gallery Shortcut (disabled placeholder)
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E1E1E)),
          contentAlignment = Alignment.Center
        ) {
          Text("GALLERY", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        // Shutter Button
        val isShutterEnabled = sessionState is CameraSessionState.Previewing
        Box(
          modifier = Modifier
            .size(76.dp)
            .border(4.dp, if (isShutterEnabled) Color.White else Color.DarkGray, CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(if (isShutterEnabled) Color.White else Color(0xFF424242))
            .clickable(
              enabled = isShutterEnabled,
              onClick = { viewModel.takePicture(deviceOrientationDegrees) }
            )
        )

        // Camera Switcher
        val isSwitchEnabled = cameraProfiles.size > 1 && sessionState is CameraSessionState.Previewing
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSwitchEnabled) Color(0xFF2E2E3A) else Color(0xFF1E1E1E))
            .clickable(
              enabled = isSwitchEnabled,
              onClick = { viewModel.switchCamera() }
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Switch Camera",
            tint = if (isSwitchEnabled) Color.White else Color.DarkGray,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }

    // 7. Loading, Switching, Capturing & Saving Overlays
    if (sessionState is CameraSessionState.Loading || 
        sessionState is CameraSessionState.Switching ||
        sessionState is CameraSessionState.Capturing ||
        sessionState is CameraSessionState.Saving) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = when (sessionState) {
              is CameraSessionState.Switching -> "Switching Lenses..."
              is CameraSessionState.Capturing -> "Capturing..."
              is CameraSessionState.Saving -> "Saving to MediaStore..."
              else -> "Starting Viewfinder..."
            },
            color = Color.LightGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    // 8. Readable Error Card on Failure
    if (sessionState is CameraSessionState.Error) {
      val errorState = sessionState as CameraSessionState.Error
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.8f))
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Camera Session Error",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onErrorContainer,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = errorState.error.toUserMessage(),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Debug Info: ${errorState.message}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError),
                modifier = Modifier.weight(1f)
              ) {
                Text("Return Home", color = MaterialTheme.colorScheme.error)
              }
              if (cameraProfiles.size > 1) {
                Button(
                  onClick = { viewModel.switchCamera() },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Try Next Lens", color = Color.White)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DebugOverlay(
  permissionGranted: Boolean,
  selectedCameraId: String?,
  lifecycleActive: Boolean,
  textureAvailable: Boolean,
  textureSize: String,
  selectedPreviewSize: String,
  cameraOpenRequestedCount: Int,
  cameraOpenedCallbackCount: Int,
  sessionConfiguredCount: Int,
  repeatingStarted: Boolean,
  frameUpdateCount: Int,
  currentState: String,
  lastBlockedOpenReason: String,
  lastError: String?,
  sensorOrientation: Int,
  displayRotationEnum: String,
  displayRotationDegrees: Int,
  jpegOrientationDegrees: Int,
  mirrorForFrontCamera: Boolean,
  transformAppliedCount: Int,
  transformViewSize: String,
  transformBufferSize: String,
  transformScale: String,
  transformMode: String,
  forcedPreviewRotation: Int?,
  forcedPreviewRotationDegrees: Int,
  autoPreviewRotationDegrees: Int,
  activePreviewRotationDegrees: Int,
  transformSource: String,
  scaleMode: PreviewScaleType,
  scaleX: Float,
  scaleY: Float,
  uniformScale: Float,
  effectiveBufferSize: String,
  scaledBufferSize: String,
  cropOrLetterboxDxDy: String,
  aspectRatioPreserved: Boolean,
  scaleModeState: String,
  cameraOpenReason: String,
  sessionRecreateReason: String,
  transformOnlyUpdateCount: Int,
  cameraReopenCount: Int,
  transformUpdateReason: String,
  previewAspectMode: String,
  jpegCaptureSize: String,
  wysiwygMatch: Boolean,
  aspectMismatchPercent: Float,
  zoomSupported: Boolean,
  zoomBackend: String,
  appliedZoom: Float,
  deviceOrientationDegrees: Int,
  jpegOrientationSource: String,
  forcedJpegOrientation: Int?,
  onForcedRotationSelected: (Int?) -> Unit,
  onScaleModeSelected: (PreviewScaleType) -> Unit,
  onForcedJpegOrientationSelected: (Int?) -> Unit,
  onPreviewAspectModeSelected: (PreviewAspectMode) -> Unit,
  modifier: Modifier = Modifier
) {
  var showDetails by remember { mutableStateOf(false) }

  Card(
    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
    modifier = modifier
      .padding(start = 12.dp, top = 92.dp, end = 12.dp)
      .width(260.dp)
      .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
      .clickable { showDetails = !showDetails },
    shape = RoundedCornerShape(8.dp)
  ) {
    Column(
      modifier = Modifier.padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("OMNICAM DEBUG VIEWER", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(
          text = if (showDetails) "HIDE [-]" else "SHOW DETAILS [+]",
          color = Color.LightGray,
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold
        )
      }
      
      HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 2.dp))
      
      DebugRow("State", currentState)
      DebugRow("WYSIWYG Match", if (wysiwygMatch) "YES" else "NO", isWarning = !wysiwygMatch)
      DebugRow("Zoom", String.format(java.util.Locale.US, "%.2fx (%s)", appliedZoom, zoomBackend))
      DebugRow("Device Orientation", "$deviceOrientationDegrees°")
      DebugRow("JPEG Source", jpegOrientationSource)
      DebugRow("JPEG Output Orientation", "$jpegOrientationDegrees°")

      if (showDetails) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 2.dp))
        Text("LIFECYCLE & STATS", color = Color(0xFF00FFCC), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        DebugRow("Permission", permissionGranted.toString())
        DebugRow("Selected Camera ID", selectedCameraId ?: "null")
        DebugRow("Open Reason", cameraOpenReason)
        DebugRow("Reopen Reason", sessionRecreateReason)
        DebugRow("Camera Reopens", cameraReopenCount.toString())
        DebugRow("Transform Updates Only", transformOnlyUpdateCount.toString())
        DebugRow("Frame Update Count", frameUpdateCount.toString())
        DebugRow("Blocked Reason", lastBlockedOpenReason, isWarning = lastBlockedOpenReason != "None")
        if (lastError != null) {
          DebugRow("Last Error", lastError, isError = true)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 2.dp))
        Text("ASPECTS & RESOLUTIONS", color = Color(0xFF00FFCC), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        DebugRow("Aspect Mode", previewAspectMode)
        DebugRow("JPEG Capture Size", jpegCaptureSize)
        DebugRow("Preview Size Chosen", selectedPreviewSize)
        DebugRow("Aspect Mismatch", String.format(java.util.Locale.US, "%.2f%%", aspectMismatchPercent))
        
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 2.dp))
        Text("TRANSFORMS & SCALING", color = Color(0xFF00FFCC), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        DebugRow("Transform Update Reason", transformUpdateReason)
        DebugRow("Active Rotation", "$activePreviewRotationDegrees°")
        DebugRow("Sensor Orientation", "$sensorOrientation°")
        DebugRow("Display Rotation", displayRotationEnum)
        DebugRow("Scale Factor", transformScale)
        DebugRow("Effective Buf Size", effectiveBufferSize)
        DebugRow("Scaled Buf Size", scaledBufferSize)
        DebugRow("Centering Dx,Dy", cropOrLetterboxDxDy)
        DebugRow("Aspect Preserved", aspectRatioPreserved.toString())
        
        if (Math.abs(scaleX - scaleY) > 1e-4f || !aspectRatioPreserved) {
          Text(
            text = "NON-UNIFORM SCALE BUG",
            color = Color.Red,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .fillMaxWidth()
              .background(Color.Red.copy(alpha = 0.2f))
              .padding(vertical = 2.dp),
            textAlign = TextAlign.Center
          )
        }
      }
      
      HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 2.dp))
      
      // Aspect Mode Selector
      Text("SELECT PREVIEW ASPECT STRATEGY", color = Color(0xFF00FFCC), fontSize = 8.sp, fontWeight = FontWeight.Bold)
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        val modes = listOf(
          PreviewAspectMode.PHOTO_WYSIWYG to "WYSIWYG",
          PreviewAspectMode.SCREEN_FILL to "FILL",
          PreviewAspectMode.VIDEO_16_9 to "16:9"
        )
        modes.forEach { (modeVal, label) ->
          val isSelected = previewAspectMode == modeVal.name
          Button(
            onClick = { onPreviewAspectModeSelected(modeVal) },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(22.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isSelected) Color(0xFF00FFCC) else Color.DarkGray,
              contentColor = if (isSelected) Color.Black else Color.White
            ),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Preview Rotation Override selector
      Spacer(modifier = Modifier.height(2.dp))
      Text("CALIBRATE PREVIEW ROTATION", color = Color(0xFF00FFCC), fontSize = 8.sp, fontWeight = FontWeight.Bold)
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        val options = listOf(null to "AUTO", 0 to "0°", 90 to "90°", 180 to "180°", 270 to "270°")
        options.forEach { (rotVal, label) ->
          val isSelected = forcedPreviewRotation == rotVal
          Button(
            onClick = { onForcedRotationSelected(rotVal) },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(22.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isSelected) Color(0xFF00FFCC) else Color.DarkGray,
              contentColor = if (isSelected) Color.Black else Color.White
            ),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // JPEG Orientation Override selector
      Spacer(modifier = Modifier.height(2.dp))
      Text("CALIBRATE JPEG ORIENTATION", color = Color(0xFF00FFCC), fontSize = 8.sp, fontWeight = FontWeight.Bold)
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        val options = listOf(null to "AUTO", 0 to "0°", 90 to "90°", 180 to "180°", 270 to "270°")
        options.forEach { (oriVal, label) ->
          val isSelected = forcedJpegOrientation == oriVal
          Button(
            onClick = { onForcedJpegOrientationSelected(oriVal) },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(22.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isSelected) Color(0xFF00FFCC) else Color.DarkGray,
              contentColor = if (isSelected) Color.Black else Color.White
            ),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Preview Scale Mode selector
      Spacer(modifier = Modifier.height(2.dp))
      Text("CALIBRATE PREVIEW SCALE MODE", color = Color(0xFF00FFCC), fontSize = 8.sp, fontWeight = FontWeight.Bold)
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        val modes = listOf(
          PreviewScaleType.CENTER_CROP to "CROP",
          PreviewScaleType.FIT_CENTER to "FIT"
        )
        modes.forEach { (modeType, label) ->
          val isSelected = scaleMode == modeType
          Button(
            onClick = { onScaleModeSelected(modeType) },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(22.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isSelected) Color(0xFF00FFCC) else Color.DarkGray,
              contentColor = if (isSelected) Color.Black else Color.White
            ),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun DebugRow(label: String, value: String, isWarning: Boolean = false, isError: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, color = Color.Gray, fontSize = 9.sp)
    Text(
      text = value,
      color = when {
        isError -> Color.Red
        isWarning -> Color.Yellow
        else -> Color.White
      },
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun ManualControlItem(
  label: String,
  value: String,
  color: Color
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.padding(4.dp)
  ) {
    Text(
      text = label,
      color = Color.LightGray,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = value,
      color = color,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
