package dev.groupimaging.unmark.model

import kotlin.math.sqrt

object AffineMath {
    val calibrationLevels: IntArray = intArrayOf(0, 32, 64, 128, 192, 255)

    data class Fit(
        val slope: Float,
        val intercept: Float,
        val rmse: Float,
    )

    fun fit(values: IntArray): Fit {
        require(values.size == calibrationLevels.size) { "Six calibration observations are required" }

        val n = calibrationLevels.size.toDouble()
        var sumX = 0.0
        var sumY = 0.0
        var sumXX = 0.0
        var sumXY = 0.0
        for (i in calibrationLevels.indices) {
            val x = calibrationLevels[i].toDouble()
            val y = values[i].toDouble()
            sumX += x
            sumY += y
            sumXX += x * x
            sumXY += x * y
        }

        val denominator = n * sumXX - sumX * sumX
        val slope = (n * sumXY - sumX * sumY) / denominator
        val intercept = (sumY - slope * sumX) / n

        var squaredError = 0.0
        for (i in calibrationLevels.indices) {
            val predicted = slope * calibrationLevels[i] + intercept
            val error = values[i] - predicted
            squaredError += error * error
        }

        return Fit(
            slope = slope.toFloat(),
            intercept = intercept.toFloat(),
            rmse = sqrt(squaredError / n).toFloat(),
        )
    }

    fun shouldKeep(red: Fit, green: Fit, blue: Fit): Boolean {
        val fits = arrayOf(red, green, blue)
        if (fits.any { it.rmse >= 0.9f || it.slope !in 0.84f..1.01f || it.intercept !in -1f..32f }) {
            return false
        }

        val meanIntercept = fits.sumOf { it.intercept.toDouble() }.toFloat() / 3f
        val meanAlpha = fits.sumOf { (1f - it.slope).toDouble() }.toFloat() / 3f
        if (meanIntercept <= 0.20f && meanAlpha <= 0.0015f) return false

        if (meanAlpha > 0.002f) {
            val inferredOverlay = meanIntercept / meanAlpha
            if (inferredOverlay !in 110f..235f) return false
        }
        return true
    }

    fun inverse(observed: Int, slope: Float, intercept: Float): Int {
        if (!slope.isFinite() || !intercept.isFinite() || slope <= 0f) return observed.coerceIn(0, 255)
        return ((observed - intercept) / slope + 0.5f).toInt().coerceIn(0, 255)
    }
}
