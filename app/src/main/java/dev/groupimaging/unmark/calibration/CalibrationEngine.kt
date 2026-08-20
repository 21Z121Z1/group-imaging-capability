package dev.groupimaging.unmark.calibration

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.ParcelFileDescriptor
import dev.groupimaging.unmark.model.AffineMath
import dev.groupimaging.unmark.model.WatermarkProfile
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

class CalibrationEngine(private val resolver: ContentResolver) {
    enum class SelectionMode {
        ScreenOverlay,
        GenericDeviation,
    }

    fun fit(flatScreenshots: List<Uri>): WatermarkProfile = fit(
        screenshots = flatScreenshots,
        xLevels = AffineMath.calibrationLevels.map(Int::toFloat).toFloatArray(),
        selectionMode = SelectionMode.ScreenOverlay,
    )

    @Suppress("DEPRECATION")
    fun fit(
        screenshots: List<Uri>,
        xLevels: FloatArray,
        selectionMode: SelectionMode,
    ): WatermarkProfile {
        require(screenshots.size == xLevels.size && screenshots.size >= 3) {
            "校准截图数量与输入档位不一致"
        }

        val descriptors = ArrayList<ParcelFileDescriptor>(screenshots.size)
        val decoders = ArrayList<BitmapRegionDecoder>(screenshots.size)
        try {
            screenshots.forEach { uri ->
                descriptors += resolver.openFileDescriptor(uri, "r") ?: error("无法打开校准截图")
            }
            descriptors.forEach { descriptor ->
                decoders += BitmapRegionDecoder.newInstance(descriptor.fileDescriptor, false)
            }
            return fitDecoders(decoders, xLevels, selectionMode)
        } finally {
            decoders.forEach { runCatching { it.recycle() } }
            descriptors.forEach { runCatching { it.close() } }
        }
    }

    @Suppress("DEPRECATION")
    fun fitFiles(
        images: List<File>,
        xLevels: FloatArray,
        selectionMode: SelectionMode,
    ): WatermarkProfile {
        require(images.size == xLevels.size && images.size >= 3) {
            "校准文件数量与输入档位不一致"
        }
        val decoders = ArrayList<BitmapRegionDecoder>(images.size)
        try {
            images.forEach { file ->
                decoders += BitmapRegionDecoder.newInstance(file.absolutePath, false)
            }
            return fitDecoders(decoders, xLevels, selectionMode)
        } finally {
            decoders.forEach { runCatching { it.recycle() } }
        }
    }

    /**
     * Fits a stable additive residual profile. Each image supplies its own neutral background
     * baseline, so this remains identifiable even when an OEM screenshot encoder keeps a gain map
     * near the same neutral code value across all HDR probe levels.
     */
    @Suppress("DEPRECATION")
    fun fitResidualOffsets(images: List<File>, baselines: FloatArray): WatermarkProfile {
        require(images.size == baselines.size && images.size >= 3) {
            "残差校准文件数量与背景档位不一致"
        }
        val decoders = ArrayList<BitmapRegionDecoder>(images.size)
        try {
            images.forEach { file ->
                decoders += BitmapRegionDecoder.newInstance(file.absolutePath, false)
            }
            validateDimensions(decoders)
            val width = decoders.first().width
            val height = decoders.first().height
            val builder = ProfileBuilder(width, height)
            val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }

            var top = 0
            while (top < height) {
                val bottom = minOf(top + STRIPE_HEIGHT, height)
                val stripeHeight = bottom - top
                val pixelCount = width * stripeHeight
                val stripes = decoders.map { decoder ->
                    decoder.decodeRegion(Rect(0, top, width, bottom), options)
                        ?: error("无法解码 gain map 校准区域")
                }
                val pixels = Array(stripes.size) { IntArray(pixelCount) }
                try {
                    stripes.forEachIndexed { index, bitmap ->
                        bitmap.getPixels(pixels[index], 0, width, 0, 0, width, stripeHeight)
                    }
                    for (local in 0 until pixelCount) {
                        val stats = residualStats(pixels, local, baselines)
                        if (shouldKeepResidual(stats)) {
                            builder.add(
                                index = top * width + local,
                                slopeR = 1f,
                                slopeG = 1f,
                                slopeB = 1f,
                                interceptR = stats[0],
                                interceptG = stats[1],
                                interceptB = stats[2],
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
        }
    }

    private fun fitDecoders(
        decoders: List<BitmapRegionDecoder>,
        xLevels: FloatArray,
        selectionMode: SelectionMode,
    ): WatermarkProfile {
        validateDimensions(decoders)
        val basis = RegressionBasis(xLevels)
        val width = decoders.first().width
        val height = decoders.first().height
        val builder = ProfileBuilder(width, height)
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
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
                    fitRgbAt(pixels, local, stats, basis)
                    if (shouldKeep(stats, selectionMode)) {
                        builder.add(
                            index = top * width + local,
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
    }

    private fun validateDimensions(decoders: List<BitmapRegionDecoder>) {
        require(decoders.isNotEmpty()) { "缺少校准图像" }
        val width = decoders.first().width
        val height = decoders.first().height
        require(decoders.all { it.width == width && it.height == height }) {
            "校准图像的分辨率必须完全一致"
        }
    }

    /** stats: slope RGB [0..2], intercept RGB [3..5], rmse RGB [6..8]. */
    private fun fitRgbAt(
        pixels: Array<IntArray>,
        offset: Int,
        stats: FloatArray,
        basis: RegressionBasis,
    ) {
        for (channel in 0..2) {
            var sumY = 0.0
            var sumXY = 0.0
            var sumY2 = 0.0
            for (i in pixels.indices) {
                val argb = pixels[i][offset]
                val value = channel(argb, channel)
                val x = basis.levels[i].toDouble()
                sumY += value
                sumXY += x * value
                sumY2 += value * value
            }

            val slope = (basis.n * sumXY - basis.sumX * sumY) / basis.denominator
            val intercept = (sumY - slope * basis.sumX) / basis.n
            val sse = (
                sumY2 - 2.0 * slope * sumXY - 2.0 * intercept * sumY +
                    slope * slope * basis.sumXX + 2.0 * slope * intercept * basis.sumX +
                    basis.n * intercept * intercept
                ).coerceAtLeast(0.0)

            stats[channel] = slope.toFloat()
            stats[3 + channel] = intercept.toFloat()
            stats[6 + channel] = sqrt(sse / basis.n).toFloat()
        }
    }

    /** Returns mean residual RGB [0..2], RMSE RGB [3..5]. */
    private fun residualStats(
        pixels: Array<IntArray>,
        offset: Int,
        baselines: FloatArray,
    ): FloatArray {
        val result = FloatArray(6)
        for (channel in 0..2) {
            var sum = 0.0
            var sumSquares = 0.0
            for (i in pixels.indices) {
                val residual = channel(pixels[i][offset], channel) - baselines[i]
                sum += residual
                sumSquares += residual * residual
            }
            val mean = sum / pixels.size
            val variance = (sumSquares / pixels.size - mean * mean).coerceAtLeast(0.0)
            result[channel] = mean.toFloat()
            result[3 + channel] = sqrt(variance).toFloat()
        }
        return result
    }

    private fun shouldKeep(stats: FloatArray, mode: SelectionMode): Boolean = when (mode) {
        SelectionMode.ScreenOverlay -> {
            for (channel in 0..2) {
                if (stats[channel] !in 0.84f..1.01f) return false
                if (stats[3 + channel] !in -1f..32f) return false
                if (stats[6 + channel] >= 0.9f) return false
            }
            val meanIntercept = (stats[3] + stats[4] + stats[5]) / 3f
            val meanAlpha = ((1f - stats[0]) + (1f - stats[1]) + (1f - stats[2])) / 3f
            if (meanIntercept <= 0.20f && meanAlpha <= 0.0015f) return false
            if (meanAlpha > 0.002f && meanIntercept / meanAlpha !in 110f..235f) return false
            true
        }

        SelectionMode.GenericDeviation -> {
            for (channel in 0..2) {
                if (stats[channel] !in 0.25f..1.5f) return false
                if (stats[3 + channel] !in -128f..128f) return false
                if (stats[6 + channel] >= 1.5f) return false
            }
            val maxSlopeDelta = (0..2).maxOf { abs(1f - stats[it]) }
            val maxIntercept = (3..5).maxOf { abs(stats[it]) }
            maxSlopeDelta > 0.003f || maxIntercept > 0.5f
        }
    }

    private fun shouldKeepResidual(stats: FloatArray): Boolean {
        if ((3..5).any { stats[it] >= 2.0f }) return false
        return (0..2).maxOf { abs(stats[it]) } > 0.75f
    }

    private fun channel(argb: Int, channel: Int): Double = when (channel) {
        0 -> (argb ushr 16 and 0xff).toDouble()
        1 -> (argb ushr 8 and 0xff).toDouble()
        else -> (argb and 0xff).toDouble()
    }

    private data class RegressionBasis(val levels: FloatArray) {
        val n = levels.size.toDouble()
        val sumX = levels.sumOf { it.toDouble() }
        val sumXX = levels.sumOf { it.toDouble() * it.toDouble() }
        val denominator = n * sumXX - sumX * sumX

        init {
            require(levels.all(Float::isFinite)) { "校准档位包含无效值" }
            require(denominator > 1e-3) { "校准档位变化不足，无法拟合" }
        }
    }

    private class ProfileBuilder(private val width: Int, private val height: Int) {
        private var capacity = minOf(65_536, width * height).coerceAtLeast(1)
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
    }
}
