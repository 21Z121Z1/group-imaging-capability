package io.github.z121z1.watermarkcleaner.core

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.InputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class CalibrationEngine(private val resolver: ContentResolver) {
    companion object {
        // 96 and 160 are hold-out levels: they are never used to fit coefficients.
        val LEVELS = intArrayOf(0, 32, 64, 96, 128, 160, 192, 255)
        private val TRAINING_INDICES = intArrayOf(0, 1, 2, 4, 6, 7)
        private val VALIDATION_INDICES = intArrayOf(3, 5)
        private const val STRIP_HEIGHT = 64
        private const val MIN_OBSERVABLE_EFFECT = 0.50f
        private const val MIN_ALPHA = 0.0010f
        private const val MAX_TRAINING_RMSE = 0.55f
        private const val MAX_VALIDATION_ERROR = 1.10f
        private const val STRONG_ENCODED_RMSE = 0.30f
        private const val STRONG_ENCODED_VALIDATION = 0.55f
    }

    suspend fun fit(samples: List<Uri>): WatermarkProfile = withContext(Dispatchers.Default) {
        require(samples.size == LEVELS.size)
        val sources = samples.map { openRegionSource(it) }
        try {
            val width = sources.first().decoder.width
            val height = sources.first().decoder.height
            require(sources.all { it.decoder.width == width && it.decoder.height == height }) {
                "Calibration screenshots must have identical dimensions"
            }

            val result = ArrayList<CompositePixel>(width * height / 20)
            var trainingSquaredError = 0.0
            var validationMaxError = 0f

            var y0 = 0
            while (y0 < height) {
                coroutineContext.ensureActive()
                val stripHeight = minOf(STRIP_HEIGHT, height - y0)
                val count = width * stripHeight
                val samplePixels = Array(LEVELS.size) { IntArray(count) }

                sources.forEachIndexed { sampleIndex, source ->
                    val bitmap = source.decoder.decodeRegion(
                        Rect(0, y0, width, y0 + stripHeight),
                        BitmapFactory.Options().apply { inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 },
                    ) ?: error("Unable to decode calibration strip")
                    try {
                        bitmap.getPixels(samplePixels[sampleIndex], 0, width, 0, 0, width, stripHeight)
                    } finally {
                        bitmap.recycle()
                    }
                }

                // Use the modal pixel as the actual screenshot baseline after color conversion/quantization.
                // This prevents a global screenshot transform from being learned as part of the watermark.
                val backgroundR = IntArray(LEVELS.size)
                val backgroundG = IntArray(LEVELS.size)
                val backgroundB = IntArray(LEVELS.size)
                samplePixels.forEachIndexed { sampleIndex, pixels ->
                    val baseline = modalBackground(pixels)
                    backgroundR[sampleIndex] = baseline.r
                    backgroundG[sampleIndex] = baseline.g
                    backgroundB[sampleIndex] = baseline.b
                }

                val observedR = IntArray(LEVELS.size)
                val observedG = IntArray(LEVELS.size)
                val observedB = IntArray(LEVELS.size)
                val last = LEVELS.lastIndex
                for (i in 0 until count) {
                    for (sampleIndex in LEVELS.indices) {
                        val color = samplePixels[sampleIndex][i]
                        observedR[sampleIndex] = (color ushr 16) and 0xff
                        observedG[sampleIndex] = (color ushr 8) and 0xff
                        observedB[sampleIndex] = color and 0xff
                    }

                    val endpointEffect = maxOf(
                        abs(observedR[0] - backgroundR[0]),
                        abs(observedG[0] - backgroundG[0]),
                        abs(observedB[0] - backgroundB[0]),
                        abs(observedR[last] - backgroundR[last]),
                        abs(observedG[last] - backgroundG[last]),
                        abs(observedB[last] - backgroundB[last]),
                    ).toFloat()
                    if (endpointEffect < MIN_OBSERVABLE_EFFECT) continue

                    val encoded = CompositeMath.fit(
                        backgroundR, backgroundG, backgroundB,
                        observedR, observedG, observedB,
                        TRAINING_INDICES, VALIDATION_INDICES,
                        BlendSpace.ENCODED_SRGB,
                    )
                    val encodedAccepted = encoded?.let(::isAccepted) == true
                    val selected = if (
                        encodedAccepted && encoded != null &&
                        encoded.trainingRmse <= STRONG_ENCODED_RMSE &&
                        encoded.validationMaxError <= STRONG_ENCODED_VALIDATION
                    ) {
                        encoded
                    } else {
                        val linear = CompositeMath.fit(
                            backgroundR, backgroundG, backgroundB,
                            observedR, observedG, observedB,
                            TRAINING_INDICES, VALIDATION_INDICES,
                            BlendSpace.LINEAR_SRGB,
                        )
                        val linearAccepted = linear?.let(::isAccepted) == true
                        when {
                            encodedAccepted && linearAccepted && encoded != null && linear != null -> betterFit(encoded, linear)
                            encodedAccepted -> encoded
                            linearAccepted -> linear
                            else -> null
                        }
                    } ?: continue

                    val meanAlpha = ((1f - selected.slopeR) + (1f - selected.slopeG) + (1f - selected.slopeB)) / 3f
                    if (meanAlpha < MIN_ALPHA) continue

                    val localY = i / width
                    val localX = i - localY * width
                    result += CompositePixel(
                        index = (y0 + localY) * width + localX,
                        blendSpace = selected.blendSpace,
                        slopeR = selected.slopeR,
                        slopeG = selected.slopeG,
                        slopeB = selected.slopeB,
                        interceptR = selected.interceptR,
                        interceptG = selected.interceptG,
                        interceptB = selected.interceptB,
                        validationError = selected.validationMaxError,
                    )
                    trainingSquaredError += selected.trainingRmse * selected.trainingRmse
                    validationMaxError = max(validationMaxError, selected.validationMaxError)
                }
                y0 += stripHeight
            }
            require(result.isNotEmpty()) { "No verified watermark pixels found" }
            WatermarkProfile(
                width = width,
                height = height,
                pixels = result,
                calibrationLevels = LEVELS.size,
                calibrationRmse = sqrt(trainingSquaredError / result.size).toFloat(),
                validationMaxError = validationMaxError,
            )
        } finally {
            sources.forEach { it.close() }
        }
    }

    private fun isAccepted(fit: CompositeFit): Boolean =
        CompositeMath.isPhysicalSourceOver(fit) &&
            fit.trainingRmse <= MAX_TRAINING_RMSE &&
            fit.validationMaxError <= MAX_VALIDATION_ERROR

    private fun betterFit(a: CompositeFit, b: CompositeFit): CompositeFit = when {
        b.validationMaxError < a.validationMaxError - 0.05f -> b
        a.validationMaxError < b.validationMaxError - 0.05f -> a
        b.trainingRmse < a.trainingRmse -> b
        else -> a
    }

    private fun modalBackground(pixels: IntArray): Rgb {
        val r = IntArray(256)
        val g = IntArray(256)
        val b = IntArray(256)
        for (color in pixels) {
            r[(color ushr 16) and 0xff]++
            g[(color ushr 8) and 0xff]++
            b[color and 0xff]++
        }
        return Rgb(mode(r), mode(g), mode(b))
    }

    private fun mode(histogram: IntArray): Int {
        var bestValue = 0
        var bestCount = -1
        for (value in histogram.indices) {
            if (histogram[value] > bestCount) {
                bestValue = value
                bestCount = histogram[value]
            }
        }
        return bestValue
    }

    private fun openRegionSource(uri: Uri): RegionSource {
        val stream = resolver.openInputStream(uri) ?: error("Unable to open $uri")
        return try {
            val decoder = BitmapRegionDecoder.newInstance(stream, false)
                ?: error("Unsupported calibration image")
            RegionSource(stream, decoder)
        } catch (t: Throwable) {
            stream.close()
            throw t
        }
    }

    private data class Rgb(val r: Int, val g: Int, val b: Int)

    private data class RegionSource(val stream: InputStream, val decoder: BitmapRegionDecoder) : Closeable {
        override fun close() {
            decoder.recycle()
            stream.close()
        }
    }
}
