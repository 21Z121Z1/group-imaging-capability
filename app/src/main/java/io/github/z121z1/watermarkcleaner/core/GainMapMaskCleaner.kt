package io.github.z121z1.watermarkcleaner.core

import android.graphics.Bitmap

/**
 * Deterministic fallback used only when no calibrated HDR gain-map profile exists.
 * It understands both ALPHA_8 single-plane and RGB gain maps. Calibrated log-gain inversion is
 * preferred because this local interpolation cannot recover information destroyed by the overlay.
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
            for (dy in -1..1) for (dx in -1..1) {
                val x = gx + dx
                val y = gy + dy
                if (x in 0 until width && y in 0 until height) mask[y * width + x] = true
            }
        }

        val channels = GainMapBitmapIO.read(bitmap)
        val sourceR = channels.r.copyOf()
        val sourceG = channels.g.copyOf()
        val sourceB = channels.b.copyOf()
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
                    rs[count] = sourceR[si]
                    gs[count] = sourceG[si]
                    bs[count] = sourceB[si]
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
                channels.r[index] = median(rs, count)
                if (channels.layout == GainMapLayout.MONO) {
                    channels.g[index] = channels.r[index]
                    channels.b[index] = channels.r[index]
                } else {
                    channels.g[index] = median(gs, count)
                    channels.b[index] = median(bs, count)
                }
            }
        }
        GainMapBitmapIO.write(bitmap, channels)
    }

    private fun median(values: IntArray, count: Int): Int {
        val copy = values.copyOf(count)
        copy.sort()
        return if (count and 1 == 1) copy[count / 2] else (copy[count / 2 - 1] + copy[count / 2]) / 2
    }
}
