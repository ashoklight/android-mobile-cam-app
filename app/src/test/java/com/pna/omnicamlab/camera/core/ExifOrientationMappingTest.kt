package com.pna.omnicamlab.camera.core

import android.media.ExifInterface
import com.pna.omnicamlab.data.media.ExifAwareBitmapLoader
import org.junit.Assert.assertEquals
import org.junit.Test

class ExifOrientationMappingTest {

    @Test
    fun verifyExifOrientationToDegreesMapping() {
        assertEquals(0, ExifAwareBitmapLoader.mapExifOrientationToDegrees(ExifInterface.ORIENTATION_NORMAL))
        assertEquals(90, ExifAwareBitmapLoader.mapExifOrientationToDegrees(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180, ExifAwareBitmapLoader.mapExifOrientationToDegrees(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270, ExifAwareBitmapLoader.mapExifOrientationToDegrees(ExifInterface.ORIENTATION_ROTATE_270))
        assertEquals(0, ExifAwareBitmapLoader.mapExifOrientationToDegrees(ExifInterface.ORIENTATION_UNDEFINED))
    }
}
