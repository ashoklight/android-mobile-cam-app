package com.pna.omnicamlab.camera.core

import com.pna.omnicamlab.camera.capabilities.CameraDeviceProfile
import com.pna.omnicamlab.camera.capabilities.CameraSize

sealed class CameraSessionState {
    object Idle : CameraSessionState()
    
    data class Loading(val cameraId: String) : CameraSessionState()
    
    data class Previewing(
        val cameraId: String,
        val profile: CameraDeviceProfile,
        val previewSize: CameraSize
    ) : CameraSessionState()

    data class Capturing(
        val cameraId: String,
        val profile: CameraDeviceProfile,
        val previewSize: CameraSize
    ) : CameraSessionState()

    data class Saving(
        val cameraId: String,
        val profile: CameraDeviceProfile,
        val previewSize: CameraSize
    ) : CameraSessionState()

    data class CaptureSuccess(
        val cameraId: String,
        val profile: CameraDeviceProfile,
        val previewSize: CameraSize,
        val savedUri: String,
        val iso: Int? = null,
        val exposureTimeNs: Long? = null,
        val focalLength: Float? = null,
        val aperture: Float? = null
    ) : CameraSessionState()

    data class CaptureError(
        val cameraId: String,
        val profile: CameraDeviceProfile,
        val previewSize: CameraSize,
        val errorMessage: String
    ) : CameraSessionState()
    
    data class Switching(val fromId: String, val toId: String) : CameraSessionState()
    
    data class Error(val error: CameraError, val message: String) : CameraSessionState()
}
