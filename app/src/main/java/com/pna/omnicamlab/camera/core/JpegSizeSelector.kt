package com.pna.omnicamlab.camera.core

import com.pna.omnicamlab.camera.capabilities.CameraSize
import com.pna.omnicamlab.camera.capabilities.SizeUtils

object JpegSizeSelector {
    private const val MAX_SAFE_PIXELS = 16_000_000L // 16 Megapixels

    /**
     * Selects the best JPEG size from the list of available sizes.
     * Rules:
     * 1. Filters sizes that are less than or equal to 16 Megapixels.
     * 2. If there are sizes within the safe range, choose the largest.
     * 3. If all sizes are larger than 16MP, choose the smallest of those larger sizes
     *    to minimize memory stress while avoiding failing.
     * 4. If the list is empty, returns a default fallback.
     */
    fun selectBestJpegSize(sizes: List<CameraSize>): CameraSize {
        if (sizes.isEmpty()) {
            return CameraSize(1920, 1080) // Default fallback
        }

        val safeSizes = sizes.filter { it.area <= MAX_SAFE_PIXELS }
        return if (safeSizes.isNotEmpty()) {
            // Pick largest within safe limit
            safeSizes.maxOrNull() ?: sizes.first()
        } else {
            // Pick the smallest among the extra-large sizes to prevent OOM
            sizes.minOrNull() ?: sizes.first()
        }
    }
}
