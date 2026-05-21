package com.pna.omnicamlab.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureResultScreen(
  result: com.pna.omnicamlab.CaptureResult,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Decode URL-encoded savedUri safely
  val decodedUriStr = remember(result.savedUri) {
    try {
      java.net.URLDecoder.decode(result.savedUri, "UTF-8")
    } catch (e: Exception) {
      result.savedUri
    }
  }

  val context = androidx.compose.ui.platform.LocalContext.current
  var thumbnailBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

  // Load verification thumbnail asynchronously with downsampling and EXIF orientation mapping
  LaunchedEffect(decodedUriStr) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      try {
        thumbnailBitmap = com.pna.omnicamlab.data.media.ExifAwareBitmapLoader.loadExifAwareBitmap(
          context = context,
          uriString = decodedUriStr,
          maxDimension = 1080
        )
      } catch (e: Exception) {
        com.pna.omnicamlab.util.logging.OmniLogger.e(
          com.pna.omnicamlab.util.logging.OmniLogger.Tag.Error,
          "Failed to load verification thumbnail bitmap from Uri with EXIF awareness: $decodedUriStr",
          e
        )
      }
    }
  }

  // Format passive exposure time to a beautiful fractional string (e.g., 1/100s)
  val formattedExposure = remember(result.exposureTimeNs) {
    val ns = result.exposureTimeNs
    if (ns == null || ns <= 0) "Not available"
    else {
      val sec = ns / 1_000_000_000.0
      if (sec >= 1.0) {
        String.format(java.util.Locale.US, "%.2fs", sec)
      } else {
        val denominator = Math.round(1.0 / sec)
        "1/${denominator}s"
      }
    }
  }

  // Format timestamp and reconstruct MediaStore metadata details
  val timestampStr = remember(result.timestamp) {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
    formatter.format(java.util.Date(result.timestamp))
  }

  val mediaStoreName = remember(result.timestamp, result.cameraId) {
    val timeFormatter = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
    val timestampFormatted = timeFormatter.format(java.util.Date(result.timestamp))
    "IMG_${timestampFormatted}_${result.cameraId}_001.jpg"
  }

  val mediaStoreRelativePath = remember(result.timestamp) {
    val timeFormatter = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
    val timestampFormatted = timeFormatter.format(java.util.Date(result.timestamp))
    "Pictures/OmniCam/OmniCam_${timestampFormatted}_Photo/"
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Still Capture Verification", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color(0xFF1E1E26),
          titleContentColor = Color.White,
          navigationIconContentColor = Color.White
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0F0F14))
        .padding(paddingValues)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      
      // Image Verification Thumbnail Preview
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(260.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF1E1E26))
          .border(1.dp, Color(0xFF2E2E3A), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
      ) {
        val bitmap = thumbnailBitmap
        if (bitmap != null) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Capture verification thumbnail",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
          )
        } else {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Loading Verification Preview...", color = Color.Gray, fontSize = 12.sp)
          }
        }
      }

      // Success Indicator Card
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(20.dp))
              .background(Color(0xFF1E3A1E)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Check, contentDescription = "Success", tint = Color.Green)
          }
          Spacer(modifier = Modifier.width(16.dp))
          Column {
            Text("Photo Saved Successfully", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Indexed in device MediaStore database.", color = Color.Gray, fontSize = 12.sp)
          }
        }
      }

      // MediaStore Details Card
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("MediaStore Core Fields", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          HorizontalDivider(color = Color(0xFF2E2E3A))
          
          DetailRow(label = "URI", value = decodedUriStr)
          DetailRow(label = "Display Name", value = mediaStoreName)
          DetailRow(label = "Relative Path", value = mediaStoreRelativePath)
        }
      }

      // Passive Capture Metadata Card
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("Passive Capture Metadata", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          HorizontalDivider(color = Color(0xFF2E2E3A))
          
          DetailRow(label = "Camera Lens ID", value = result.cameraId)
          DetailRow(label = "Lens Facing", value = result.facing)
          DetailRow(label = "JPEG Capture Size", value = result.jpegSize)
          DetailRow(label = "Timestamp", value = timestampStr)
          DetailRow(label = "Sensor ISO", value = result.iso?.toString() ?: "Not available")
          DetailRow(label = "Exposure Time", value = formattedExposure)
          DetailRow(label = "Aperture", value = result.aperture?.let { "f/$it" } ?: "Not available")
          DetailRow(label = "Focal Length", value = result.focalLength?.let { "${it}mm" } ?: "Not available")
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Button(
        onClick = onNavigateBack,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Done")
      }
    }
  }
}

@Composable
fun DetailRow(label: String, value: String) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(2.dp))
    Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
  }
}
