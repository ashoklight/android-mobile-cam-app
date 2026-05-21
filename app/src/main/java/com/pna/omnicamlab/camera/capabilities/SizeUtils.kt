package com.pna.omnicamlab.camera.capabilities

import java.util.Locale

data class CameraSize(val width: Int, val height: Int) : Comparable<CameraSize> {
    val area: Long get() = width.toLong() * height.toLong()
    val aspect: Float get() = if (height > 0) width.toFloat() / height.toFloat() else 0f

    override fun toString(): String = "${width}x${height}"

    override fun compareTo(other: CameraSize): Int {
        // Compare by area, then by width if areas are identical
        val areaDiff = this.area - other.area
        return if (areaDiff != 0L) {
            if (areaDiff > 0) 1 else -1
        } else {
            this.width - other.width
        }
    }
}

object SizeUtils {
    /**
     * Parses a string formatted as "WIDTHxHEIGHT" into a CameraSize object.
     */
    fun parseSizeString(sizeStr: String): CameraSize? {
        return try {
            val parts = sizeStr.lowercase(Locale.ROOT).split("x")
            if (parts.size == 2) {
                val w = parts[0].trim().toInt()
                val h = parts[1].trim().toInt()
                CameraSize(w, h)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sorts list of CameraSizes descending (largest area first).
     */
    fun sortSizesDescending(sizes: List<CameraSize>): List<CameraSize> {
        return sizes.sortedWith(compareByDescending<CameraSize> { it.area }.thenByDescending { it.width })
    }

    /**
     * Retrieves the largest size from the list.
     */
    fun getLargestSize(sizes: List<CameraSize>): CameraSize? {
        if (sizes.isEmpty()) return null
        return sizes.maxOrNull()
    }

    /**
     * Filters sizes that match a given target aspect ratio within a specific tolerance range.
     */
    fun filterByAspectRatio(
        sizes: List<CameraSize>,
        targetRatio: Float,
        tolerance: Float = 0.01f
    ): List<CameraSize> {
        return sizes.filter { Math.abs(it.aspect - targetRatio) <= tolerance }
    }
}
