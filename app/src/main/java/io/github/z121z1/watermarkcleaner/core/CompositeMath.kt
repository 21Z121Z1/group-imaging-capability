package io.github.z121z1.watermarkcleaner.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class BlendSpace(val wireValue: Int) {
    ENCODED_SRGB(0),
    LINEAR_SRGB(1),
    ;

    companion object {
        fun fromWireValue(value: Int): BlendSpace = entries.firstOrNull { it.wireValue == value }
            ?: throw IllegalArgumentException("unsupported blend space: $value")
    }
}

data class CompositeFit(
    val blendSpace: BlendSpace,
    val slopeR: Float,
    val slopeG: Float,
    val slopeB: Float,
    val interceptR: Float,
    val interceptG: Float,
    val interceptB: Float,
    val trainingRmse: Float,
    val validationMaxError: Float,
)

object CompositeMath {
    private const val MIN_SLOPE = 0.02f
    private const val MAX_SLOPE = 1.003f
    private const val MAX_SLOPE_SPREAD = 0.035f
    private const val PREMULTIPLIED_TOLERANCE = 0.012f

    fun fit(
        backgroundR: IntArray,
        backgroundG: IntArray,
        backgroundB: IntArray,
        observedR: IntArray,
        observedG: IntArray,
        observedB: IntArray,
        trainingIndices: IntArray,
        validationIndices: IntArray,
        blendSpace: BlendSpace,
    ): CompositeFit? {
        val sampleCount = backgroundR.size
        require(sampleCount >= 4)
        require(backgroundG.size == sampleCount && backgroundB.size == sampleCount)
        require(observedR.size == sampleCount && observedG.size == sampleCount && observedB.size == sampleCount)
        require(trainingIndices.size >= 2 && validationIndices.isNotEmpty())
        require(trainingIndices.all { it in 0 until sampleCount })
        require(validationIndices.all { it in 0 until sampleCount })

        val r = fitChannel(backgroundR, observedR, trainingIndices, blendSpace) ?: return null
        val g = fitChannel(backgroundG, observedG, trainingIndices, blendSpace) ?: return null
        val b = fitChannel(backgroundB, observedB, trainingIndices, blendSpace) ?: return null

        var trainingSquaredError = 0.0
        var validationMaxError = 0.0
        for (index in trainingIndices) {
            val er = predictCode(backgroundR[index], r.slope, r.intercept, blendSpace) - observedR[index]
            val eg = predictCode(backgroundG[index], g.slope, g.intercept, blendSpace) - observedG[index]
            val eb = predictCode(backgroundB[index], b.slope, b.intercept, blendSpace) - observedB[index]
            trainingSquaredError += er * er + eg * eg + eb * eb
        }
        for (index in validationIndices) {
            validationMaxError = max(
                validationMaxError,
                abs(predictCode(backgroundR[index], r.slope, r.intercept, blendSpace) - observedR[index]).toDouble(),
            )
            validationMaxError = max(
                validationMaxError,
                abs(predictCode(backgroundG[index], g.slope, g.intercept, blendSpace) - observedG[index]).toDouble(),
            )
            validationMaxError = max(
                validationMaxError,
                abs(predictCode(backgroundB[index], b.slope, b.intercept, blendSpace) - observedB[index]).toDouble(),
            )
        }
        val trainingRmse = sqrt(trainingSquaredError / (trainingIndices.size * 3.0)).toFloat()
        return CompositeFit(
            blendSpace = blendSpace,
            slopeR = r.slope,
            slopeG = g.slope,
            slopeB = b.slope,
            interceptR = r.intercept,
            interceptG = g.intercept,
            interceptB = b.intercept,
            trainingRmse = trainingRmse,
            validationMaxError = validationMaxError.toFloat(),
        )
    }

    fun isPhysicalSourceOver(fit: CompositeFit): Boolean {
        val minSlope = min(fit.slopeR, min(fit.slopeG, fit.slopeB))
        val maxSlope = max(fit.slopeR, max(fit.slopeG, fit.slopeB))
        if (!minSlope.isFinite() || !maxSlope.isFinite()) return false
        if (minSlope < MIN_SLOPE || maxSlope > MAX_SLOPE || maxSlope - minSlope > MAX_SLOPE_SPREAD) return false
        return channelIsPhysical(fit.slopeR, fit.interceptR) &&
            channelIsPhysical(fit.slopeG, fit.interceptG) &&
            channelIsPhysical(fit.slopeB, fit.interceptB)
    }

    fun predictCode(input: Int, slope: Float, intercept: Float, blendSpace: BlendSpace): Float {
        if (!slope.isFinite() || !intercept.isFinite()) return input.toFloat()
        val domainInput = toDomain(input, blendSpace)
        return fromDomain(slope * domainInput + intercept, blendSpace)
    }

    fun invert(observed: Int, slope: Float, intercept: Float, blendSpace: BlendSpace): Int {
        if (!slope.isFinite() || !intercept.isFinite() || slope <= 0f) return observed
        val sourceDomain = ((toDomain(observed, blendSpace) - intercept) / slope).coerceIn(0f, 1f)
        val estimate = fromDomain(sourceDomain, blendSpace).coerceIn(0f, 255f)
        val center = estimate.roundToInt().coerceIn(0, 255)
        val low = (center - 3).coerceAtLeast(0)
        val high = (center + 3).coerceAtMost(255)
        var best = center
        var bestError = Float.POSITIVE_INFINITY
        var bestDistance = Float.POSITIVE_INFINITY
        for (candidate in low..high) {
            val error = abs(predictCode(candidate, slope, intercept, blendSpace) - observed)
            val distance = abs(candidate - estimate)
            if (error < bestError - 1e-6f || (abs(error - bestError) <= 1e-6f && distance < bestDistance)) {
                best = candidate
                bestError = error
                bestDistance = distance
            }
        }
        return best
    }

    private fun fitChannel(
        background: IntArray,
        observed: IntArray,
        trainingIndices: IntArray,
        blendSpace: BlendSpace,
    ): ChannelFit? {
        val n = trainingIndices.size.toDouble()
        var sx = 0.0
        var sy = 0.0
        var sxx = 0.0
        var sxy = 0.0
        for (index in trainingIndices) {
            val x = toDomain(background[index], blendSpace).toDouble()
            val y = toDomain(observed[index], blendSpace).toDouble()
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

    private fun channelIsPhysical(slope: Float, intercept: Float): Boolean {
        if (!slope.isFinite() || !intercept.isFinite()) return false
        val alpha = 1f - slope
        if (alpha < -0.003f) return false
        val upper = max(0f, alpha) + PREMULTIPLIED_TOLERANCE
        return intercept >= -PREMULTIPLIED_TOLERANCE && intercept <= upper
    }

    private fun toDomain(code: Int, blendSpace: BlendSpace): Float {
        val encoded = code.coerceIn(0, 255) / 255f
        return when (blendSpace) {
            BlendSpace.ENCODED_SRGB -> encoded
            BlendSpace.LINEAR_SRGB -> if (encoded <= 0.04045f) {
                encoded / 12.92f
            } else {
                (((encoded + 0.055f) / 1.055f).toDouble().pow(2.4)).toFloat()
            }
        }
    }

    private fun fromDomain(value: Float, blendSpace: BlendSpace): Float {
        val normalized = value.coerceIn(0f, 1f)
        val encoded = when (blendSpace) {
            BlendSpace.ENCODED_SRGB -> normalized
            BlendSpace.LINEAR_SRGB -> if (normalized <= 0.0031308f) {
                normalized * 12.92f
            } else {
                (1.055 * normalized.toDouble().pow(1.0 / 2.4) - 0.055).toFloat()
            }
        }
        return encoded * 255f
    }

    private data class ChannelFit(val slope: Float, val intercept: Float)
}
