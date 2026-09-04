package io.github.z121z1.watermarkcleaner.core

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class GainMapCalibrationEngine(private val resolver: ContentResolver) {
    companion object {
        private val TRAINING_INDICES = intArrayOf(0, 1, 3, 5)
        private val VALIDATION_INDICES = intArrayOf(2, 4)
        private const val MIN_BACKGROUND_LOG_SPAN = 0.18f
        private const val MIN_OBSERVABLE_LOG_EFFECT = 0.0035f
        private const val MAX_TRAINING_RMSE = 0.018f
        private const val MAX_VALIDATION_ERROR = 0.030f
        private const val MIN_SLOPE = 0.08f
        private const val MAX_SLOPE = 1.50f
    }

    suspend fun fit(samples: List<Uri>): GainMapProfile = withContext(Dispatchers.Default) {
        require(samples.size == CalibrationEngine.LEVELS.size)
        val decoded = samples.map(::decodeSample)
        try {
            val first = decoded.first()
            require(decoded.all { it.baseWidth == first.baseWidth && it.baseHeight == first.baseHeight }) { "HDR 校准截图尺寸不一致" }
            require(decoded.all { it.channels.width == first.channels.width && it.channels.height == first.channels.height }) { "HDR 校准 gain map 尺寸不一致" }
            require(decoded.all { it.channels.layout == first.channels.layout }) { "HDR 校准 gain map 通道布局发生变化" }

            val backgroundR = FloatArray(decoded.size)
            val backgroundG = FloatArray(decoded.size)
            val backgroundB = FloatArray(decoded.size)
            decoded.forEachIndexed { sampleIndex, sample ->
                backgroundR[sampleIndex] = GainMapMath.toLogGain(mode(sample.channels.r), sample.metadata[0])
                backgroundG[sampleIndex] = GainMapMath.toLogGain(mode(sample.channels.g), sample.metadata[1])
                backgroundB[sampleIndex] = GainMapMath.toLogGain(mode(sample.channels.b), sample.metadata[2])
            }
            require(span(backgroundR) >= MIN_BACKGROUND_LOG_SPAN || span(backgroundG) >= MIN_BACKGROUND_LOG_SPAN || span(backgroundB) >= MIN_BACKGROUND_LOG_SPAN) {
                "HDR 探针没有产生足够的 gain map 动态范围；请确认在 HDR 模式下重新截图"
            }

            val count = first.channels.width * first.channels.height
            val result = ArrayList<GainMapPixel>(count / 20)
            var trainingSquared = 0.0
            var validationMax = 0f
            val observedR = FloatArray(decoded.size)
            val observedG = FloatArray(decoded.size)
            val observedB = FloatArray(decoded.size)
            var anyObservableEffect = false

            for (index in 0 until count) {
                if ((index and 0x3fff) == 0) coroutineContext.ensureActive()
                var effect = 0f
                decoded.forEachIndexed { sampleIndex, sample ->
                    observedR[sampleIndex] = GainMapMath.toLogGain(sample.channels.r[index], sample.metadata[0])
                    observedG[sampleIndex] = GainMapMath.toLogGain(sample.channels.g[index], sample.metadata[1])
                    observedB[sampleIndex] = GainMapMath.toLogGain(sample.channels.b[index], sample.metadata[2])
                    effect = max(effect, abs(observedR[sampleIndex] - backgroundR[sampleIndex]))
                    effect = max(effect, abs(observedG[sampleIndex] - backgroundG[sampleIndex]))
                    effect = max(effect, abs(observedB[sampleIndex] - backgroundB[sampleIndex]))
                }
                if (effect < MIN_OBSERVABLE_LOG_EFFECT) continue
                anyObservableEffect = true

                val fit = GainMapMath.fit(
                    backgroundR, backgroundG, backgroundB,
                    observedR, observedG, observedB,
                    TRAINING_INDICES, VALIDATION_INDICES,
                ) ?: continue
                if (!accepted(fit)) continue

                result += GainMapPixel(
                    index = index,
                    slopeR = fit.slopeR,
                    slopeG = fit.slopeG,
                    slopeB = fit.slopeB,
                    interceptR = fit.interceptR,
                    interceptG = fit.interceptG,
                    interceptB = fit.interceptB,
                    validationError = fit.validationMaxError,
                )
                trainingSquared += fit.trainingRmse * fit.trainingRmse
                validationMax = max(validationMax, fit.validationMaxError)
            }

            if (anyObservableEffect) {
                require(result.isNotEmpty()) { "检测到 gain map 中存在水印影响，但固定模型未通过留出验证；拒绝保存不可靠 HDR 模型" }
            }
            GainMapProfile(
                baseWidth = first.baseWidth,
                baseHeight = first.baseHeight,
                width = first.channels.width,
                height = first.channels.height,
                layout = first.channels.layout,
                pixels = result,
                calibrationLevels = decoded.size,
                calibrationRmse = if (result.isEmpty()) 0f else sqrt(trainingSquared / result.size).toFloat(),
                validationMaxError = validationMax,
            )
        } finally {
            decoded.forEach { it.bitmap.recycle() }
        }
    }

    private fun decodeSample(uri: Uri): DecodedGainSample {
        val bitmap = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 })
        } ?: error("无法解码 HDR 校准截图")
        try {
            val gainmap = bitmap.gainmap ?: error("这张校准截图没有 gain map；请确认 HDR 探针正在显示")
            require(GainMapBitmapIO.isSdrToHdr(gainmap)) { "暂不接受 HDR_TO_SDR gain map 校准" }
            val channels = GainMapBitmapIO.read(gainmap.gainmapContents)
            val metadata = GainMapBitmapIO.metadata(gainmap).first
            return DecodedGainSample(bitmap, bitmap.width, bitmap.height, channels, metadata)
        } catch (t: Throwable) {
            bitmap.recycle()
            throw t
        }
    }

    private fun accepted(fit: GainMapFit): Boolean {
        val slopes = floatArrayOf(fit.slopeR, fit.slopeG, fit.slopeB)
        return slopes.all { it.isFinite() && it in MIN_SLOPE..MAX_SLOPE } && fit.trainingRmse <= MAX_TRAINING_RMSE && fit.validationMaxError <= MAX_VALIDATION_ERROR
    }

    private fun mode(values: IntArray): Int {
        val histogram = IntArray(256)
        values.forEach { histogram[it.coerceIn(0, 255)]++ }
        var best = 0
        for (i in 1 until histogram.size) if (histogram[i] > histogram[best]) best = i
        return best
    }

    private fun span(values: FloatArray): Float = (values.maxOrNull() ?: 0f) - (values.minOrNull() ?: 0f)

    private data class DecodedGainSample(
        val bitmap: Bitmap,
        val baseWidth: Int,
        val baseHeight: Int,
        val channels: GainMapChannels,
        val metadata: Array<GainChannelMetadata>,
    )
}
