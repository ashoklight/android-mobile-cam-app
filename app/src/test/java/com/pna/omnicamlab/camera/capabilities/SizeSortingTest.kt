package com.pna.omnicamlab.camera.capabilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SizeSortingTest {

    @Test
    fun testParseSizeString_success() {
        val size = SizeUtils.parseSizeString("4000x3000")
        assertTrue(size != null)
        assertEquals(4000, size?.width)
        assertEquals(3000, size?.height)
        assertEquals("4000x3000", size?.toString())
        assertEquals(12_000_000L, size?.area)
    }

    @Test
    fun testParseSizeString_caseInsensitiveAndWhitespace() {
        val sizeStr = " 1920X1080 "
        val size = SizeUtils.parseSizeString(sizeStr)
        assertTrue(size != null)
        assertEquals(1920, size?.width)
        assertEquals(1080, size?.height)
    }

    @Test
    fun testParseSizeString_invalid() {
        assertNull(SizeUtils.parseSizeString("invalid"))
        assertNull(SizeUtils.parseSizeString("4000"))
        assertNull(SizeUtils.parseSizeString("4000xabc"))
    }

    @Test
    fun testSortSizesDescending() {
        val sizes = listOf(
            CameraSize(1920, 1080),
            CameraSize(4000, 3000),
            CameraSize(1280, 720),
            CameraSize(3264, 2448)
        )

        val sorted = SizeUtils.sortSizesDescending(sizes)
        
        assertEquals(4, sorted.size)
        assertEquals("4000x3000", sorted[0].toString()) // 12MP
        assertEquals("3264x2448", sorted[1].toString()) // 8MP
        assertEquals("1920x1080", sorted[2].toString()) // ~2MP
        assertEquals("1280x720", sorted[3].toString())  // ~0.9MP
    }

    @Test
    fun testGetLargestSize() {
        val sizes = listOf(
            CameraSize(1280, 720),
            CameraSize(3840, 2160),
            CameraSize(1920, 1080)
        )
        val largest = SizeUtils.getLargestSize(sizes)
        assertEquals("3840x2160", largest?.toString())
    }

    @Test
    fun testGetLargestSize_empty() {
        assertNull(SizeUtils.getLargestSize(emptyList()))
    }

    @Test
    fun testFilterByAspectRatio() {
        val sizes = listOf(
            CameraSize(4000, 3000), // 1.333 (4:3)
            CameraSize(1920, 1080), // 1.777 (16:9)
            CameraSize(3264, 2448), // 1.333 (4:3)
            CameraSize(1280, 720)   // 1.777 (16:9)
        )

        // 4:3 is ~1.333
        val fourThirds = SizeUtils.filterByAspectRatio(sizes, 1.333f)
        assertEquals(2, fourThirds.size)
        assertTrue(fourThirds.contains(CameraSize(4000, 3000)))
        assertTrue(fourThirds.contains(CameraSize(3264, 2448)))

        // 16:9 is ~1.777
        val sixteenNinths = SizeUtils.filterByAspectRatio(sizes, 1.777f)
        assertEquals(2, sixteenNinths.size)
        assertTrue(sixteenNinths.contains(CameraSize(1920, 1080)))
        assertTrue(sixteenNinths.contains(CameraSize(1280, 720)))
    }
}
