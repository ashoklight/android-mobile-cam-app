package com.pna.omnicamlab.util.logging

import android.util.Log

object OmniLogger {

  enum class Tag {
    CapabilityScanner,
    CameraSession,
    CaptureRequest,
    CaptureResult,
    MediaStore,
    UIState,
    Extensions,
    Error
  }

  fun d(tag: Tag, message: String) {
    Log.d("OmniCam_${tag.name}", message)
  }

  fun i(tag: Tag, message: String) {
    Log.i("OmniCam_${tag.name}", message)
  }

  fun w(tag: Tag, message: String) {
    Log.w("OmniCam_${tag.name}", message)
  }

  fun e(tag: Tag, message: String, throwable: Throwable? = null) {
    if (throwable != null) {
      Log.e("OmniCam_${tag.name}", message, throwable)
    } else {
      Log.e("OmniCam_${tag.name}", message)
    }
  }
}
