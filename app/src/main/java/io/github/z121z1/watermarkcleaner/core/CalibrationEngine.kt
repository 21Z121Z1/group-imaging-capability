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
import kotlin.math.max
import kotlin.math.sqrt

class CalibrationEngine(private val resolver: ContentResolver) {
    companion object {
        val LEVELS = intArrayOf(0, 32, 64, 128, 192, 255)
        private const val STRIP_HEIGHT = 64
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

            val x = LEVELS.map(Int::toDouble)
            val n = x.size.toDouble()
            val sx = x.sum()
            val sxx = x.sumOf { it * it }
            val denominator = n * sxx - sx * sx
            val result = ArrayList<AffinePixel>(width * height / 25)

            var y0 = 0
            while (y0 < height) {
                coroutineContext.ensureActive()
                val stripHeight = minOf(STRIP_HEIGHT, height - y0)
                val count = width * stripHeight
                val sy = Array(3) { DoubleArray(count) }
                val sxy = Array(3) { DoubleArray(count) }
                val sy2 = Array(3) { DoubleArray(count) }

                sources.forEachIndexed { sampleIndex, source ->
                    val bitmap = source.decoder.decodeRegion(
                        Rect(0, y0, width, y0 + stripHeight),
                        BitmapFactory.Options().apply { inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 },
                    ) ?: error("Unable to decode calibration strip")
                    try {
                        val pixels = IntArray(count)
                        bitmap.getPixels(pixels, 0, width, 0, 0, width, stripHeight)
                        val xv = x[sampleIndex]
                        for (i in pixels.indices) {
                            val color = pixels[i]
                            val values = intArrayOf(
                                (color ushr 16) and 0xff,
                                (color ushr 8) and 0xff,
                                color and 0xff,
                            )
                            for (c in 0..2) {
                                val v = values[c].toDouble()
                                sy[c][i] += v
                                sxy[c][i] += xv * v
                                sy2[c][i] += v * v
                            }
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }

                for (i in 0 until count) {
                    val slopes = FloatArray(3)
                    val intercepts = FloatArray(3)
                    val rmses = FloatArray(3)
                    var acceptable = true
                    for (c in 0..2) {
                        val slope = (n * sxy[c][i] - sx * sy[c][i]) / denominator
                        val intercept = (sy[c][i] - slope * sx) / n
                        val sse = max(
                            0.0,
                            sy2[c][i] - 2 * slope * sxy[c][i] - 2 * intercept * sy[c][i] +
                                slope * slope * sxx + 2 * slope * intercept * sx + n * intercept * intercept,
                        )
                        val rmse = sqrt(sse / n)
                        slopes[c] = slope.toFloat()
                        intercepts[c] = intercept.toFloat()
                        rmses[c] = rmse.toFloat()
                        if (rmse >= 0.9 || slope !in 0.84..1.01 || intercept !in -1.0..32.0) {
                            acceptable = false
                        }
                    }
                    if (!acceptable) continue

                    val meanAlpha = ((1f - slopes[0]) + (1f - slopes[1]) + (1f - slopes[2])) / 3f
                    val meanIntercept = (intercepts[0] + intercepts[1] + intercepts[2]) / 3f
                    if (meanIntercept <= 0.20f && meanAlpha <= 0.0015f) continue
                    if (meanAlpha > 0.002f) {
                        val inferred = meanIntercept / max(meanAlpha, 1e-6f)
                        if (inferred !in 110f..235f) continue
                    }

                    val localY = i / width
                    val localX = i - localY * width
                    result += AffinePixel(
                        index = (y0 + localY) * width + localX,
                        slopeR = slopes[0], slopeG = slopes[1], slopeB = slopes[2],
                        interceptR = intercepts[0], interceptG = intercepts[1], interceptB = intercepts[2],
                    )
                }
                y0 += stripHeight
            }
            require(result.isNotEmpty()) { "No stable watermark pixels found" }
            WatermarkProfile(width, height, result)
        } finally {
            sources.forEach { it.close() }
        }
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

    private data class RegionSource(val stream: InputStream, val decoder: BitmapRegionDecoder) : Closeable {
        override fun close() {
            decoder.recycle()
            stream.close()
        }
    }
}
