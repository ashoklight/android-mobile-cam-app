package com.pna.omnicamlab.camera.core

import com.pna.omnicamlab.camera.capabilities.CameraSize
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewSizeSelectorTest {

    private val availableSizes = listOf(
        CameraSize(3840, 2160), // Huge 16:9 (exceeds limit)
        CameraSize(1920, 1080), // Normal 16:9
        CameraSize(1440, 1080), // Normal 4:3
        CameraSize(1280, 720),  // Normal 16:9
        CameraSize(1280, 960),  // Normal 4:3
        CameraSize(640, 480)    // Low 4:3
    )

    @Test
    fun selectPreviewSize_photoWysiwyg_choosesClosestToJpegAspect() {
        // Still JPEG is 4096x3072 (4:3 ratio = 1.333)
        val jpegSize = CameraSize(4096, 3072)
        val result = PreviewSizeSelector.selectPreviewSize(
            availableSizes = availableSizes,
            stillCaptureSize = jpegSize,
            aspectMode = PreviewAspectMode.PHOTO_WYSIWYG,
            screenAspectRatio = 16f / 9f
        )
        // 1440x1080 is the largest safe 4:3 preview size
        assertEquals(CameraSize(1440, 1080), result)
    }

    @Test
    fun selectPreviewSize_video169_chooses169() {
        val jpegSize = CameraSize(4096, 3072)
        val result = PreviewSizeSelector.selectPreviewSize(
            availableSizes = availableSizes,
            stillCaptureSize = jpegSize,
            aspectMode = PreviewAspectMode.VIDEO_16_9,
            screenAspectRatio = 16f / 9f
        )
        // 1920x1080 is the largest safe 16:9 preview size (3840x2160 filtered out)
        assertEquals(CameraSize(1920, 1080), result)
    }

    @Test
    fun selectPreviewSize_screenFill_choosesClosestToScreenAspect() {
        val jpegSize = CameraSize(4096, 3072)
        // Screen aspect: 1280x2772 (approx 2.16 ratio)
        // Closest is 16:9 (1.777 ratio) rather than 4:3 (1.333 ratio)
        val result = PreviewSizeSelector.selectPreviewSize(
            availableSizes = availableSizes,
            stillCaptureSize = jpegSize,
            aspectMode = PreviewAspectMode.SCREEN_FILL,
            screenAspectRatio = 2772f / 1280f
        )
        assertEquals(CameraSize(1920, 1080), result)
    }

    @Test
    fun selectPreviewSize_avoidsHugePreviewSizes() {
        val result = PreviewSizeSelector.selectPreviewSize(
            availableSizes = listOf(CameraSize(3840, 2160), CameraSize(1920, 1080)),
            stillCaptureSize = CameraSize(4096, 3072),
            aspectMode = PreviewAspectMode.VIDEO_16_9,
            screenAspectRatio = 16f / 9f
        )
        // 3840x2160 is >1920x1080, so 1920x1080 should be selected
        assertEquals(CameraSize(1920, 1080), result)
    }
}
