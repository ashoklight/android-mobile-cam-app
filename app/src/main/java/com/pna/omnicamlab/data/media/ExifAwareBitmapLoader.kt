package com.pna.omnicamlab.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.pna.omnicamlab.util.logging.OmniLogger

object ExifAwareBitmapLoader {

    /**
     * Map EXIF orientation tag value to absolute degrees rotation.
     */
    fun mapExifOrientationToDegrees(exifOrientation: Int): Int {
        return when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    /**
     * Loads a bitmap from a content Uri, determines EXIF orientation, and rotates the bitmap if necessary.
     * Uses downsampling to avoid out-of-memory errors.
     */
    fun loadExifAwareBitmap(context: Context, uriString: String, maxDimension: Int = 1080): Bitmap? {
        val uri = Uri.parse(uriString)
        var bitmap: Bitmap? = null
        var exifOrientation = ExifInterface.ORIENTATION_NORMAL

        try {
            // 1. Read EXIF orientation safely using ExifInterface
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exifInterface = ExifInterface(stream)
                exifOrientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Exception) {
            OmniLogger.e(OmniLogger.Tag.Error, "Failed to read EXIF orientation from $uriString", e)
        }

        try {
            // 2. Decode the bitmap safely with downsampling
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            // Calculate the correct inSampleSize
            val outWidth = options.outWidth
            val outHeight = options.outHeight
            var inSampleSize = 1
            if (outWidth > maxDimension || outHeight > maxDimension) {
                val halfWidth = outWidth / 2
                val halfHeight = outHeight / 2
                while ((halfWidth / inSampleSize) >= maxDimension && (halfHeight / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // Decode the actual downsampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            OmniLogger.e(OmniLogger.Tag.Error, "Failed to decode stream from $uriString", e)
        }

        // 3. Rotate bitmap if rotation degrees > 0
        val originalBitmap = bitmap ?: return null
        val degrees = mapExifOrientationToDegrees(exifOrientation)
        if (degrees == 0) {
            return originalBitmap
        }

        return try {
            val matrix = Matrix().apply {
                postRotate(degrees.toFloat())
            }
            val rotatedBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )
            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            rotatedBitmap
        } catch (e: Exception) {
            OmniLogger.e(OmniLogger.Tag.Error, "Failed to rotate bitmap by $degrees degrees", e)
            originalBitmap
        }
    }
}
