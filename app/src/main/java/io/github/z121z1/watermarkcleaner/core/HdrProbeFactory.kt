package io.github.z121z1.watermarkcleaner.core

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Gainmap
import kotlin.math.pow

enum class HdrProbePattern { FLAT, HORIZONTAL, VERTICAL, CHECKER }

object HdrProbeFactory {
    fun create(width: Int, height: Int, baseLevel: Int, pattern: HdrProbePattern): Bitmap {
        require(width > 0 && height > 0)
        val base = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(baseLevel, baseLevel, baseLevel))
        }
        val gainWidth = maxOf(1, width / 4)
        val gainHeight = maxOf(1, height / 4)
        val gainPixels = IntArray(gainWidth * gainHeight)
        for (y in 0 until gainHeight) {
            for (x in 0 until gainWidth) {
                val v = when (pattern) {
                    HdrProbePattern.FLAT -> 0
                    HdrProbePattern.HORIZONTAL -> (255f * x / maxOf(1, gainWidth - 1)).toInt()
                    HdrProbePattern.VERTICAL -> (255f * y / maxOf(1, gainHeight - 1)).toInt()
                    HdrProbePattern.CHECKER -> if (((x / 24) + (y / 24)) and 1 == 0) 0 else 255
                }
                gainPixels[y * gainWidth + x] = Color.rgb(v, v, v)
            }
        }
        val gainContents = Bitmap.createBitmap(gainPixels, gainWidth, gainHeight, Bitmap.Config.ARGB_8888)
        val gainmap = Gainmap(gainContents).apply {
            setRatioMin(1f, 1f, 1f)
            setRatioMax(16f, 16f, 16f) // 4 EV probe range
            setGamma(1f, 1f, 1f)
            setEpsilonSdr(0f, 0f, 0f)
            setEpsilonHdr(0f, 0f, 0f)
            displayRatioForFullHdr = 16f
            minDisplayRatioForHdrTransition = 1f
        }
        base.gainmap = gainmap
        return base
    }
}
