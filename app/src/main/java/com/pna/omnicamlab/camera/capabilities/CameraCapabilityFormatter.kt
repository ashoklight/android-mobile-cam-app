package com.pna.omnicamlab.camera.capabilities

import java.util.Locale

object CameraCapabilityFormatter {

    fun formatFocalLength(fl: Float): String {
        return String.format(Locale.ROOT, "%.2fmm", fl)
    }

    fun formatAperture(ap: Float): String {
        return String.format(Locale.ROOT, "f/%.2f", ap)
    }

    fun formatFocalLengths(fls: List<Float>): String {
        if (fls.isEmpty()) return "N/A"
        return fls.joinToString(", ") { formatFocalLength(it) }
    }

    fun formatApertures(aps: List<Float>): String {
        if (aps.isEmpty()) return "N/A"
        return aps.joinToString(", ") { formatAperture(it) }
    }

    fun formatIsoRange(minIso: Int?, maxIso: Int?): String {
        if (minIso == null || maxIso == null) return "Unknown Range"
        return "ISO $minIso - $maxIso"
    }

    /**
     * Translates exposure time in nanoseconds to a clean photographic string.
     * Examples: 125000 ns -> 1/8000s, 30000000000 ns -> 30s
     */
    fun formatExposureTime(ns: Long): String {
        if (ns <= 0L) return "0s"
        
        val seconds = ns.toDouble() / 1_000_000_000.0
        return if (seconds >= 1.0) {
            // Whole seconds or decimal seconds
            if (seconds == Math.floor(seconds)) {
                "${seconds.toLong()}s"
            } else {
                String.format(Locale.ROOT, "%.1fs", seconds)
            }
        } else {
            // Fractional seconds (e.g. 1/125s)
            val reciprocal = 1_000_000_000.0 / ns
            if (reciprocal >= 1.0) {
                val roundedReciprocal = Math.round(reciprocal)
                "1/${roundedReciprocal}s"
            } else {
                // If it's incredibly tiny, display in milliseconds
                val ms = ns.toDouble() / 1_000_000.0
                String.format(Locale.ROOT, "%.3fms", ms)
            }
        }
    }

    fun formatExposureTimeRange(minNs: Long?, maxNs: Long?): String {
        if (minNs == null || maxNs == null) return "Unknown Range"
        return "${formatExposureTime(minNs)} - ${formatExposureTime(maxNs)}"
    }

    fun formatFocusCalibration(calibration: Int?): String {
        return when (calibration) {
            0 -> "UNCALIBRATED" // CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_UNCALIBRATED
            1 -> "APPROXIMATE"  // CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_APPROXIMATE
            2 -> "CALIBRATED"   // CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_CALIBRATED
            else -> "UNKNOWN"
        }
    }

    fun formatHardwareLevel(level: HardwareLevel): String {
        return when (level) {
            HardwareLevel.LEGACY -> "Legacy (LEGACY)"
            HardwareLevel.LIMITED -> "Limited (LIMITED)"
            HardwareLevel.FULL -> "Full (FULL)"
            HardwareLevel.LEVEL_3 -> "Level 3 (LEVEL_3)"
            HardwareLevel.EXTERNAL -> "External (EXTERNAL)"
            HardwareLevel.UNKNOWN -> "Unknown"
        }
    }

    fun formatAfMode(modeStr: String): String {
        return when (modeStr) {
            "OFF" -> "Manual Focus (OFF)"
            "AUTO" -> "Single AF (AUTO)"
            "MACRO" -> "Macro AF (MACRO)"
            "CONTINUOUS_VIDEO" -> "Continuous Video AF"
            "CONTINUOUS_PICTURE" -> "Continuous Picture AF"
            "EDOF" -> "Extended Depth of Field (EDOF)"
            else -> modeStr
        }
    }
}
