package com.pna.omnicamlab.camera.capabilities

import com.pna.omnicamlab.camera.core.PreviewSizeSelector
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewSizeSelectionTest {

    @Test
    fun testSelects1080pWhenAvailableAndAspectFits() {
        val sizes = listOf(
            CameraSize(3840, 2160), // 4K (should be filtered out as too large)
            CameraSize(1920, 1080), // 1080p (ideal 16:9)
            CameraSize(1280, 720),  // 720p (16:9)
            CameraSize(640, 480)    // 480p (4:3)
        )
        // 16:9 target ratio (~1.77)
        val selected = PreviewSizeSelector.selectPreviewSize(sizes, 16f / 9f)
        assertEquals(CameraSize(1920, 1080), selected)
    }

    @Test
    fun testSelects720pWhen1080pUnavailableAndAspectFits() {
        val sizes = listOf(
            CameraSize(3840, 2160), // 4K
            CameraSize(1280, 720),  // 720p
            CameraSize(640, 480)    // 480p
        )
        val selected = PreviewSizeSelector.selectPreviewSize(sizes, 16f / 9f)
        assertEquals(CameraSize(1280, 720), selected)
    }

    @Test
    fun testFiltersOutLargeStillSizesToPreventMemoryStrain() {
        val sizes = listOf(
            CameraSize(6000, 4000), // Huge 24MP still size
            CameraSize(4000, 3000), // Huge 12MP still size
            CameraSize(1920, 1080)  // Safe 1080p size
        )
        // Even if aspect ratio fits or not, we should avoid sizes larger than 1920x1080
        val selected = PreviewSizeSelector.selectPreviewSize(sizes, 16f / 9f)
        assertEquals(CameraSize(1920, 1080), selected)
    }

    @Test
    fun testFallsBackToClosestAspectRatioWhenStandardUnavailable() {
        val sizes = listOf(
            CameraSize(1000, 1000), // 1:1
            CameraSize(800, 600)    // 4:3
        )
        // Target is 16:9 (1.77). 4:3 is 1.33, 1:1 is 1.0. So 4:3 is closer than 1:1.
        val selected = PreviewSizeSelector.selectPreviewSize(sizes, 16f / 9f)
        assertEquals(CameraSize(800, 600), selected)
    }

    @Test
    fun testNormalizesOrientationAndAspectCorrectly() {
        // Portrait target aspect ratio (9:16) should map correctly to landscape preview sizes
        val sizes = listOf(
            CameraSize(1920, 1080),
            CameraSize(640, 480)
        )
        val selected = PreviewSizeSelector.selectPreviewSize(sizes, 9f / 16f)
        assertEquals(CameraSize(1920, 1080), selected)
    }
}
