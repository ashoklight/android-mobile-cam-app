package com.pna.omnicamlab.camera.capabilities

import android.content.Context
import com.pna.omnicamlab.util.logging.OmniLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CameraCapabilityJsonExporter {

    fun exportToJsonString(profiles: List<CameraDeviceProfile>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        profiles.forEachIndexed { index, profile ->
            sb.append(profileToJson(profile, "  "))
            if (index < profiles.size - 1) {
                sb.append(",\n")
            } else {
                sb.append("\n")
            }
        }
        sb.append("]")
        return sb.toString()
    }

    private fun profileToJson(profile: CameraDeviceProfile, indent: String): String {
        val sb = StringBuilder()
        sb.append("$indent{\n")
        val inner = "$indent  "

        sb.append("$inner\"cameraId\": \"${profile.cameraId}\",\n")
        sb.append("$inner\"facing\": \"${profile.facing.name}\",\n")
        sb.append("$inner\"hardwareLevel\": \"${profile.hardwareLevel.name}\",\n")
        sb.append("$inner\"isLogicalMultiCamera\": ${profile.isLogicalMultiCamera},\n")
        sb.append("$inner\"openableCameraId\": ${profile.openableCameraId},\n")
        
        // physicalCameraIds
        sb.append("$inner\"physicalCameraIds\": [")
        sb.append(profile.physicalCameraIds.joinToString(", ") { "\"$it\"" })
        sb.append("],\n")

        // capabilities list
        sb.append("$inner\"capabilities\": [")
        sb.append(profile.capabilities.joinToString(", ") { "\"${it.name}\"" })
        sb.append("],\n")

        // lensProfile
        sb.append("$inner\"lensProfile\": {\n")
        val sub = "$inner  "
        sb.append("$sub\"focalLengths\": [${profile.lensProfile.focalLengths.joinToString(", ")}],\n")
        sb.append("$sub\"apertures\": [${profile.lensProfile.apertures.joinToString(", ")}],\n")
        sb.append("$sub\"minFocusDistance\": ${profile.lensProfile.minFocusDistance ?: "null"},\n")
        sb.append("$sub\"focusDistanceCalibration\": ${profile.lensProfile.focusDistanceCalibration?.let { "\"$it\"" } ?: "null"},\n")
        
        sb.append("$sub\"opticalStabilizationModes\": [")
        sb.append(profile.lensProfile.opticalStabilizationModes.joinToString(", ") { "\"$it\"" })
        sb.append("],\n")
        
        sb.append("$sub\"availableAfModes\": [")
        sb.append(profile.lensProfile.availableAfModes.joinToString(", ") { "\"$it\"" })
        sb.append("],\n")
        
        sb.append("$sub\"lensFacingRaw\": ${profile.lensProfile.lensFacingRaw ?: "null"},\n")
        sb.append("$sub\"zoomRatioRange\": ${profile.lensProfile.zoomRatioRange?.let { "\"$it\"" } ?: "null"}\n")
        sb.append("$inner},\n")

        // sensorProfile
        sb.append("$inner\"sensorProfile\": {\n")
        sb.append("$sub\"isoRange\": ${profile.sensorProfile.isoRange?.let { "\"$it\"" } ?: "null"},\n")
        sb.append("$sub\"exposureTimeRangeNs\": ${profile.sensorProfile.exposureTimeRangeNs?.let { "\"$it\"" } ?: "null"},\n")
        sb.append("$sub\"maxFrameDurationNs\": ${profile.sensorProfile.maxFrameDurationNs ?: "null"},\n")
        sb.append("$sub\"activeArraySize\": ${profile.sensorProfile.activeArraySize?.let { "\"$it\"" } ?: "null"},\n")
        sb.append("$sub\"pixelArraySize\": ${profile.sensorProfile.pixelArraySize?.let { "\"$it\"" } ?: "null"},\n")
        sb.append("$sub\"sensorOrientation\": ${profile.sensorProfile.sensorOrientation ?: "null"},\n")
        sb.append("$sub\"colorFilterArrangement\": ${profile.sensorProfile.colorFilterArrangement?.let { "\"$it\"" } ?: "null"},\n")
        sb.append("$sub\"timestampSource\": ${profile.sensorProfile.timestampSource?.let { "\"$it\"" } ?: "null"}\n")
        sb.append("$inner},\n")

        // photoProfile
        sb.append("$inner\"photoProfile\": {\n")
        sb.append("$sub\"jpegSizes\": [${profile.photoProfile.jpegSizes.joinToString(", ") { "\"$it\"" }}],\n")
        sb.append("$sub\"rawSizes\": [${profile.photoProfile.rawSizes.joinToString(", ") { "\"$it\"" }}],\n")
        sb.append("$sub\"yuvSizes\": [${profile.photoProfile.yuvSizes.joinToString(", ") { "\"$it\"" }}],\n")
        sb.append("$sub\"privateSizes\": [${profile.photoProfile.privateSizes.joinToString(", ") { "\"$it\"" }}],\n")
        sb.append("$sub\"supportsRaw\": ${profile.photoProfile.supportsRaw},\n")
        sb.append("$sub\"supportsBurst\": ${profile.photoProfile.supportsBurst},\n")
        sb.append("$sub\"largestJpeg\": ${profile.photoProfile.largestJpeg?.let { "\"$it\"" } ?: "null"},\n")
        sb.append("$sub\"largestRaw\": ${profile.photoProfile.largestRaw?.let { "\"$it\"" } ?: "null"},\n")
        sb.append("$sub\"largestYuv\": ${profile.photoProfile.largestYuv?.let { "\"$it\"" } ?: "null"}\n")
        sb.append("$inner},\n")

        // videoProfile
        sb.append("$inner\"videoProfile\": {\n")
        sb.append("$sub\"fpsRanges\": [${profile.videoProfile.fpsRanges.joinToString(", ") { "\"$it\"" }}],\n")
        sb.append("$sub\"highSpeedVideoSizes\": [${profile.videoProfile.highSpeedVideoSizes.joinToString(", ") { "\"$it\"" }}],\n")
        sb.append("$sub\"highSpeedFpsRanges\": [${profile.videoProfile.highSpeedFpsRanges.joinToString(", ") { "\"$it\"" }}],\n")
        sb.append("$sub\"stabilizationModes\": [${profile.videoProfile.stabilizationModes.joinToString(", ") { "\"$it\"" }}],\n")
        sb.append("$sub\"supportsHighSpeedVideo\": ${profile.videoProfile.supportsHighSpeedVideo}\n")
        sb.append("$inner},\n")

        // extensionProfile
        sb.append("$inner\"extensionProfile\": {\n")
        sb.append("$sub\"supportsAuto\": ${profile.extensionProfile.supportsAuto},\n")
        sb.append("$sub\"supportsHdr\": ${profile.extensionProfile.supportsHdr},\n")
        sb.append("$sub\"supportsNight\": ${profile.extensionProfile.supportsNight},\n")
        sb.append("$sub\"supportsBokeh\": ${profile.extensionProfile.supportsBokeh},\n")
        sb.append("$sub\"supportsFaceRetouch\": ${profile.extensionProfile.supportsFaceRetouch},\n")
        sb.append("$sub\"notes\": [${profile.extensionProfile.notes.joinToString(", ") { "\"$it\"" }}]\n")
        sb.append("$inner},\n")

        // supportWarnings
        sb.append("$inner\"supportWarnings\": [")
        sb.append(profile.supportWarnings.joinToString(", ") { "\"$it\"" })
        sb.append("]\n")

        sb.append("$indent}")
        return sb.toString()
    }

    /**
     * Saves JSON string report to external app files directory dynamically (no permissions required).
     */
    fun saveReport(context: Context, jsonString: String): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
            val fileName = "OmniCam_Capability_Report_$timestamp.json"
            // Use external app-specific documents folder (permission-free)
            val docDir = context.getExternalFilesDir("documents") ?: context.filesDir
            if (!docDir.exists()) {
                docDir.mkdirs()
            }
            val file = File(docDir, fileName)
            file.writeText(jsonString)
            OmniLogger.i(OmniLogger.Tag.CapabilityScanner, "Saved capability JSON report to: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            OmniLogger.e(OmniLogger.Tag.CapabilityScanner, "Failed to save JSON capability report", e)
            null
        }
    }
}
