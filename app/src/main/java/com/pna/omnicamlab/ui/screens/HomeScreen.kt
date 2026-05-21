package com.pna.omnicamlab.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun HomeScreen(
  onNavigateToCamera: () -> Unit,
  onNavigateToReport: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onRedirectToOnboarding: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // Verify camera permission is still active
  LaunchedEffect(Unit) {
    val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    if (!hasCamera) {
      onRedirectToOnboarding()
    }
  }

  // High-fidelity dark mode background gradient
  val backgroundBrush = Brush.verticalGradient(
    colors = listOf(
      Color(0xFF0F0F14), // Deep Space Blue-Grey
      Color(0xFF16161D), 
      Color(0xFF0D0D10)
    )
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(backgroundBrush),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // Header Section
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = "SensorCam Pro",
          fontSize = 36.sp,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center
        )
        Text(
          text = "OmniCam Lab Suite",
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.tertiary,
          textAlign = TextAlign.Center
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Navigation grid / list
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        MenuButton(
          title = "Start Camera Engine",
          subtitle = "Access manual viewfinder & controls",
          icon = Icons.Default.PlayArrow,
          tint = MaterialTheme.colorScheme.primary,
          onClick = onNavigateToCamera
        )

        MenuButton(
          title = "Device Capability Report",
          subtitle = "Audit lenses, levels, and resolutions",
          icon = Icons.Default.Info,
          tint = Color(0xFF00C8FF), // Vivid cyan for data/report
          onClick = onNavigateToReport
        )

        MenuButton(
          title = "Capture Recipes & Modes",
          subtitle = "Presets for Astro, Macro, Storms",
          icon = Icons.Default.Star,
          tint = Color(0xFFFFC800), // Vivid gold for modes
          onClick = {} // Placeholder for Phase 2
        )

        MenuButton(
          title = "Preferences & Diagnostics",
          subtitle = "Storage folders, logs, telemetry",
          icon = Icons.Default.Settings,
          tint = MaterialTheme.colorScheme.secondary,
          onClick = onNavigateToSettings
        )
      }

      Spacer(modifier = Modifier.height(30.dp))

      // Footnote
      Text(
        text = "Verifying hardware bindings • Active package: com.pna.omnicamlab",
        fontSize = 10.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun MenuButton(
  title: String,
  subtitle: String,
  icon: ImageVector,
  tint: Color,
  onClick: () -> Unit
) {
  Button(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = Color(0xFF1E1E26), // Premium dark surface
      contentColor = Color.White
    ),
    contentPadding = PaddingValues(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .height(84.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF2E2E3A)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = tint,
          modifier = Modifier.size(28.dp)
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = title,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = Color.White
        )
        Text(
          text = subtitle,
          fontSize = 11.sp,
          color = Color.Gray
        )
      }
    }
  }
}
