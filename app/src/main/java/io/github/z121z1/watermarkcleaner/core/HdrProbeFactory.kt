package io.github.z121z1.watermarkcleaner.core

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Gainmap
import android.os.Build

enum class HdrProbePattern { FLAT, HORIZONTAL, VERTICAL, CHECKER }

object HdrProbeFactory {
    fun create(
        width: Int,
        height: Int,
        baseLevel: Int,
        pattern: HdrProbePattern,
        gainLevel: Int = defaultGainLevel(baseLevel),
    ): Bitmap {
        require(width > 0 && height > 0)
        require(baseLevel in 0..255 && gainLevel in 0..255)
        val base = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(baseLevel, baseLevel, baseLevel))
        }
        val gainWidth = maxOf(1, width / 4)
        val gainHeight = maxOf(1, height / 4)
        val gainPixels = IntArray(gainWidth * gainHeight)
        for (y in 0 until gainHeight) {
            for (x in 0 until gainWidth) {
                val v = when (pattern) {
                    HdrProbePattern.FLAT -> gainLevel
                    HdrProbePattern.HORIZONTAL -> (255f * x / maxOf(1, gainWidth - 1)).toInt()
                    HdrProbePattern.VERTICAL -> (255f * y / maxOf(1, gainHeight - 1)).toInt()
                    HdrProbePattern.CHECKER -> if (((x / 24) + (y / 24)) and 1 == 0) 32 else 224
                }
                gainPixels[y * gainWidth + x] = Color.rgb(v, v, v)
            }
        }
        val gainContents = Bitmap.createBitmap(gainPixels, gainWidth, gainHeight, Bitmap.Config.ARGB_8888)
        val gainmap = Gainmap(gainContents).apply {
            setRatioMin(1f, 1f, 1f)
            setRatioMax(8f, 8f, 8f) // 3 EV: enough range without needlessly stressing display headroom.
            setGamma(1f, 1f, 1f)
            setEpsilonSdr(0f, 0f, 0f)
            setEpsilonHdr(0f, 0f, 0f)
            displayRatioForFullHdr = 8f
            minDisplayRatioForHdrTransition = 1f
            if (Build.VERSION.SDK_INT >= 36) {
                gainmapDirection = Gainmap.GAINMAP_DIRECTION_SDR_TO_HDR
            }
        }
        base.gainmap = gainmap
        return base
    }

    private fun defaultGainLevel(baseLevel: Int): Int = when (baseLevel) {
        in 0..15 -> 32
        in 16..47 -> 64
        in 48..95 -> 96
        in 96..159 -> 144
        in 160..223 -> 192
        else -> 240
    }
}
