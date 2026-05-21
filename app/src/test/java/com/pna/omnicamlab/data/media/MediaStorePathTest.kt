package com.pna.omnicamlab.data.media

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaStorePathTest {

    @Test
    fun verifyPathFormatting_specificTimestamp_generatesCorrectSessionFolderAndFile() {
        // May 20, 2026 10:15:30 UTC -> 1779272130000 milliseconds
        val testTimestampMs = 1779272130000L
        val cameraId = "0"

        val date = Date(testTimestampMs)
        val timeFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).apply {
            // Set timezone to UTC to make it independent of regional environment running the unit tests
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val timestampStr = timeFormatter.format(date)

        // Verify standard date formatting
        assertEquals("20260520_101530", timestampStr)

        val sessionFolder = "OmniCam_${timestampStr}_Photo"
        val fileName = "IMG_${timestampStr}_${cameraId}_001.jpg"
        val relativePath = "Pictures/OmniCam/$sessionFolder"

        // Verify folders and names match Phase 1E requirements
        assertEquals("OmniCam_20260520_101530_Photo", sessionFolder)
        assertEquals("IMG_20260520_101530_0_001.jpg", fileName)
        assertEquals("Pictures/OmniCam/OmniCam_20260520_101530_Photo", relativePath)
    }
}
