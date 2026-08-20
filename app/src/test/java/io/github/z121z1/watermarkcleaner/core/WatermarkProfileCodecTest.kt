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
        4,
        3,
        listOf(
            AffinePixel(1, .94f, .95f, .96f, 10f, 9f, 8f),
            AffinePixel(7, .90f, .91f, .92f, 14f, 13f, 12f),
        ),
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
        val dir = createTempDirectory(prefix = "wmr2-").toFile()
        try {
            val file = File(dir, "primary.wmr2")
            WatermarkProfileCodec.writeAtomically(profile, file)
            assertEquals(profile, file.inputStream().use(WatermarkProfileCodec::read))
            assertEquals(emptyList<String>(), dir.listFiles()!!.filter { it.name.startsWith(".primary") }.map { it.name })
        } finally {
            dir.deleteRecursively()
        }
    }
}
