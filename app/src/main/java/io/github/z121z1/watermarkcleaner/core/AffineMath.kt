package io.github.z121z1.watermarkcleaner.core

import kotlin.math.max
import kotlin.math.sqrt

data class AffineFit(val slope: Float, val intercept: Float, val rmse: Float)

object AffineMath {
    fun fit(x: FloatArray, y: FloatArray): AffineFit {
        require(x.size == y.size && x.size >= 2)
        val n = x.size.toDouble()
        var sx = 0.0
        var sy = 0.0
        var sxx = 0.0
        var sxy = 0.0
        for (i in x.indices) {
            val xd = x[i].toDouble()
            val yd = y[i].toDouble()
            sx += xd
            sy += yd
            sxx += xd * xd
            sxy += xd * yd
        }
        val denominator = n * sxx - sx * sx
        require(kotlin.math.abs(denominator) > 1e-12)
        val slope = (n * sxy - sx * sy) / denominator
        val intercept = (sy - slope * sx) / n
        var squaredError = 0.0
        for (i in x.indices) {
            val e = y[i] - (slope * x[i] + intercept)
            squaredError += e * e
        }
        return AffineFit(slope.toFloat(), intercept.toFloat(), sqrt(squaredError / n).toFloat())
    }

    fun invert(observed: Int, slope: Float, intercept: Float): Int {
        if (!slope.isFinite() || !intercept.isFinite() || slope <= 0.0f) return observed
        return (((observed - intercept) / slope).coerceIn(0f, 255f) + 0.5f).toInt()
    }

    fun inferredOverlayColor(slope: Float, intercept: Float): Float? {
        val alpha = 1f - slope
        if (alpha <= 0.002f) return null
        return intercept / max(alpha, 1e-6f)
    }
}
