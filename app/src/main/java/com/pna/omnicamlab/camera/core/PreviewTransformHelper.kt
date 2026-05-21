package com.pna.omnicamlab.camera.core

import android.graphics.Matrix
import android.view.Surface

enum class PreviewScaleType {
    CENTER_CROP,
    FIT_CENTER
}

data class TransformResult(
    val matrix: Matrix,
    val scaleX: Float,
    val scaleY: Float,
    val uniformScale: Float,
    val effectiveW: Float,
    val effectiveH: Float,
    val scaledW: Float,
    val scaledH: Float,
    val dx: Float,
    val dy: Float,
    val aspectRatioOk: Boolean
)

object PreviewTransformHelper {

    /**
     * Computes the display-rotation-based preview rotation applied in auto mode.
     */
    fun getAutoPreviewRotationDegrees(displayRotation: Int): Int {
        return when (displayRotation) {
            Surface.ROTATION_90 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 90
            else -> 0
        }
    }

    /**
     * Builds a TransformResult containing the computed transform matrix and detailed 
     * scaling/cropping telemetry using pure uniform scaling logic.
     */
    fun buildTransform(
        viewWidth: Int,
        viewHeight: Int,
        previewWidth: Int,
        previewHeight: Int,
        displayRotation: Int,
        forcedRotationDegrees: Int? = null,
        mirrorForFrontCamera: Boolean = false,
        scaleType: PreviewScaleType = PreviewScaleType.CENTER_CROP
    ): TransformResult {
        val matrix = Matrix()
        if (viewWidth <= 0 || viewHeight <= 0 || previewWidth <= 0 || previewHeight <= 0) {
            return TransformResult(
                matrix = matrix,
                scaleX = 1f,
                scaleY = 1f,
                uniformScale = 1f,
                effectiveW = previewWidth.toFloat(),
                effectiveH = previewHeight.toFloat(),
                scaledW = previewWidth.toFloat(),
                scaledH = previewHeight.toFloat(),
                dx = 0f,
                dy = 0f,
                aspectRatioOk = true
            )
        }

        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f

        val activeRotationDegrees = forcedRotationDegrees ?: getAutoPreviewRotationDegrees(displayRotation)

        // 1. Calculate effective buffer dimensions swapping for 90 or 270 degrees
        val (effectiveW, effectiveH) = if (activeRotationDegrees == 90 || activeRotationDegrees == 270) {
            previewHeight.toFloat() to previewWidth.toFloat()
        } else {
            previewWidth.toFloat() to previewHeight.toFloat()
        }

        // 2. Compute uniform scale based on scale type
        val scale = when (scaleType) {
            PreviewScaleType.CENTER_CROP -> maxOf(viewWidth.toFloat() / effectiveW, viewHeight.toFloat() / effectiveH)
            PreviewScaleType.FIT_CENTER -> minOf(viewWidth.toFloat() / effectiveW, viewHeight.toFloat() / effectiveH)
        }

        // 3. Compute scale X and scale Y to undo natural TextureView stretch and apply the uniform scale
        val scaleX = (previewWidth.toFloat() * scale) / viewWidth.toFloat()
        val scaleY = (previewHeight.toFloat() * scale) / viewHeight.toFloat()

        // 4. Calculate scaled dimensions and centering offsets
        val scaledW = effectiveW * scale
        val scaledH = effectiveH * scale
        val dx = (viewWidth.toFloat() - scaledW) / 2f
        val dy = (viewHeight.toFloat() - scaledH) / 2f

        // Verify aspect ratio preservation
        // scaleX relative to natural stretch must exactly match scaleY relative to natural stretch
        val actualScaleX = scaleX * viewWidth.toFloat() / previewWidth.toFloat()
        val actualScaleY = scaleY * viewHeight.toFloat() / previewHeight.toFloat()
        val aspectRatioOk = Math.abs(actualScaleX - actualScaleY) < 1e-4f

        // 5. Apply transformations to Matrix
        // Scale to center
        matrix.setScale(scaleX, scaleY, centerX, centerY)

        // Rotate around center
        if (activeRotationDegrees != 0) {
            matrix.postRotate(activeRotationDegrees.toFloat(), centerX, centerY)
        }

        // Front camera mirror around center
        if (mirrorForFrontCamera) {
            matrix.postScale(-1f, 1f, centerX, centerY)
        }

        return TransformResult(
            matrix = matrix,
            scaleX = actualScaleX,
            scaleY = actualScaleY,
            uniformScale = scale,
            effectiveW = effectiveW,
            effectiveH = effectiveH,
            scaledW = scaledW,
            scaledH = scaledH,
            dx = dx,
            dy = dy,
            aspectRatioOk = aspectRatioOk
        )
    }

    /**
     * Backward-compatible wrapper that calls buildTransform and returns only the Matrix.
     */
    fun buildCamera2BasicTransform(
        viewWidth: Int,
        viewHeight: Int,
        previewWidth: Int,
        previewHeight: Int,
        displayRotation: Int,
        forcedRotationDegrees: Int? = null,
        mirrorForFrontCamera: Boolean = false
    ): Matrix {
        return buildTransform(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            displayRotation = displayRotation,
            forcedRotationDegrees = forcedRotationDegrees,
            mirrorForFrontCamera = mirrorForFrontCamera,
            scaleType = PreviewScaleType.CENTER_CROP
        ).matrix
    }
}
