package com.pna.omnicamlab

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Onboarding : NavKey
@Serializable data object Home : NavKey
@Serializable data object CapabilityReport : NavKey
@Serializable data object CameraCapture : NavKey
@Serializable
data class CaptureResult(
    val savedUri: String, // Keep this URL-encoded to prevent Navigation 3 route parsing bugs
    val cameraId: String,
    val facing: String,
    val jpegSize: String,
    val timestamp: Long,
    val orientation: Int,
    val iso: Int? = null,
    val exposureTimeNs: Long? = null,
    val focalLength: Float? = null,
    val aperture: Float? = null
) : NavKey

@Serializable data object Settings : NavKey

