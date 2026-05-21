package com.pna.omnicamlab.camera.core

sealed class CameraError {
    object PermissionMissing : CameraError()
    object CameraInUse : CameraError()
    object MaxCamerasInUse : CameraError()
    object CameraDisabled : CameraError()
    object CameraFatalError : CameraError()
    object SessionConfigurationFailed : CameraError()
    data class Unknown(val exception: Throwable?) : CameraError()

    fun toUserMessage(): String {
        return when (this) {
            is PermissionMissing -> "Camera permissions were denied or revoked. Please grant access in settings."
            is CameraInUse -> "The camera is currently being used by another application."
            is MaxCamerasInUse -> "Too many active camera sessions are open on this device."
            is CameraDisabled -> "Camera access has been disabled by security policy."
            is CameraFatalError -> "A critical hardware camera error occurred. Please restart the device."
            is SessionConfigurationFailed -> "Failed to configure the preview capture session pipeline."
            is Unknown -> "An unexpected error occurred: ${exception?.message ?: "Unknown error"}"
        }
    }
}
