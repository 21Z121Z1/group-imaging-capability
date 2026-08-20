package dev.groupimaging.unmark.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WatermarkProfileCodecTest {
    private fun profile(): WatermarkProfile = WatermarkProfile(
        width = 4,
        height = 3,
        indices = intArrayOf(1, 5, 10),
        slopeR = floatArrayOf(0.94f, 0.95f, 0.96f),
        slopeG = floatArrayOf(0.93f, 0.94f, 0.95f),
        slopeB = floatArrayOf(0.92f, 0.93f, 0.94f),
        interceptR = floatArrayOf(10f, 8f, 6f),
        interceptG = floatArrayOf(10f, 8f, 6f),
        interceptB = floatArrayOf(10f, 8f, 6f),
    )

    @Test
    fun roundTripPreservesProfile() {
        val original = profile()
        val decoded = WatermarkProfileCodec.decode(WatermarkProfileCodec.encode(original))

        assertEquals(original.width, decoded.width)
        assertEquals(original.height, decoded.height)
        assertArrayEquals(original.indices, decoded.indices)
        assertArrayEquals(original.slopeR, decoded.slopeR, 0f)
        assertArrayEquals(original.interceptB, decoded.interceptB, 0f)
    }

    @Test
    fun checksumRejectsSingleByteCorruption() {
        val bytes = WatermarkProfileCodec.encode(profile())
        bytes[24] = (bytes[24].toInt() xor 0x20).toByte()
        assertThrows(IllegalArgumentException::class.java) {
            WatermarkProfileCodec.decode(bytes)
        }
    }

    @Test
    fun truncatedProfileIsRejected() {
        val bytes = WatermarkProfileCodec.encode(profile()).copyOf(15)
        assertThrows(IllegalArgumentException::class.java) {
            WatermarkProfileCodec.decode(bytes)
        }
    }

    @Test
    fun constructorRejectsNonMonotonicIndices() {
        assertThrows(IllegalArgumentException::class.java) {
            profile().copy(indices = intArrayOf(1, 1, 10))
        }
    }

    @Test
    fun constructorRejectsNaN() {
        assertThrows(IllegalArgumentException::class.java) {
            profile().copy(slopeR = floatArrayOf(Float.NaN, 0.95f, 0.96f))
        }
    }
}
