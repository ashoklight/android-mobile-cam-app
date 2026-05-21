package com.pna.omnicamlab.camera.core

import android.content.Context
import android.view.OrientationEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceOrientationMonitor(context: Context) {

    private val _orientation = MutableStateFlow(0)
    val orientation: StateFlow<Int> = _orientation.asStateFlow()

    private val listener: OrientationEventListener

    init {
        listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(rawDegrees: Int) {
                val quantized = quantizeOrientation(rawDegrees)
                if (quantized != -1) {
                    if (_orientation.value != quantized) {
                        _orientation.value = quantized
                    }
                }
            }
        }
    }

    fun start() {
        if (listener.canDetectOrientation()) {
            listener.enable()
        }
    }

    fun stop() {
        listener.disable()
    }

    companion object {
        const val ORIENTATION_UNKNOWN = -1

        fun quantizeOrientation(rawDegrees: Int): Int {
            if (rawDegrees == ORIENTATION_UNKNOWN) return -1

            val degrees = (rawDegrees % 360 + 360) % 360
            return when {
                degrees >= 315 || degrees < 45 -> 0
                degrees in 45 until 135 -> 90
                degrees in 135 until 225 -> 180
                else -> 270
            }
        }
    }
}
