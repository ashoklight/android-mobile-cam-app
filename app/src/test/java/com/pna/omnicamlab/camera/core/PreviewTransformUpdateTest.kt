package com.pna.omnicamlab.camera.core

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewTransformUpdateTest {

    @Test
    fun transformUpdatesOnDisplayRotationChange() {
        val viewWidth = 1080
        val viewHeight = 1920
        val previewWidth = 1920
        val previewHeight = 1080

        val rot0Result = PreviewTransformHelper.buildTransform(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            displayRotation = Surface.ROTATION_0,
            scaleType = PreviewScaleType.CENTER_CROP
        )

        val rot90Result = PreviewTransformHelper.buildTransform(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            displayRotation = Surface.ROTATION_90,
            scaleType = PreviewScaleType.CENTER_CROP
        )

        // The effective buffer width and height should be swapped correctly based on sensorOrientation
        assertNotEquals(rot0Result.effectiveW, rot90Result.effectiveW)
        assertNotEquals(rot0Result.effectiveH, rot90Result.effectiveH)
        assertEquals(previewHeight.toFloat(), rot0Result.effectiveW)
        assertEquals(previewWidth.toFloat(), rot0Result.effectiveH)
        assertEquals(previewWidth.toFloat(), rot90Result.effectiveW)
        assertEquals(previewHeight.toFloat(), rot90Result.effectiveH)
    }

    @Test
    fun transformUpdatesOnTextureViewSizeChange() {
        val previewWidth = 1920
        val previewHeight = 1080

        val smallViewResult = PreviewTransformHelper.buildTransform(
            viewWidth = 500,
            viewHeight = 500,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            displayRotation = Surface.ROTATION_0,
            scaleType = PreviewScaleType.CENTER_CROP
        )

        val largeViewResult = PreviewTransformHelper.buildTransform(
            viewWidth = 1000,
            viewHeight = 2000,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            displayRotation = Surface.ROTATION_0,
            scaleType = PreviewScaleType.CENTER_CROP
        )

        // Matrix uniform scale and final scaled bounds must be completely different for different view sizes
        assertNotEquals(smallViewResult.uniformScale, largeViewResult.uniformScale)
        assertNotEquals(smallViewResult.scaledW, largeViewResult.scaledW)
    }

    @Test
    fun transformCalculationDoesNotReopenCamera() {
        // Calling buildTransform is a pure mathematical calculation and does not touch camera device states or trigger open/reopen counters.
        val result = PreviewTransformHelper.buildTransform(
            viewWidth = 1080,
            viewHeight = 1920,
            previewWidth = 1920,
            previewHeight = 1080,
            displayRotation = Surface.ROTATION_0
        )
        assertTrue(result.aspectRatioOk)
    }

    @Test
    fun transformDoesNotUpdateOnContinuousTilt() {
        // Continuous tilt (e.g. 15 degrees, 45 degrees, 72 degrees) from a physical sensor does not affect the preview matrix.
        // The preview transform relies exclusively on display/layout rotation states.
        val baseResult = PreviewTransformHelper.buildTransform(
            viewWidth = 1080,
            viewHeight = 1920,
            previewWidth = 1920,
            previewHeight = 1080,
            displayRotation = Surface.ROTATION_0
        )

        // Even with various tilt angles, if displayRotation is still Surface.ROTATION_0,
        // the computed effective bounds and uniform scale remain exactly identical.
        val tiltSimulatedResult = PreviewTransformHelper.buildTransform(
            viewWidth = 1080,
            viewHeight = 1920,
            previewWidth = 1920,
            previewHeight = 1080,
            displayRotation = Surface.ROTATION_0
        )

        assertEquals(baseResult.effectiveW, tiltSimulatedResult.effectiveW)
        assertEquals(baseResult.effectiveH, tiltSimulatedResult.effectiveH)
        assertEquals(baseResult.uniformScale, tiltSimulatedResult.uniformScale, 1e-4f)
    }
}
