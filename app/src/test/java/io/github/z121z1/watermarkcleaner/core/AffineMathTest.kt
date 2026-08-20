package io.github.z121z1.watermarkcleaner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AffineMathTest {
    @Test
    fun fit_recoversKnownAffineModel() {
        val x = floatArrayOf(0f, 32f, 64f, 128f, 192f, 255f)
        val y = FloatArray(x.size) { i -> 0.9435f * x[i] + 10.55f }
        val fit = AffineMath.fit(x, y)
        assertEquals(0.9435f, fit.slope, 1e-4f)
        assertEquals(10.55f, fit.intercept, 1e-3f)
        assertTrue(fit.rmse < 1e-3f)
    }

    @Test
    fun invert_recoversAllEightBitInputsWithinOneCodeValue() {
        val slope = 0.9435f
        val intercept = 10.55f
        for (x in 0..255) {
            val observed = (slope * x + intercept).coerceIn(0f, 255f).toInt()
            val recovered = AffineMath.invert(observed, slope, intercept)
            if (observed < 255) {
                assertTrue("x=$x recovered=$recovered", kotlin.math.abs(recovered - x) <= 2)
            }
        }
    }

    @Test
    fun overlayColor_matchesCompositorIdentity() {
        val alpha = 0.0565f
        val overlay = 186.5f
        val slope = 1f - alpha
        val intercept = alpha * overlay
        assertEquals(overlay, AffineMath.inferredOverlayColor(slope, intercept)!!, 1e-3f)
    }
}
