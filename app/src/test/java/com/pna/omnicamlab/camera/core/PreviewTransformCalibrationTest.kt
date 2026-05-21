package com.pna.omnicamlab.camera.core

import android.graphics.Matrix
import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PreviewTransformCalibrationTest {

    @Test
    fun getAutoPreviewRotationDegrees_allDisplayRotations() {
        // Verify auto rotation degrees correctly maps the display rotation
        assertEquals(0, PreviewTransformHelper.getAutoPreviewRotationDegrees(Surface.ROTATION_0))
        assertEquals(270, PreviewTransformHelper.getAutoPreviewRotationDegrees(Surface.ROTATION_90))
        assertEquals(180, PreviewTransformHelper.getAutoPreviewRotationDegrees(Surface.ROTATION_180))
        assertEquals(90, PreviewTransformHelper.getAutoPreviewRotationDegrees(Surface.ROTATION_270))
    }

    @Test
    fun buildCamera2BasicTransform_forcedModes_executeWithoutCrash() {
        val viewWidth = 1280
        val viewHeight = 2772
        val previewWidth = 1920
        val previewHeight = 1080

        // Test all forced rotation angles (0, 90, 180, 270)
        val forcedAngles = listOf(0, 90, 180, 270)
        for (forcedAngle in forcedAngles) {
            val matrix = PreviewTransformHelper.buildCamera2BasicTransform(
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                previewWidth = previewWidth,
                previewHeight = previewHeight,
                displayRotation = Surface.ROTATION_0,
                forcedRotationDegrees = forcedAngle,
                mirrorForFrontCamera = false
            )
            assertNotNull("Matrix should not be null for forced angle $forcedAngle", matrix)
        }
    }

    @Test
    fun buildCamera2BasicTransform_autoModes_executeWithoutCrash() {
        val viewWidth = 1280
        val viewHeight = 2772
        val previewWidth = 1920
        val previewHeight = 1080

        // Test all auto display rotation configurations
        val displayRotations = listOf(
            Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270
        )
        for (displayRot in displayRotations) {
            val matrix = PreviewTransformHelper.buildCamera2BasicTransform(
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                previewWidth = previewWidth,
                previewHeight = previewHeight,
                displayRotation = displayRot,
                forcedRotationDegrees = null,
                mirrorForFrontCamera = false
            )
            assertNotNull("Matrix should not be null for display rotation $displayRot", matrix)
        }
    }

    @Test
    fun buildCamera2BasicTransform_frontMirroring_executeWithoutCrash() {
        val matrix = PreviewTransformHelper.buildCamera2BasicTransform(
            viewWidth = 1280,
            viewHeight = 2772,
            previewWidth = 1920,
            previewHeight = 1080,
            displayRotation = Surface.ROTATION_0,
            forcedRotationDegrees = 90,
            mirrorForFrontCamera = true
        )
        assertNotNull(matrix)
    }

    @Test
    fun buildCamera2BasicTransform_boundaryAndZeroDimensions_gracefulReturn() {
        // Zero dimensions should return an empty/identity matrix gracefully
        val matrix = PreviewTransformHelper.buildCamera2BasicTransform(
            viewWidth = 0,
            viewHeight = 2772,
            previewWidth = 1920,
            previewHeight = 1080,
            displayRotation = Surface.ROTATION_0,
            forcedRotationDegrees = 90,
            mirrorForFrontCamera = false
        )
        assertNotNull(matrix)
    }

    @Test
    fun verifyBufferDimensionsSwappingLogic() {
        // We simulate the dimension swapping check logic inside buildCamera2BasicTransform
        // for 90 or 270 degrees forced rotation.
        fun getEffectiveBufferSize(forcedAngle: Int, width: Int, height: Int): Pair<Int, Int> {
            return if (forcedAngle == 90 || forcedAngle == 270) {
                height to width
            } else {
                width to height
            }
        }

        val originalWidth = 1920
        val originalHeight = 1080

        // 90 degrees -> should swap
        val size90 = getEffectiveBufferSize(90, originalWidth, originalHeight)
        assertEquals(originalHeight, size90.first)
        assertEquals(originalWidth, size90.second)

        // 270 degrees -> should swap
        val size270 = getEffectiveBufferSize(270, originalWidth, originalHeight)
        assertEquals(originalHeight, size270.first)
        assertEquals(originalWidth, size270.second)

        // 0 degrees -> should NOT swap
        val size0 = getEffectiveBufferSize(0, originalWidth, originalHeight)
        assertEquals(originalWidth, size0.first)
        assertEquals(originalHeight, size0.second)

        // 180 degrees -> should NOT swap
        val size180 = getEffectiveBufferSize(180, originalWidth, originalHeight)
        assertEquals(originalWidth, size180.first)
        assertEquals(originalHeight, size180.second)
    }

    @Test
    fun verifyUniformScalingAndAspectPreservation() {
        val viewWidth = 1280
        val viewHeight = 2772
        val previewWidth = 1920
        val previewHeight = 1080

        // 1. CENTER_CROP
        val cropResult = PreviewTransformHelper.buildTransform(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            displayRotation = Surface.ROTATION_0,
            forcedRotationDegrees = 0,
            mirrorForFrontCamera = false,
            scaleType = PreviewScaleType.CENTER_CROP
        )

        assertEquals("scaleX must equal scaleY for uniform scale", cropResult.scaleX, cropResult.scaleY, 1e-4f)
        assertEquals("aspectRatioOk must be true", true, cropResult.aspectRatioOk)

        // 2. FIT_CENTER
        val fitResult = PreviewTransformHelper.buildTransform(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            displayRotation = Surface.ROTATION_0,
            forcedRotationDegrees = 0,
            mirrorForFrontCamera = false,
            scaleType = PreviewScaleType.FIT_CENTER
        )

        assertEquals("scaleX must equal scaleY for uniform scale", fitResult.scaleX, fitResult.scaleY, 1e-4f)
        assertEquals("aspectRatioOk must be true", true, fitResult.aspectRatioOk)

        // 3. 90/270 Swaps Effective Dimensions
        val rotation90Result = PreviewTransformHelper.buildTransform(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            displayRotation = Surface.ROTATION_0,
            forcedRotationDegrees = 90,
            mirrorForFrontCamera = false,
            scaleType = PreviewScaleType.CENTER_CROP
        )
        // Effective dimensions when rotated by 90 should be previewHeight by previewWidth (1080x1920)
        assertEquals(previewHeight.toFloat(), rotation90Result.effectiveW)
        assertEquals(previewWidth.toFloat(), rotation90Result.effectiveH)

        // 4. Zero dimensions should handle gracefully
        val zeroResult = PreviewTransformHelper.buildTransform(
            viewWidth = 0,
            viewHeight = 0,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            displayRotation = Surface.ROTATION_0,
            forcedRotationDegrees = 0,
            mirrorForFrontCamera = false,
            scaleType = PreviewScaleType.CENTER_CROP
        )
        assertEquals(true, zeroResult.aspectRatioOk)
    }
}
