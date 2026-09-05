package io.github.z121z1.watermarkcleaner.core

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class GainChannelMetadata(
    val ratioMin: Float,
    val ratioMax: Float,
    val gamma: Float,
) {
    init {
        require(ratioMin.isFinite() && ratioMin > 0f)
        require(ratioMax.isFinite() && ratioMax > 0f)
        require(gamma.isFinite() && gamma > 0f)
    }
}

data class GainMapFit(
    val slopeR: Float,
    val slopeG: Float,
    val slopeB: Float,
    val interceptR: Float,
    val interceptG: Float,
    val interceptB: Float,
    val trainingRmse: Float,
    val validationMaxError: Float,
)

object GainMapMath {
    fun toLogGain(code: Int, metadata: GainChannelMetadata): Float {
        val g = code.coerceIn(0, 255) / 255f
        val t = g.toDouble().pow(metadata.gamma.toDouble()).toFloat()
        val lo = ln(metadata.ratioMin.toDouble()).toFloat()
        val hi = ln(metadata.ratioMax.toDouble()).toFloat()
        return lo + (hi - lo) * t
    }

    fun fromLogGain(logGain: Float, metadata: GainChannelMetadata): Int {
        val lo = ln(metadata.ratioMin.toDouble()).toFloat()
        val hi = ln(metadata.ratioMax.toDouble()).toFloat()
        val span = hi - lo
        if (!logGain.isFinite() || abs(span) < 1e-7f) return 0
        val normalized = ((logGain - lo) / span).coerceIn(0f, 1f)
        val encoded = normalized.toDouble().pow(1.0 / metadata.gamma.toDouble()).toFloat()
        return (encoded * 255f).roundToInt().coerceIn(0, 255)
    }

    fun fit(
        backgroundR: FloatArray,
        backgroundG: FloatArray,
        backgroundB: FloatArray,
        observedR: FloatArray,
        observedG: FloatArray,
        observedB: FloatArray,
        trainingIndices: IntArray,
        validationIndices: IntArray,
    ): GainMapFit? {
        val count = backgroundR.size
        require(count >= 4)
        require(backgroundG.size == count && backgroundB.size == count)
        require(observedR.size == count && observedG.size == count && observedB.size == count)
        require(trainingIndices.size >= 2 && validationIndices.isNotEmpty())
        val r = fitChannel(backgroundR, observedR, trainingIndices) ?: return null
        val g = fitChannel(backgroundG, observedG, trainingIndices) ?: return null
        val b = fitChannel(backgroundB, observedB, trainingIndices) ?: return null

        var squared = 0.0
        var validationMax = 0f
        for (index in trainingIndices) {
            val er = observedR[index] - (r.slope * backgroundR[index] + r.intercept)
            val eg = observedG[index] - (g.slope * backgroundG[index] + g.intercept)
            val eb = observedB[index] - (b.slope * backgroundB[index] + b.intercept)
            squared += er * er + eg * eg + eb * eb
        }
        for (index in validationIndices) {
            validationMax = max(validationMax, abs(observedR[index] - (r.slope * backgroundR[index] + r.intercept)))
            validationMax = max(validationMax, abs(observedG[index] - (g.slope * backgroundG[index] + g.intercept)))
            validationMax = max(validationMax, abs(observedB[index] - (b.slope * backgroundB[index] + b.intercept)))
        }
        return GainMapFit(
            slopeR = r.slope,
            slopeG = g.slope,
            slopeB = b.slope,
            interceptR = r.intercept,
            interceptG = g.intercept,
            interceptB = b.intercept,
            trainingRmse = sqrt(squared / (trainingIndices.size * 3.0)).toFloat(),
            validationMaxError = validationMax,
        )
    }

    fun invertLogGain(observedLogGain: Float, slope: Float, intercept: Float): Float {
        if (!observedLogGain.isFinite() || !slope.isFinite() || !intercept.isFinite() || slope <= 0f) {
            return observedLogGain
        }
        return (observedLogGain - intercept) / slope
    }

    fun reconstructedHdr(baseLinear: Float, logGain: Float, epsilonSdr: Float, epsilonHdr: Float): Float =
        ((baseLinear + epsilonSdr) * exp(logGain.toDouble()) - epsilonHdr).toFloat()

    private fun fitChannel(background: FloatArray, observed: FloatArray, indices: IntArray): ChannelFit? {
        val n = indices.size.toDouble()
        var sx = 0.0
        var sy = 0.0
        var sxx = 0.0
        var sxy = 0.0
        for (index in indices) {
            val x = background[index].toDouble()
            val y = observed[index].toDouble()
            sx += x
            sy += y
            sxx += x * x
            sxy += x * y
        }
        val denominator = n * sxx - sx * sx
        if (abs(denominator) <= 1e-12) return null
        val slope = (n * sxy - sx * sy) / denominator
        val intercept = (sy - slope * sx) / n
        if (!slope.isFinite() || !intercept.isFinite()) return null
        return ChannelFit(slope.toFloat(), intercept.toFloat())
    }

    private data class ChannelFit(val slope: Float, val intercept: Float)
}
