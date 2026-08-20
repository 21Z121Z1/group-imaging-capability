package dev.groupimaging.unmark.model

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

object WatermarkProfileCodec {
    private val MAGIC = byteArrayOf('W'.code.toByte(), 'M'.code.toByte(), 'R'.code.toByte(), '2'.code.toByte())
    private const val VERSION = 2
    private const val HEADER_BYTES = 20
    private const val RECORD_BYTES = 28
    private const val CRC_BYTES = 4
    private const val MAX_RECORDS = 2_000_000
    private const val MAX_FILE_BYTES = HEADER_BYTES + RECORD_BYTES * MAX_RECORDS + CRC_BYTES

    fun encode(profile: WatermarkProfile): ByteArray {
        require(profile.size <= MAX_RECORDS) { "Profile is too large" }
        val output = ByteArrayOutputStream(HEADER_BYTES + profile.size * RECORD_BYTES + CRC_BYTES)
        write(profile, output)
        return output.toByteArray()
    }

    fun write(profile: WatermarkProfile, output: OutputStream) {
        require(profile.size <= MAX_RECORDS) { "Profile is too large" }

        val body = ByteArrayOutputStream(HEADER_BYTES + profile.size * RECORD_BYTES)
        DataOutputStream(body).use { data ->
            data.write(MAGIC)
            data.writeInt(VERSION)
            data.writeInt(profile.width)
            data.writeInt(profile.height)
            data.writeInt(profile.size)
            for (i in profile.indices.indices) {
                data.writeInt(profile.indices[i])
                data.writeFloat(profile.slopeR[i])
                data.writeFloat(profile.slopeG[i])
                data.writeFloat(profile.slopeB[i])
                data.writeFloat(profile.interceptR[i])
                data.writeFloat(profile.interceptG[i])
                data.writeFloat(profile.interceptB[i])
            }
        }

        val bodyBytes = body.toByteArray()
        val crc = CRC32().apply { update(bodyBytes) }.value.toInt()
        DataOutputStream(output).use { data ->
            data.write(bodyBytes)
            data.writeInt(crc)
            data.flush()
        }
    }

    fun decode(input: InputStream): WatermarkProfile = decode(readLimited(input))

    fun decode(bytes: ByteArray): WatermarkProfile {
        require(bytes.size >= HEADER_BYTES + CRC_BYTES) { "Truncated profile" }
        require(bytes.size <= MAX_FILE_BYTES) { "Profile file is too large" }

        val storedCrc = ByteBuffer.wrap(bytes, bytes.size - CRC_BYTES, CRC_BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .int
        val crc = CRC32().apply { update(bytes, 0, bytes.size - CRC_BYTES) }.value.toInt()
        require(storedCrc == crc) { "Profile checksum mismatch" }

        val buffer = ByteBuffer.wrap(bytes, 0, bytes.size - CRC_BYTES).order(ByteOrder.BIG_ENDIAN)
        val magic = ByteArray(MAGIC.size).also(buffer::get)
        require(magic.contentEquals(MAGIC)) { "Unsupported profile magic" }
        require(buffer.int == VERSION) { "Unsupported profile version" }

        val width = buffer.int
        val height = buffer.int
        val count = buffer.int
        require(width in 1..20_000 && height in 1..20_000) { "Invalid profile dimensions" }
        require(count in 0..MAX_RECORDS) { "Invalid profile record count" }
        require(count.toLong() <= width.toLong() * height.toLong()) { "Profile contains too many pixels" }

        val expected = HEADER_BYTES.toLong() + count.toLong() * RECORD_BYTES + CRC_BYTES
        require(expected == bytes.size.toLong()) { "Profile length does not match header" }

        val indices = IntArray(count)
        val slopeR = FloatArray(count)
        val slopeG = FloatArray(count)
        val slopeB = FloatArray(count)
        val interceptR = FloatArray(count)
        val interceptG = FloatArray(count)
        val interceptB = FloatArray(count)

        for (i in 0 until count) {
            indices[i] = buffer.int
            slopeR[i] = buffer.float
            slopeG[i] = buffer.float
            slopeB[i] = buffer.float
            interceptR[i] = buffer.float
            interceptG[i] = buffer.float
            interceptB[i] = buffer.float
        }

        return WatermarkProfile(
            width = width,
            height = height,
            indices = indices,
            slopeR = slopeR,
            slopeG = slopeG,
            slopeB = slopeB,
            interceptR = interceptR,
            interceptG = interceptG,
            interceptB = interceptB,
        )
    }

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_FILE_BYTES) { "Profile file is too large" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
