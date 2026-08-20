package dev.groupimaging.unmark.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class AffineMathTest {
    @Test
    fun fitRecoversKnownCompositor() {
        val slope = 0.942f
        val intercept = 10.8f
        val observed = AffineMath.calibrationLevels.map { (slope * it + intercept).roundToInt() }.toIntArray()

        val fit = AffineMath.fit(observed)

        assertEquals(slope.toDouble(), fit.slope.toDouble(), 0.004)
        assertEquals(intercept.toDouble(), fit.intercept.toDouble(), 0.6)
        assertTrue(fit.rmse < 0.6f)
        assertTrue(AffineMath.shouldKeep(fit, fit, fit))
    }

    @Test
    fun flatIdentityIsNotStoredAsWatermark() {
        val observed = AffineMath.calibrationLevels.copyOf()
        val fit = AffineMath.fit(observed)
        assertFalse(AffineMath.shouldKeep(fit, fit, fit))
    }

    @Test
    fun inverseRoundTripStaysWithinOneCodeValue() {
        val slope = 0.942f
        val intercept = 10.8f
        for (source in 0..255) {
            val observed = (slope * source + intercept).roundToInt().coerceIn(0, 255)
            val recovered = AffineMath.inverse(observed, slope, intercept)
            // Saturation at the extreme ends is not invertible; the calibrated watermark never
            // reaches those clipped cases in the useful range.
            if (observed in 1..254) {
                assertTrue("source=$source observed=$observed recovered=$recovered", kotlin.math.abs(recovered - source) <= 1)
            }
        }
    }
}
