package io.github.z121z1.watermarkcleaner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class GainMapProfileCodecTest {
    private val profile = GainMapProfile(
        baseWidth = 1440,
        baseHeight = 3168,
        width = 360,
        height = 792,
        layout = GainMapLayout.MONO,
        pixels = listOf(
            GainMapPixel(10, .91f, .91f, .91f, -.02f, -.02f, -.02f, .004f),
            GainMapPixel(100, .87f, .87f, .87f, -.04f, -.04f, -.04f, .007f),
        ),
        calibrationLevels = 6,
        calibrationRmse = .005f,
        validationMaxError = .007f,
    )

    @Test
    fun roundTrip_preservesProfile() {
        val bytes = ByteArrayOutputStream().also { GainMapProfileCodec.write(profile, it) }.toByteArray()
        assertEquals(profile, GainMapProfileCodec.read(ByteArrayInputStream(bytes)))
    }

    @Test
    fun readRejectsTrailingBytes() {
        val bytes = ByteArrayOutputStream().also { GainMapProfileCodec.write(profile, it) }.toByteArray()
        assertThrows(IllegalArgumentException::class.java) {
            GainMapProfileCodec.read(ByteArrayInputStream(bytes + byteArrayOf(1)))
        }
    }

    @Test
    fun writeRejectsUnsortedIndices() {
        assertThrows(IllegalArgumentException::class.java) {
            GainMapProfileCodec.write(profile.copy(pixels = profile.pixels.reversed()), ByteArrayOutputStream())
        }
    }
}
