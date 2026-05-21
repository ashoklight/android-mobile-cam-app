package com.pna.omnicamlab.camera.core

data class ZoomState(
    val minZoom: Float = 1.0f,
    val maxZoom: Float = 1.0f,
    val currentZoom: Float = 1.0f,
    val isZoomSupported: Boolean = false,
    val zoomBackend: String = "UNSUPPORTED"
)
