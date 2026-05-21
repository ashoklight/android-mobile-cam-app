package com.pna.omnicamlab.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun OnboardingScreen(
  onPermissionsGranted: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // State to track permissions
  var hasCameraPermission by remember {
    mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
  }
  var hasAudioPermission by remember {
    mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
  }

  // Permission launcher
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
    hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission

    // Camera is our core permission. Audio is optional for Phase 1.
    if (hasCameraPermission) {
      onPermissionsGranted()
    }
  }

  // Auto transition if core permission is already granted
  LaunchedEffect(hasCameraPermission) {
    if (hasCameraPermission) {
      onPermissionsGranted()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = "OmniCam Lab",
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
      )

      Text(
        text = "Capability-First Pro Photography",
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.secondary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "Info",
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Dynamic Capability Philosophy",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }

          Text(
            text = "OmniCam Lab scans your device's exact lens, sensor, and OEM characteristics. It dynamically adjusts controls and only shows features your hardware exposes through official APIs. No fake modes.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Status indicator list
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        PermissionStatusRow(
          title = "Camera Access (Core)",
          description = "Required for live preview, manual exposure, and capture.",
          isGranted = hasCameraPermission
        )

        PermissionStatusRow(
          title = "Microphone Access (Optional)",
          description = "Required for video audio capture.",
          isGranted = hasAudioPermission
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
          
          // Media/storage permissions depending on API level
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
          } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Only need write permission below Android 10
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
          }

          permissionLauncher.launch(permissionsToRequest.toTypedArray())
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
      ) {
        Text(
          text = if (hasCameraPermission) "Proceed to Dashboard" else "Grant Camera Access",
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
fun PermissionStatusRow(
  title: String,
  description: String,
  isGranted: Boolean
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(if (isGranted) Color(0xFF1E3A1E) else Color(0xFF2C2C2C))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Icon(
      imageVector = Icons.Default.CheckCircle,
      contentDescription = "Status",
      tint = if (isGranted) Color.Green else Color.Gray,
      modifier = Modifier.size(24.dp)
    )

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = if (isGranted) Color.White else Color.LightGray
      )
      Text(
        text = description,
        fontSize = 11.sp,
        color = if (isGranted) Color(0xFFB0DFB0) else Color.Gray
      )
    }
  }
}
