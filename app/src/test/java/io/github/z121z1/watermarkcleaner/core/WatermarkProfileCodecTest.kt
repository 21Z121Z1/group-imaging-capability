package io.github.z121z1.watermarkcleaner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.io.path.createTempDirectory

class WatermarkProfileCodecTest {
    private val profile = WatermarkProfile(
        width = 4,
        height = 3,
        pixels = listOf(
            CompositePixel(1, BlendSpace.ENCODED_SRGB, .94f, .95f, .96f, .02f, .018f, .016f, .40f),
            CompositePixel(7, BlendSpace.LINEAR_SRGB, .90f, .91f, .92f, .03f, .028f, .026f, .60f),
        ),
        calibrationLevels = 6,
        calibrationRmse = .24f,
        validationMaxError = .60f,
    )

    @Test
    fun roundTrip_preservesProfile() {
        val bytes = ByteArrayOutputStream().also { WatermarkProfileCodec.write(profile, it) }.toByteArray()
        assertEquals(profile, WatermarkProfileCodec.read(ByteArrayInputStream(bytes)))
    }

    @Test
    fun read_rejectsTruncation() {
        val bytes = ByteArrayOutputStream().also { WatermarkProfileCodec.write(profile, it) }.toByteArray()
        assertThrows(Exception::class.java) {
            WatermarkProfileCodec.read(ByteArrayInputStream(bytes.copyOf(bytes.size - 3)))
        }
    }

    @Test
    fun read_rejectsTrailingBytes() {
        val bytes = ByteArrayOutputStream().also { WatermarkProfileCodec.write(profile, it) }.toByteArray()
        assertThrows(IllegalArgumentException::class.java) {
            WatermarkProfileCodec.read(ByteArrayInputStream(bytes + byteArrayOf(1)))
        }
    }

    @Test
    fun write_rejectsUnsortedIndices() {
        val invalid = profile.copy(pixels = profile.pixels.reversed())
        assertThrows(IllegalArgumentException::class.java) {
            WatermarkProfileCodec.write(invalid, ByteArrayOutputStream())
        }
    }

    @Test
    fun atomicWrite_publishesOnlyValidatedProfile() {
        val dir = createTempDirectory("wmr3-").toFile()
        try {
            val file = File(dir, "primary.wmr3")
            WatermarkProfileCodec.writeAtomically(profile, file)
            assertEquals(profile, file.inputStream().use(WatermarkProfileCodec::read))
            assertEquals(emptyList<String>(), dir.listFiles()!!.filter { it.name.startsWith(".primary") }.map { it.name })
        } finally {
            dir.deleteRecursively()
        }
    }
}
