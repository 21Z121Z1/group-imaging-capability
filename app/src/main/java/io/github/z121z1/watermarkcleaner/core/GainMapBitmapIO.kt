package io.github.z121z1.watermarkcleaner.core

import android.graphics.Bitmap
import android.graphics.Gainmap
import android.os.Build
import java.nio.ByteBuffer

data class GainMapChannels(
    val width: Int,
    val height: Int,
    val layout: GainMapLayout,
    val r: IntArray,
    val g: IntArray,
    val b: IntArray,
)

object GainMapBitmapIO {
    fun read(bitmap: Bitmap): GainMapChannels {
        val count = bitmap.width * bitmap.height
        return if (bitmap.config == Bitmap.Config.ALPHA_8) {
            val bytes = ByteArray(bitmap.byteCount)
            bitmap.copyPixelsToBuffer(ByteBuffer.wrap(bytes))
            val values = IntArray(count)
            for (y in 0 until bitmap.height) {
                val rowOffset = y * bitmap.rowBytes
                val dstOffset = y * bitmap.width
                for (x in 0 until bitmap.width) {
                    values[dstOffset + x] = bytes[rowOffset + x].toInt() and 0xff
                }
            }
            GainMapChannels(bitmap.width, bitmap.height, GainMapLayout.MONO, values, values.copyOf(), values.copyOf())
        } else {
            val colors = IntArray(count)
            bitmap.getPixels(colors, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val r = IntArray(count)
            val g = IntArray(count)
            val b = IntArray(count)
            for (i in colors.indices) {
                val color = colors[i]
                r[i] = (color ushr 16) and 0xff
                g[i] = (color ushr 8) and 0xff
                b[i] = color and 0xff
            }
            GainMapChannels(bitmap.width, bitmap.height, GainMapLayout.RGB, r, g, b)
        }
    }

    fun write(bitmap: Bitmap, channels: GainMapChannels) {
        require(bitmap.width == channels.width && bitmap.height == channels.height)
        if (bitmap.config == Bitmap.Config.ALPHA_8) {
            require(channels.layout == GainMapLayout.MONO)
            val bytes = ByteArray(bitmap.byteCount)
            bitmap.copyPixelsToBuffer(ByteBuffer.wrap(bytes))
            for (y in 0 until bitmap.height) {
                val rowOffset = y * bitmap.rowBytes
                val srcOffset = y * bitmap.width
                for (x in 0 until bitmap.width) {
                    bytes[rowOffset + x] = channels.r[srcOffset + x].coerceIn(0, 255).toByte()
                }
            }
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
        } else {
            val colors = IntArray(channels.width * channels.height)
            for (i in colors.indices) {
                val r = channels.r[i].coerceIn(0, 255)
                val g = channels.g[i].coerceIn(0, 255)
                val b = channels.b[i].coerceIn(0, 255)
                colors[i] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
            }
            bitmap.setPixels(colors, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }
    }

    fun metadata(gainmap: Gainmap): Triple<Array<GainChannelMetadata>, FloatArray, FloatArray> {
        val ratioMin = gainmap.ratioMin
        val ratioMax = gainmap.ratioMax
        val gamma = gainmap.gamma
        val metadata = Array(3) { channel ->
            GainChannelMetadata(
                ratioMin = ratioMin[channel],
                ratioMax = ratioMax[channel],
                gamma = gamma[channel],
            )
        }
        return Triple(metadata, gainmap.epsilonSdr, gainmap.epsilonHdr)
    }

    fun isSdrToHdr(gainmap: Gainmap): Boolean =
        Build.VERSION.SDK_INT < 36 || gainmap.gainmapDirection == Gainmap.GAINMAP_DIRECTION_SDR_TO_HDR
}
