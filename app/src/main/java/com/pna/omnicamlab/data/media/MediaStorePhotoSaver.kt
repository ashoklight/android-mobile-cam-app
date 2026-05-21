package com.pna.omnicamlab.data.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.pna.omnicamlab.util.logging.OmniLogger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStorePhotoSaver {

    /**
     * Data structure holding save details for a successful capture result.
     */
    data class SaveResult(
        val uri: Uri,
        val displayName: String,
        val relativePath: String
    )

    /**
     * Saves JPEG bytes to the public Pictures folder under a session-specific subdirectory.
     * Run on the IO Dispatcher thread for asynchronous safety.
     *
     * Session Folder: Pictures/OmniCam/OmniCam_YYYYMMDD_HHMMSS_Photo/
     * File Name: IMG_YYYYMMDD_HHMMSS_cameraId_001.jpg
     */
    suspend fun savePhoto(
        context: Context,
        jpegBytes: ByteArray,
        cameraId: String,
        timestampMs: Long
    ): SaveResult = withContext(Dispatchers.IO) {
        val date = Date(timestampMs)
        val timeFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestampStr = timeFormatter.format(date)

        val sessionFolder = "OmniCam_${timestampStr}_Photo"
        val fileName = "IMG_${timestampStr}_${cameraId}_001.jpg"
        val relativePath = "Pictures/OmniCam/$sessionFolder"

        OmniLogger.i(OmniLogger.Tag.CameraSession, "Saving photo: $fileName to $relativePath")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ Scoped Storage (No broad storage permissions required for writing)
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                put(MediaStore.Images.Media.DATE_ADDED, timestampMs / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, timestampMs)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IOException("Failed to insert MediaStore record for still image capture.")

            var writeSuccessful = false
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jpegBytes)
                    outputStream.flush()
                } ?: throw IOException("Failed to open output stream for MediaStore Uri: $uri")

                writeSuccessful = true
                
                // Complete insert by clearing IS_PENDING
                val updateValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, updateValues, null, null)
                OmniLogger.i(OmniLogger.Tag.CameraSession, "Successfully saved photo to Scoped Storage: $uri")
                
                SaveResult(uri = uri, displayName = fileName, relativePath = relativePath)

            } catch (e: Exception) {
                OmniLogger.e(OmniLogger.Tag.Error, "Error writing to MediaStore Scoped Storage, cleaning up pending record...", e)
                if (!writeSuccessful) {
                    try {
                        resolver.delete(uri, null, null)
                    } catch (cleanupEx: Exception) {
                        OmniLogger.e(OmniLogger.Tag.Error, "Failed to clean up pending record: $uri", cleanupEx)
                    }
                }
                throw e
            }
        } else {
            // Android 9 / API 28 Legacy File Saving
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val omniCamDir = File(picturesDir, "OmniCam/$sessionFolder")
            if (!omniCamDir.exists()) {
                val created = omniCamDir.mkdirs()
                if (!created && !omniCamDir.isDirectory) {
                    throw IOException("Failed to create directory path: ${omniCamDir.absolutePath}")
                }
            }

            val file = File(omniCamDir, fileName)
            try {
                FileOutputStream(file).use { fileOutputStream ->
                    fileOutputStream.write(jpegBytes)
                    fileOutputStream.flush()
                }
                OmniLogger.i(OmniLogger.Tag.CameraSession, "Successfully saved photo to legacy external path: ${file.absolutePath}")

                // Scan the file so it displays instantly in external gallery apps
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )

                val fileUri = Uri.fromFile(file)
                SaveResult(uri = fileUri, displayName = fileName, relativePath = relativePath)

            } catch (e: Exception) {
                OmniLogger.e(OmniLogger.Tag.Error, "Error writing to legacy external path: ${file.absolutePath}", e)
                if (file.exists()) {
                    file.delete()
                }
                throw e
            }
        }
    }
}
