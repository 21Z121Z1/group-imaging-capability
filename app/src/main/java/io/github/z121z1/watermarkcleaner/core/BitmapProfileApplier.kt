package io.github.z121z1.watermarkcleaner.core

import android.graphics.Bitmap

object BitmapProfileApplier {
    private const val STRIP_HEIGHT = 64

    fun apply(bitmap: Bitmap, profile: WatermarkProfile) {
        require(bitmap.isMutable) { "Bitmap must be mutable" }
        require(bitmap.width == profile.width && bitmap.height == profile.height) {
            "Profile ${profile.width}x${profile.height} does not match bitmap ${bitmap.width}x${bitmap.height}"
        }
        if (profile.pixels.isEmpty()) return

        val width = bitmap.width
        var profileCursor = 0
        var y0 = 0
        while (y0 < bitmap.height && profileCursor < profile.pixels.size) {
            val stripHeight = minOf(STRIP_HEIGHT, bitmap.height - y0)
            val stripStart = y0 * width
            val stripEnd = (y0 + stripHeight) * width
            while (profileCursor < profile.pixels.size && profile.pixels[profileCursor].index < stripStart) {
                profileCursor++
            }
            if (profileCursor >= profile.pixels.size) break
            if (profile.pixels[profileCursor].index >= stripEnd) {
                y0 += stripHeight
                continue
            }

            val pixels = IntArray(width * stripHeight)
            bitmap.getPixels(pixels, 0, width, 0, y0, width, stripHeight)
            var cursor = profileCursor
            while (cursor < profile.pixels.size) {
                val model = profile.pixels[cursor]
                if (model.index >= stripEnd) break
                val local = model.index - stripStart
                val argb = pixels[local]
                val a = (argb ushr 24) and 0xff
                val r = (argb ushr 16) and 0xff
                val g = (argb ushr 8) and 0xff
                val b = argb and 0xff
                val rr = AffineMath.invert(r, model.slopeR, model.interceptR)
                val gg = AffineMath.invert(g, model.slopeG, model.interceptG)
                val bb = AffineMath.invert(b, model.slopeB, model.interceptB)
                pixels[local] = (a shl 24) or (rr shl 16) or (gg shl 8) or bb
                cursor++
            }
            bitmap.setPixels(pixels, 0, width, 0, y0, width, stripHeight)
            profileCursor = cursor
            y0 += stripHeight
        }
    }
}
