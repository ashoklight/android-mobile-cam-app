package com.pna.omnicamlab.camera.core

import com.pna.omnicamlab.camera.capabilities.CameraSize
import org.junit.Assert.assertEquals
import org.junit.Test

class JpegSizeSelectorTest {

    @Test
    fun selectBestJpegSize_emptyList_returnsFallback() {
        val result = JpegSizeSelector.selectBestJpegSize(emptyList())
        assertEquals(CameraSize(1920, 1080), result)
    }

    @Test
    fun selectBestJpegSize_sizesWithinSafeLimit_returnsLargest() {
        val sizes = listOf(
            CameraSize(1920, 1080), // 2.07 MP
            CameraSize(4000, 3000), // 12.00 MP
            CameraSize(3264, 2448)  // 7.99 MP
        )
        val result = JpegSizeSelector.selectBestJpegSize(sizes)
        assertEquals(CameraSize(4000, 3000), result)
    }

    @Test
    fun selectBestJpegSize_someSizesExceedLimit_picksLargestSafe() {
        val sizes = listOf(
            CameraSize(1920, 1080), // 2.07 MP
            CameraSize(4000, 3000), // 12.00 MP
            CameraSize(6000, 4000), // 24.00 MP (exceeds 16MP)
            CameraSize(3264, 2448)  // 7.99 MP
        )
        val result = JpegSizeSelector.selectBestJpegSize(sizes)
        assertEquals(CameraSize(4000, 3000), result)
    }

    @Test
    fun selectBestJpegSize_allSizesExceedLimit_picksSmallestExceeding() {
        val sizes = listOf(
            CameraSize(6000, 4000), // 24.00 MP
            CameraSize(8000, 6000), // 48.00 MP
            CameraSize(7000, 5000)  // 35.00 MP
        )
        val result = JpegSizeSelector.selectBestJpegSize(sizes)
        assertEquals(CameraSize(6000, 4000), result)
    }
}
