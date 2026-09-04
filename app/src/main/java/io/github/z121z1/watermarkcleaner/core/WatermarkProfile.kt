package io.github.z121z1.watermarkcleaner.core

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class CompositePixel(
    val index: Int,
    val blendSpace: BlendSpace,
    val slopeR: Float,
    val slopeG: Float,
    val slopeB: Float,
    val interceptR: Float,
    val interceptG: Float,
    val interceptB: Float,
    val validationError: Float,
)

data class WatermarkProfile(
    val width: Int,
    val height: Int,
    val pixels: List<CompositePixel>,
    val calibrationLevels: Int = 0,
    val calibrationRmse: Float = 0f,
    val validationMaxError: Float = 0f,
) {
    init {
        require(width > 0 && height > 0)
        require(pixels.size <= width * height)
        require(calibrationLevels in 0..256)
        require(calibrationRmse.isFinite() && calibrationRmse >= 0f)
        require(validationMaxError.isFinite() && validationMaxError >= 0f)
    }
}

object WatermarkProfileCodec {
    private const val MAGIC = 0x574D5233 // WMR3
    private const val VERSION = 3

    fun write(profile: WatermarkProfile, output: OutputStream) {
        DataOutputStream(output.buffered()).use { out ->
            out.writeInt(MAGIC)
            out.writeInt(VERSION)
            out.writeInt(profile.width)
            out.writeInt(profile.height)
            out.writeInt(profile.pixels.size)
            out.writeInt(profile.calibrationLevels)
            out.writeFloat(profile.calibrationRmse)
            out.writeFloat(profile.validationMaxError)
            var previous = -1
            profile.pixels.forEach { pixel ->
                require(pixel.index in 0 until profile.width * profile.height)
                require(pixel.index > previous) { "profile indices must be strictly increasing" }
                validate(pixel)
                out.writeInt(pixel.index)
                out.writeByte(pixel.blendSpace.wireValue)
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

    fun read(input: InputStream): WatermarkProfile {
        DataInputStream(input.buffered()).use { data ->
            require(data.readInt() == MAGIC) { "not a WMR3 profile; recalibration is required" }
            require(data.readInt() == VERSION) { "unsupported profile version; recalibration is required" }
            val width = data.readInt()
            val height = data.readInt()
            val count = data.readInt()
            val calibrationLevels = data.readInt()
            val calibrationRmse = data.readFloat()
            val validationMaxError = data.readFloat()
            require(width in 1..16384 && height in 1..16384)
            val total = width.toLong() * height.toLong()
            require(count >= 0 && count.toLong() <= total)
            require(calibrationLevels in 0..256)
            require(calibrationRmse.isFinite() && calibrationRmse >= 0f)
            require(validationMaxError.isFinite() && validationMaxError >= 0f)
            val pixels = ArrayList<CompositePixel>(count)
            var previous = -1
            repeat(count) {
                val pixel = CompositePixel(
                    index = data.readInt(),
                    blendSpace = BlendSpace.fromWireValue(data.readUnsignedByte()),
                    slopeR = data.readFloat(),
                    slopeG = data.readFloat(),
                    slopeB = data.readFloat(),
                    interceptR = data.readFloat(),
                    interceptG = data.readFloat(),
                    interceptB = data.readFloat(),
                    validationError = data.readFloat(),
                )
                require(pixel.index in 0 until width * height)
                require(pixel.index > previous) { "profile indices are not strictly increasing" }
                validate(pixel)
                pixels += pixel
                previous = pixel.index
            }
            require(data.read() == -1) { "trailing bytes in profile" }
            return WatermarkProfile(
                width = width,
                height = height,
                pixels = pixels,
                calibrationLevels = calibrationLevels,
                calibrationRmse = calibrationRmse,
                validationMaxError = validationMaxError,
            )
        }
    }

    fun writeAtomically(profile: WatermarkProfile, target: File) {
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

    private fun validate(pixel: CompositePixel) {
        val slopes = floatArrayOf(pixel.slopeR, pixel.slopeG, pixel.slopeB)
        val intercepts = floatArrayOf(pixel.interceptR, pixel.interceptG, pixel.interceptB)
        require(slopes.all { it.isFinite() && it in 0.01f..1.05f })
        require(intercepts.all { it.isFinite() && it in -0.05f..1.05f })
        require(pixel.validationError.isFinite() && pixel.validationError in 0f..255f)
    }
}
