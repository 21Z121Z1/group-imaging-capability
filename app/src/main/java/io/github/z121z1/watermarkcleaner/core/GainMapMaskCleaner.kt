package io.github.z121z1.watermarkcleaner.core

import android.graphics.Bitmap

/**
 * Deterministic fallback for HDR screenshots whose enhancement layer contains the same fixed
 * screenshot watermark but no separately calibrated gain-map profile is available.
 *
 * Only pixels covered by the calibrated primary watermark mask are changed. Each target is replaced
 * by the per-channel median of a small ring of non-mask neighbours. This is intentionally local,
 * non-generative and conservative; a calibrated gain profile is preferred whenever available.
 */
object GainMapMaskCleaner {
    private const val MAX_RADIUS = 6

    fun clean(bitmap: Bitmap, primaryProfile: WatermarkProfile) {
        require(bitmap.isMutable)
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0 || primaryProfile.pixels.isEmpty()) return

        val mask = BooleanArray(width * height)
        primaryProfile.pixels.forEach { model ->
            val px = model.index % primaryProfile.width
            val py = model.index / primaryProfile.width
            val gx = (px.toLong() * width / primaryProfile.width).toInt().coerceIn(0, width - 1)
            val gy = (py.toLong() * height / primaryProfile.height).toInt().coerceIn(0, height - 1)
            // One-pixel dilation absorbs antialiasing/resampling differences between base and gain map.
            for (dy in -1..1) for (dx in -1..1) {
                val x = gx + dx
                val y = gy + dy
                if (x in 0 until width && y in 0 until height) mask[y * width + x] = true
            }
        }

        val source = IntArray(width * height)
        bitmap.getPixels(source, 0, width, 0, 0, width, height)
        val output = source.copyOf()
        val rs = IntArray(8 * MAX_RADIUS * 2 + 8)
        val gs = IntArray(rs.size)
        val bs = IntArray(rs.size)

        for (index in mask.indices) {
            if (!mask[index]) continue
            val x = index % width
            val y = index / width
            var count = 0
            var radius = 2
            while (radius <= MAX_RADIUS && count < 8) {
                val left = x - radius
                val right = x + radius
                val top = y - radius
                val bottom = y + radius
                fun sample(sx: Int, sy: Int) {
                    if (sx !in 0 until width || sy !in 0 until height) return
                    val si = sy * width + sx
                    if (mask[si] || count >= rs.size) return
                    val c = source[si]
                    rs[count] = (c ushr 16) and 0xff
                    gs[count] = (c ushr 8) and 0xff
                    bs[count] = c and 0xff
                    count++
                }
                for (sx in left..right) {
                    sample(sx, top)
                    sample(sx, bottom)
                }
                for (sy in top + 1 until bottom) {
                    sample(left, sy)
                    sample(right, sy)
                }
                radius++
            }
            if (count >= 4) {
                val old = source[index]
                val a = (old ushr 24) and 0xff
                val r = median(rs, count)
                val g = median(gs, count)
                val b = median(bs, count)
                output[index] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bitmap.setPixels(output, 0, width, 0, 0, width, height)
    }

    private fun median(values: IntArray, count: Int): Int {
        val copy = values.copyOf(count)
        copy.sort()
        return if (count and 1 == 1) copy[count / 2] else (copy[count / 2 - 1] + copy[count / 2]) / 2
    }
}
