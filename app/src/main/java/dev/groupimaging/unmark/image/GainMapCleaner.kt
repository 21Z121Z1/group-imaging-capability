package dev.groupimaging.unmark.image

import android.graphics.Bitmap
import dev.groupimaging.unmark.model.WatermarkProfile
import java.util.BitSet

/**
 * Deterministic, non-generative fallback for Ultra HDR enhancement layers.
 *
 * ColorOS screenshot samples show that the visible screenshot watermark can be baked into the gain
 * map as well as the SDR primary. When no dedicated gain-map calibration exists, map the calibrated
 * primary watermark support into gain-map coordinates and replace only those samples with the
 * per-channel median of a nearby non-watermark ring. All gain-map metadata is preserved by the
 * caller; this object only edits the enhancement bitmap.
 *
 * Memory is intentionally sparse: one source pixel buffer, a BitSet mask, and replacement arrays
 * sized to the watermark support. We do not allocate a second full-size gain-map buffer.
 */
object GainMapCleaner {
    private const val MAX_RADIUS = 6
    private const val MIN_SAMPLES = 8

    fun clean(gainContents: Bitmap, primaryProfile: WatermarkProfile) {
        require(gainContents.isMutable) { "Gain-map bitmap must be mutable" }
        if (primaryProfile.size == 0 || gainContents.width <= 0 || gainContents.height <= 0) return

        val width = gainContents.width
        val height = gainContents.height
        val pixelCountLong = width.toLong() * height.toLong()
        require(pixelCountLong <= Int.MAX_VALUE) { "Gain map is too large" }
        val pixelCount = pixelCountLong.toInt()
        val mask = BitSet(pixelCount)

        for (record in primaryProfile.indices.indices) {
            val primaryIndex = primaryProfile.indices[record]
            val px = primaryIndex % primaryProfile.width
            val py = primaryIndex / primaryProfile.width
            val gx = ((px.toLong() * width) / primaryProfile.width).toInt().coerceIn(0, width - 1)
            val gy = ((py.toLong() * height) / primaryProfile.height).toInt().coerceIn(0, height - 1)
            // Small dilation absorbs resampling/antialiasing differences between base and gain map.
            for (dy in -1..1) {
                val y = gy + dy
                if (y !in 0 until height) continue
                for (dx in -1..1) {
                    val x = gx + dx
                    if (x in 0 until width) mask.set(y * width + x)
                }
            }
        }

        val affectedCount = mask.cardinality()
        if (affectedCount == 0) return

        val pixels = IntArray(pixelCount)
        gainContents.getPixels(pixels, 0, width, 0, 0, width, height)
        val replacementIndices = IntArray(affectedCount)
        val replacementValues = IntArray(affectedCount)
        val rs = IntArray(256)
        val gs = IntArray(256)
        val bs = IntArray(256)
        var replacementCount = 0

        var index = mask.nextSetBit(0)
        while (index >= 0) {
            val x = index % width
            val y = index / width
            var count = 0
            var radius = 2
            while (radius <= MAX_RADIUS && count < MIN_SAMPLES) {
                val left = x - radius
                val right = x + radius
                val top = y - radius
                val bottom = y + radius

                fun sample(sx: Int, sy: Int) {
                    if (sx !in 0 until width || sy !in 0 until height || count >= rs.size) return
                    val sourceIndex = sy * width + sx
                    if (mask[sourceIndex]) return
                    val color = pixels[sourceIndex]
                    rs[count] = color ushr 16 and 0xff
                    gs[count] = color ushr 8 and 0xff
                    bs[count] = color and 0xff
                    count++
                }

                for (sx in left..right) {
                    sample(sx, top)
                    if (bottom != top) sample(sx, bottom)
                }
                for (sy in (top + 1) until bottom) {
                    sample(left, sy)
                    if (right != left) sample(right, sy)
                }
                radius++
            }

            if (count >= 4) {
                val old = pixels[index]
                val alpha = old ushr 24 and 0xff
                val red = median(rs, count)
                val green = median(gs, count)
                val blue = median(bs, count)
                replacementIndices[replacementCount] = index
                replacementValues[replacementCount] =
                    (alpha shl 24) or (red shl 16) or (green shl 8) or blue
                replacementCount++
            }
            if (index == Int.MAX_VALUE) break
            index = mask.nextSetBit(index + 1)
        }

        for (i in 0 until replacementCount) {
            pixels[replacementIndices[i]] = replacementValues[i]
        }
        gainContents.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun median(values: IntArray, count: Int): Int {
        val copy = values.copyOf(count)
        copy.sort()
        return if (count and 1 == 1) {
            copy[count / 2]
        } else {
            (copy[count / 2 - 1] + copy[count / 2]) / 2
        }
    }
}
