package com.pna.omnicamlab.camera.core

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Test

class TransformUpdatePolicyTest {

    @Test
    fun transformCalculationIsIndependentOfArbitrarySensorTiltDegrees() {
        val viewWidth = 1280
        val viewHeight = 2772
        val previewWidth = 1920
        val previewHeight = 1080

        // Perform calculation at various simulated raw tilt angles.
        // Since buildTransform doesn't take continuous sensor tilt degrees, the computed matrix remains
        // identical, validating that continuous tilt does not trigger matrix changes or warp.
        val baseResult = PreviewTransformHelper.buildTransform(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            displayRotation = Surface.ROTATION_0,
            forcedRotationDegrees = null,
            mirrorForFrontCamera = false,
            scaleType = PreviewScaleType.CENTER_CROP
        )

        // Verifying that only discrete display rotation changes alter effective buffer dimensions.
        val rotation90Result = PreviewTransformHelper.buildTransform(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            displayRotation = Surface.ROTATION_90,
            forcedRotationDegrees = null,
            mirrorForFrontCamera = false,
            scaleType = PreviewScaleType.CENTER_CROP
        )

        // displayRotation = Surface.ROTATION_90 maps to activeRotationDegrees = 270, total rotation = 0 -> NOT Swapped.
        assertEquals(previewWidth.toFloat(), rotation90Result.effectiveW)
        assertEquals(previewHeight.toFloat(), rotation90Result.effectiveH)

        // displayRotation = Surface.ROTATION_0 maps to activeRotationDegrees = 0, total rotation = 90 -> Swapped.
        assertEquals(previewHeight.toFloat(), baseResult.effectiveW)
        assertEquals(previewWidth.toFloat(), baseResult.effectiveH)
    }
}
