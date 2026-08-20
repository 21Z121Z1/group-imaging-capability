package dev.groupimaging.unmark.calibration

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import dev.groupimaging.unmark.model.AffineMath
import dev.groupimaging.unmark.model.WatermarkProfile
import kotlin.math.sqrt

class CalibrationEngine(private val resolver: ContentResolver) {
    @Suppress("DEPRECATION")
    fun fit(flatScreenshots: List<Uri>): WatermarkProfile {
        require(flatScreenshots.size == AffineMath.calibrationLevels.size) {
            "需要六张灰阶校准截图"
        }

        val descriptors = flatScreenshots.map { uri ->
            resolver.openFileDescriptor(uri, "r") ?: error("无法打开校准截图")
        }
        val decoders = try {
            descriptors.map { BitmapRegionDecoder.newInstance(it.fileDescriptor, false) }
        } catch (t: Throwable) {
            descriptors.forEach { runCatching { it.close() } }
            throw t
        }

        try {
            val width = decoders.first().width
            val height = decoders.first().height
            require(decoders.all { it.width == width && it.height == height }) {
                "六张校准截图的分辨率必须完全一致"
            }

            val builder = ProfileBuilder(width, height)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val stats = FloatArray(9)

            var top = 0
            while (top < height) {
                val bottom = minOf(top + STRIPE_HEIGHT, height)
                val stripeHeight = bottom - top
                val pixelCount = width * stripeHeight
                val stripes = decoders.map { decoder ->
                    decoder.decodeRegion(Rect(0, top, width, bottom), options)
                        ?: error("无法解码校准截图区域")
                }
                val pixels = Array(stripes.size) { IntArray(pixelCount) }
                try {
                    stripes.forEachIndexed { index, bitmap ->
                        bitmap.getPixels(pixels[index], 0, width, 0, 0, width, stripeHeight)
                    }

                    for (local in 0 until pixelCount) {
                        fitRgbAt(pixels, local, stats)
                        if (shouldKeep(stats)) {
                            val global = top * width + local
                            builder.add(
                                index = global,
                                slopeR = stats[0],
                                slopeG = stats[1],
                                slopeB = stats[2],
                                interceptR = stats[3],
                                interceptG = stats[4],
                                interceptB = stats[5],
                            )
                        }
                    }
                } finally {
                    stripes.forEach(Bitmap::recycle)
                }
                top = bottom
            }

            return builder.build()
        } finally {
            decoders.forEach { runCatching { it.recycle() } }
            descriptors.forEach { runCatching { it.close() } }
        }
    }

    /** stats: slope RGB [0..2], intercept RGB [3..5], rmse RGB [6..8]. */
    private fun fitRgbAt(pixels: Array<IntArray>, offset: Int, stats: FloatArray) {
        for (channel in 0..2) {
            var sumY = 0.0
            var sumXY = 0.0
            var sumY2 = 0.0
            for (i in pixels.indices) {
                val argb = pixels[i][offset]
                val value = when (channel) {
                    0 -> argb ushr 16 and 0xff
                    1 -> argb ushr 8 and 0xff
                    else -> argb and 0xff
                }
                val x = AffineMath.calibrationLevels[i]
                sumY += value
                sumXY += x * value.toDouble()
                sumY2 += value * value.toDouble()
            }

            val slope = (N * sumXY - SUM_X * sumY) / DENOMINATOR
            val intercept = (sumY - slope * SUM_X) / N
            val sse = (
                sumY2 - 2.0 * slope * sumXY - 2.0 * intercept * sumY +
                    slope * slope * SUM_XX + 2.0 * slope * intercept * SUM_X +
                    N * intercept * intercept
                ).coerceAtLeast(0.0)

            stats[channel] = slope.toFloat()
            stats[3 + channel] = intercept.toFloat()
            stats[6 + channel] = sqrt(sse / N).toFloat()
        }
    }

    private fun shouldKeep(stats: FloatArray): Boolean {
        for (channel in 0..2) {
            if (stats[channel] !in 0.84f..1.01f) return false
            if (stats[3 + channel] !in -1f..32f) return false
            if (stats[6 + channel] >= 0.9f) return false
        }
        val meanIntercept = (stats[3] + stats[4] + stats[5]) / 3f
        val meanAlpha = ((1f - stats[0]) + (1f - stats[1]) + (1f - stats[2])) / 3f
        if (meanIntercept <= 0.20f && meanAlpha <= 0.0015f) return false
        if (meanAlpha > 0.002f && meanIntercept / meanAlpha !in 110f..235f) return false
        return true
    }

    private class ProfileBuilder(private val width: Int, private val height: Int) {
        private var capacity = 65_536
        private var count = 0
        private var indices = IntArray(capacity)
        private var slopeR = FloatArray(capacity)
        private var slopeG = FloatArray(capacity)
        private var slopeB = FloatArray(capacity)
        private var interceptR = FloatArray(capacity)
        private var interceptG = FloatArray(capacity)
        private var interceptB = FloatArray(capacity)

        fun add(
            index: Int,
            slopeR: Float,
            slopeG: Float,
            slopeB: Float,
            interceptR: Float,
            interceptG: Float,
            interceptB: Float,
        ) {
            if (count == capacity) grow()
            indices[count] = index
            this.slopeR[count] = slopeR
            this.slopeG[count] = slopeG
            this.slopeB[count] = slopeB
            this.interceptR[count] = interceptR
            this.interceptG[count] = interceptG
            this.interceptB[count] = interceptB
            count++
        }

        fun build(): WatermarkProfile = WatermarkProfile(
            width = width,
            height = height,
            indices = indices.copyOf(count),
            slopeR = slopeR.copyOf(count),
            slopeG = slopeG.copyOf(count),
            slopeB = slopeB.copyOf(count),
            interceptR = interceptR.copyOf(count),
            interceptG = interceptG.copyOf(count),
            interceptB = interceptB.copyOf(count),
        )

        private fun grow() {
            capacity = (capacity * 2).coerceAtMost(width * height)
            require(capacity > count) { "校准模型过大" }
            indices = indices.copyOf(capacity)
            slopeR = slopeR.copyOf(capacity)
            slopeG = slopeG.copyOf(capacity)
            slopeB = slopeB.copyOf(capacity)
            interceptR = interceptR.copyOf(capacity)
            interceptG = interceptG.copyOf(capacity)
            interceptB = interceptB.copyOf(capacity)
        }
    }

    companion object {
        private const val STRIPE_HEIGHT = 64
        private const val N = 6.0
        private val SUM_X = AffineMath.calibrationLevels.sum().toDouble()
        private val SUM_XX = AffineMath.calibrationLevels.sumOf { it.toDouble() * it.toDouble() }
        private val DENOMINATOR = N * SUM_XX - SUM_X * SUM_X
    }
}
