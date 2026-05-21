package com.pna.omnicamlab.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pna.omnicamlab.camera.capabilities.*
import com.pna.omnicamlab.util.logging.OmniLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapabilityReportScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scanner = remember { CapabilityScanner(context) }
    var profiles by remember { mutableStateOf<List<CameraDeviceProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Scan cameras on launch
    LaunchedEffect(Unit) {
        profiles = scanner.scanCapabilities()
        isLoading = false
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F0F14),
            Color(0xFF16161D),
            Color(0xFF0D0D10)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Camera Audit Report",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isLoading = true
                            profiles = scanner.scanCapabilities()
                            isLoading = false
                            Toast.makeText(context, "Rescanned camera hardware.", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan")
                    }
                    if (profiles.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val jsonStr = CameraCapabilityJsonExporter.exportToJsonString(profiles)
                                val file = CameraCapabilityJsonExporter.saveReport(context, jsonStr)
                                if (file != null) {
                                    Toast.makeText(
                                        context,
                                        "Report exported:\n${file.name}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    OmniLogger.i(OmniLogger.Tag.UIState, "Exported capability report to ${file.absolutePath}")
                                } else {
                                    Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export JSON")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF16161F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Auditing Hardware Sensor Bindings...", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else if (profiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "No Lenses Found",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Scannable Cameras Found",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ensure that device permissions are granted or check hardware configurations.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        val allOpenableIds = remember(profiles) { profiles.map { it.cameraId }.toSet() }
                        val allPhysicalIds = remember(profiles) { profiles.flatMap { it.physicalCameraIds }.distinct() }
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Scanned ${profiles.size} openable Camera2 camera IDs successfully on API ${android.os.Build.VERSION.SDK_INT}",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                            
                            // Top Summary Panel
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "System Camera Summary",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Openable Camera2 IDs", fontSize = 11.sp, color = Color.Gray)
                                            Text("${profiles.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            val logicalMultiCount = profiles.count { it.isLogicalMultiCamera }
                                            Text("Logical Multi-Clusters", fontSize = 11.sp, color = Color.Gray)
                                            Text("$logicalMultiCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }

                                    if (allPhysicalIds.isNotEmpty()) {
                                        HorizontalDivider(color = Color(0xFF2E2E3A))
                                        Text(
                                            text = "Detected Physical Sub-Cameras (${allPhysicalIds.size})",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            allPhysicalIds.forEach { physicalId ->
                                                val isDirectlyOpenable = allOpenableIds.contains(physicalId)
                                                val parentCamera = profiles.find { it.physicalCameraIds.contains(physicalId) }?.cameraId
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFF2E2E3A)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = physicalId,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp,
                                                                color = Color.LightGray
                                                            )
                                                        }
                                                        Text(
                                                            text = if (parentCamera != null) "Nested in Camera $parentCamera" else "Independent Sensor",
                                                            fontSize = 11.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                    Text(
                                                        text = if (isDirectlyOpenable) "Openable directly" else "Hidden sub-camera",
                                                        fontSize = 10.sp,
                                                        color = if (isDirectlyOpenable) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Poco-style global note disclaimer
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2828)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "OEM Disclaimer",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "OEM Camera Constraint Info",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "If your phone has more physical lenses than shown here, the OEM may expose them only through the stock camera app or as hidden physical sub-cameras inside a logical Camera2 camera.",
                                                    fontSize = 11.sp,
                                                    color = Color.LightGray,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val allOpenableIds = profiles.map { it.cameraId }.toSet()
                    items(profiles) { profile ->
                        CameraDeviceCard(profile = profile, allOpenableIds = allOpenableIds)
                    }
                }
            }
        }
    }
}

@Composable
fun CameraDeviceCard(profile: CameraDeviceProfile, allOpenableIds: Set<String>) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E2E3A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.cameraId,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = when (profile.facing) {
                                CameraFacing.BACK -> "Rear logical camera ${profile.cameraId}"
                                CameraFacing.FRONT -> "Front camera ${profile.cameraId}"
                                CameraFacing.EXTERNAL -> "External camera ${profile.cameraId}"
                                CameraFacing.UNKNOWN -> "Camera ${profile.cameraId}"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Level: ${CameraCapabilityFormatter.formatHardwareLevel(profile.hardwareLevel)}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse",
                        tint = Color.Gray,
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Feature Chip Grid (Always Visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FeatureStatusChip(
                    label = "RAW",
                    isSupported = profile.photoProfile.supportsRaw,
                    modifier = Modifier.weight(1f)
                )
                FeatureStatusChip(
                    label = "MANUAL",
                    isSupported = profile.capabilities.contains(CameraCapability.MANUAL_SENSOR),
                    modifier = Modifier.weight(1f)
                )
                FeatureStatusChip(
                    label = "BURST",
                    isSupported = profile.photoProfile.supportsBurst,
                    modifier = Modifier.weight(1f)
                )
                FeatureStatusChip(
                    label = "SLOW-MO",
                    isSupported = profile.videoProfile.supportsHighSpeedVideo,
                    modifier = Modifier.weight(1f)
                )
            }

            // Expanded Subsections
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider(color = Color(0xFF2E2E3A))

                    // Warnings
                    if (profile.supportWarnings.isNotEmpty()) {
                        WarningCalloutSection(warnings = profile.supportWarnings)
                    }

                    // 1. Sensor Profiles Section
                    CardDetailSection(title = "Sensor Configuration") {
                        DetailItem(label = "Active Array Size", value = profile.sensorProfile.activeArraySize ?: "N/A")
                        DetailItem(label = "Pixel Array Size", value = profile.sensorProfile.pixelArraySize ?: "N/A")
                        DetailItem(label = "ISO Range", value = profile.sensorProfile.isoRange ?: "N/A")
                        DetailItem(
                            label = "Exposure Bounds", 
                            value = profile.sensorProfile.exposureTimeRangeNs?.split("-")?.let { parts ->
                                if (parts.size == 2) {
                                    val min = parts[0].trim().toLongOrNull()
                                    val max = parts[1].trim().toLongOrNull()
                                    CameraCapabilityFormatter.formatExposureTimeRange(min, max)
                                } else null
                            } ?: "N/A"
                        )
                        DetailItem(label = "Orientation", value = "${profile.sensorProfile.sensorOrientation ?: 0}°")
                        DetailItem(label = "Color Filter Arrangement", value = profile.sensorProfile.colorFilterArrangement ?: "N/A")
                        DetailItem(label = "Timestamp Source", value = profile.sensorProfile.timestampSource ?: "N/A")
                    }

                    // 2. Lens Details Section
                    CardDetailSection(title = "Optical Lens System") {
                        DetailItem(label = "Focal Lengths", value = CameraCapabilityFormatter.formatFocalLengths(profile.lensProfile.focalLengths))
                        DetailItem(label = "Apertures", value = CameraCapabilityFormatter.formatApertures(profile.lensProfile.apertures))
                        DetailItem(label = "Min Focus Distance", value = profile.lensProfile.minFocusDistance?.let { "${it}m (Focus bounds)" } ?: "Infinite only")
                        DetailItem(label = "Distance Calibration", value = profile.lensProfile.focusDistanceCalibration ?: "N/A")
                        DetailItem(label = "AF Modes", value = profile.lensProfile.availableAfModes.joinToString(", ") { CameraCapabilityFormatter.formatAfMode(it) })
                        DetailItem(label = "OIS Support", value = profile.lensProfile.opticalStabilizationModes.joinToString(", ").ifEmpty { "N/A" })
                        DetailItem(label = "Zoom Ratio Range", value = profile.lensProfile.zoomRatioRange ?: "N/A")
                    }

                    // 3. Photo Configurations
                    CardDetailSection(title = "Still Image Formats") {
                        DetailItem(label = "JPEG Resolutions", value = profile.photoProfile.jpegSizes.take(4).joinToString(", ") + if (profile.photoProfile.jpegSizes.size > 4) "..." else "")
                        DetailItem(label = "RAW DNG Sizes", value = profile.photoProfile.rawSizes.ifEmpty { listOf("Unsupported") }.joinToString(", "))
                        DetailItem(label = "YUV Output Sizes", value = profile.photoProfile.yuvSizes.take(3).joinToString(", ") + if (profile.photoProfile.yuvSizes.size > 3) "..." else "")
                        DetailItem(label = "Largest JPEG Output", value = profile.photoProfile.largestJpeg ?: "N/A")
                        DetailItem(label = "Largest RAW Output", value = profile.photoProfile.largestRaw ?: "Unsupported")
                    }

                    // 4. Video Configurations
                    CardDetailSection(title = "Video Capture Profiles") {
                        DetailItem(label = "Available Framerates", value = profile.videoProfile.fpsRanges.joinToString(", "))
                        DetailItem(label = "Stabilization Modes", value = profile.videoProfile.stabilizationModes.joinToString(", ").ifEmpty { "N/A" })
                        DetailItem(label = "High Speed Slow-Mo", value = if (profile.videoProfile.supportsHighSpeedVideo) "Supported" else "Unsupported")
                        if (profile.videoProfile.supportsHighSpeedVideo) {
                            DetailItem(label = "Slow-Mo Sizes", value = profile.videoProfile.highSpeedVideoSizes.take(3).joinToString(", "))
                            DetailItem(label = "Slow-Mo Framerates", value = profile.videoProfile.highSpeedFpsRanges.joinToString(", "))
                        }
                    }

                    // 5. OEM Camera Extensions Section
                    CardDetailSection(title = "OEM API Extensions (Android 12+)") {
                        DetailExtensionItem(label = "Auto Brightness/Adjust", isSupported = profile.extensionProfile.supportsAuto)
                        DetailExtensionItem(label = "HDR (High Dynamic Range)", isSupported = profile.extensionProfile.supportsHdr)
                        DetailExtensionItem(label = "Night Capture Boost", isSupported = profile.extensionProfile.supportsNight)
                        DetailExtensionItem(label = "Portrait/Bokeh Mode", isSupported = profile.extensionProfile.supportsBokeh)
                        DetailExtensionItem(label = "Face Retouch / Beauty", isSupported = profile.extensionProfile.supportsFaceRetouch)
                        
                        if (profile.extensionProfile.notes.isNotEmpty()) {
                            Text(
                                text = "Notes: " + profile.extensionProfile.notes.joinToString("; "),
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Logical & Physical Architecture
                    CardDetailSection(title = "Logical & Physical Architecture") {
                        DetailItem(label = "Directly Openable (Camera2 ID List)", value = if (profile.openableCameraId) "YES" else "NO")
                        DetailItem(label = "Is Logical Multi-Camera", value = if (profile.isLogicalMultiCamera) "YES" else "NO")
                        DetailItem(label = "Physical Sub-Camera Count", value = "${profile.physicalCameraIds.size}")
                        
                        if (profile.physicalCameraIds.isNotEmpty()) {
                            DetailItem(
                                label = "Physical Sub-Camera IDs",
                                value = profile.physicalCameraIds.joinToString(", ") { id ->
                                    val isDirectlyOpenable = allOpenableIds.contains(id)
                                    if (isDirectlyOpenable) "$id (openable)" else "$id (non-openable directly, physical sub-camera)"
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Physical camera IDs may not be directly openable. They may be usable only through the logical camera session if the OEM exposes them.",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp
                        )

                        if (profile.cameraId == "0" && profile.physicalCameraIds.isEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF331111)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "OEM Info",
                                        tint = Color(0xFFE74C3C),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "OEM did not expose auxiliary rear camera through Camera2 physical camera IDs.",
                                        fontSize = 10.sp,
                                        color = Color(0xFFE74C3C),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Raw Camera2 Debug Information Section
                    var isDebugExpanded by remember { mutableStateOf(false) }
                    val debugRotationState by animateFloatAsState(targetValue = if (isDebugExpanded) 180f else 0f)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF15151A))
                                .clickable { isDebugExpanded = !isDebugExpanded }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Raw Camera2 Debug Information",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand/Collapse Debug",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(debugRotationState)
                            )
                        }
                        
                        AnimatedVisibility(visible = isDebugExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF111115))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DetailItem(label = "Camera ID", value = profile.cameraId)
                                DetailItem(label = "LENS_FACING", value = profile.facing.name)
                                DetailItem(label = "Logical Multi-Camera", value = "${profile.isLogicalMultiCamera}")
                                DetailItem(label = "Physical IDs List", value = profile.physicalCameraIds.joinToString(", ").ifEmpty { "None" })
                                DetailItem(label = "Focal Lengths", value = profile.lensProfile.focalLengths.toString())
                                DetailItem(label = "Apertures", value = profile.lensProfile.apertures.toString())
                                DetailItem(label = "Zoom Ratio Range", value = profile.lensProfile.zoomRatioRange ?: "N/A")
                                DetailItem(label = "Active Array Size", value = profile.sensorProfile.activeArraySize ?: "N/A")
                                DetailItem(label = "Available Output Formats", value = "JPEG, RAW, YUV, PRIVATE")
                                DetailItem(label = "Stream Sizes (JPEG)", value = profile.photoProfile.jpegSizes.take(5).joinToString(", ") + if (profile.photoProfile.jpegSizes.size > 5) "..." else "")
                                DetailItem(label = "Stream Sizes (RAW)", value = profile.photoProfile.rawSizes.joinToString(", ").ifEmpty { "None" })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureStatusChip(
    label: String,
    isSupported: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSupported) Color(0xFF1B3B2B) else Color(0xFF3B1E1E))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSupported) Color(0xFF2ECC71) else Color(0xFFE74C3C)
        )
    }
}

@Composable
fun CardDetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF15151A))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DetailExtensionItem(label: String, isSupported: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(
            text = if (isSupported) "SUPPORTED" else "UNSUPPORTED",
            fontSize = 10.sp,
            color = if (isSupported) Color(0xFF2ECC71) else Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WarningCalloutSection(warnings: List<String>) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF332211))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Warning",
                tint = Color(0xFFF1C40F),
                modifier = Modifier.size(20.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Hardware Integration Warnings",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1C40F)
                )

                warnings.forEach { warning ->
                    Text(
                        text = "• $warning",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
