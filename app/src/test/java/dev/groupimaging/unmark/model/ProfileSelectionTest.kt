package dev.groupimaging.unmark.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSelectionTest {
    @Test
    fun affineInverseRecoversRepresentativeHdrPrimaryValues() {
        val slope = 0.943f
        val intercept = 10.7f
        for (source in intArrayOf(32, 67, 90, 128, 154, 192, 240)) {
            val observed = (slope * source + intercept).toInt().coerceIn(0, 255)
            val recovered = AffineMath.inverse(observed, slope, intercept)
            assertTrue("source=$source recovered=$recovered", kotlin.math.abs(recovered - source) <= 1)
        }
    }

    @Test
    fun residualProfileActsAsAdditiveSubtraction() {
        val profile = WatermarkProfile(
            width = 2,
            height = 1,
            indices = intArrayOf(1),
            slopeR = floatArrayOf(1f),
            slopeG = floatArrayOf(1f),
            slopeB = floatArrayOf(1f),
            interceptR = floatArrayOf(4f),
            interceptG = floatArrayOf(3f),
            interceptB = floatArrayOf(2f),
        )
        assertEquals(96, AffineMath.inverse(100, profile.slopeR[0], profile.interceptR[0]))
        assertEquals(97, AffineMath.inverse(100, profile.slopeG[0], profile.interceptG[0]))
        assertEquals(98, AffineMath.inverse(100, profile.slopeB[0], profile.interceptB[0]))
        assertTrue(profile.matches(2, 1))
        assertFalse(profile.matches(1, 2))
    }
}
