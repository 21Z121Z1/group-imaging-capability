package io.github.z121z1.watermarkcleaner.core

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

enum class GainMapLayout(val wireValue: Int) {
    MONO(0),
    RGB(1),
    ;

    companion object {
        fun fromWireValue(value: Int): GainMapLayout = entries.firstOrNull { it.wireValue == value }
            ?: throw IllegalArgumentException("unsupported gain-map layout: $value")
    }
}

data class GainMapPixel(
    val index: Int,
    val slopeR: Float,
    val slopeG: Float,
    val slopeB: Float,
    val interceptR: Float,
    val interceptG: Float,
    val interceptB: Float,
    val validationError: Float,
)

data class GainMapProfile(
    val baseWidth: Int,
    val baseHeight: Int,
    val width: Int,
    val height: Int,
    val layout: GainMapLayout,
    val pixels: List<GainMapPixel>,
    val calibrationLevels: Int,
    val calibrationRmse: Float,
    val validationMaxError: Float,
) {
    init {
        require(baseWidth > 0 && baseHeight > 0 && width > 0 && height > 0)
        require(pixels.size <= width * height)
        require(calibrationLevels in 2..256)
        require(calibrationRmse.isFinite() && calibrationRmse >= 0f)
        require(validationMaxError.isFinite() && validationMaxError >= 0f)
    }
}

object GainMapProfileCodec {
    private const val MAGIC = 0x474D5231 // GMR1
    private const val VERSION = 1

    fun write(profile: GainMapProfile, output: OutputStream) {
        DataOutputStream(output.buffered()).use { out ->
            out.writeInt(MAGIC)
            out.writeInt(VERSION)
            out.writeInt(profile.baseWidth)
            out.writeInt(profile.baseHeight)
            out.writeInt(profile.width)
            out.writeInt(profile.height)
            out.writeByte(profile.layout.wireValue)
            out.writeInt(profile.pixels.size)
            out.writeInt(profile.calibrationLevels)
            out.writeFloat(profile.calibrationRmse)
            out.writeFloat(profile.validationMaxError)
            var previous = -1
            profile.pixels.forEach { pixel ->
                require(pixel.index in 0 until profile.width * profile.height)
                require(pixel.index > previous) { "gain-map profile indices must be strictly increasing" }
                validate(pixel)
                out.writeInt(pixel.index)
                out.writeFloat(pixel.slopeR)
                out.writeFloat(pixel.slopeG)
                out.writeFloat(pixel.slopeB)
                out.writeFloat(pixel.interceptR)
                out.writeFloat(pixel.interceptG)
                out.writeFloat(pixel.interceptB)
                out.writeFloat(pixel.validationError)
                previous = pixel.index
            }
        }
    }

    fun read(input: InputStream): GainMapProfile {
        DataInputStream(input.buffered()).use { data ->
            require(data.readInt() == MAGIC) { "not a GMR1 gain-map profile; HDR recalibration is required" }
            require(data.readInt() == VERSION) { "unsupported gain-map profile version" }
            val baseWidth = data.readInt()
            val baseHeight = data.readInt()
            val width = data.readInt()
            val height = data.readInt()
            val layout = GainMapLayout.fromWireValue(data.readUnsignedByte())
            val count = data.readInt()
            val calibrationLevels = data.readInt()
            val calibrationRmse = data.readFloat()
            val validationMaxError = data.readFloat()
            require(baseWidth in 1..16384 && baseHeight in 1..16384)
            require(width in 1..16384 && height in 1..16384)
            require(count >= 0 && count.toLong() <= width.toLong() * height.toLong())
            require(calibrationLevels in 2..256)
            require(calibrationRmse.isFinite() && calibrationRmse >= 0f)
            require(validationMaxError.isFinite() && validationMaxError >= 0f)
            val pixels = ArrayList<GainMapPixel>(count)
            var previous = -1
            repeat(count) {
                val pixel = GainMapPixel(
                    index = data.readInt(),
                    slopeR = data.readFloat(),
                    slopeG = data.readFloat(),
                    slopeB = data.readFloat(),
                    interceptR = data.readFloat(),
                    interceptG = data.readFloat(),
                    interceptB = data.readFloat(),
                    validationError = data.readFloat(),
                )
                require(pixel.index in 0 until width * height)
                require(pixel.index > previous) { "gain-map profile indices are not strictly increasing" }
                validate(pixel)
                pixels += pixel
                previous = pixel.index
            }
            require(data.read() == -1) { "trailing bytes in gain-map profile" }
            return GainMapProfile(
                baseWidth = baseWidth,
                baseHeight = baseHeight,
                width = width,
                height = height,
                layout = layout,
                pixels = pixels,
                calibrationLevels = calibrationLevels,
                calibrationRmse = calibrationRmse,
                validationMaxError = validationMaxError,
            )
        }
    }

    fun writeAtomically(profile: GainMapProfile, target: File) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, ".${target.name}.tmp-${System.nanoTime()}")
        try {
            tmp.outputStream().use { write(profile, it) }
            tmp.inputStream().use { read(it) }
            try {
                Files.move(
                    tmp.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: Exception) {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    private fun validate(pixel: GainMapPixel) {
        val slopes = floatArrayOf(pixel.slopeR, pixel.slopeG, pixel.slopeB)
        val intercepts = floatArrayOf(pixel.interceptR, pixel.interceptG, pixel.interceptB)
        require(slopes.all { it.isFinite() && it in 0.01f..2.5f })
        require(intercepts.all { it.isFinite() && it in -32f..32f })
        require(pixel.validationError.isFinite() && pixel.validationError in 0f..255f)
    }
}
