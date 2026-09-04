package io.github.z121z1.watermarkcleaner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GainMapMathTest {
    private val training = intArrayOf(0, 1, 3, 5)
    private val validation = intArrayOf(2, 4)

    @Test
    fun logGainRoundTrip_isWithinOneCodeValue() {
        val metadata = GainChannelMetadata(ratioMin = 0.75f, ratioMax = 8f, gamma = 1.35f)
        for (code in 0..255) {
            val restored = GainMapMath.fromLogGain(GainMapMath.toLogGain(code, metadata), metadata)
            assertTrue("code=$code restored=$restored", abs(restored - code) <= 1)
        }
    }

    @Test
    fun fitNormalizesDifferentGainMetadataAcrossSamples() {
        val metadata = arrayOf(
            GainChannelMetadata(1f, 4f, 1f),
            GainChannelMetadata(1f, 8f, 1.2f),
            GainChannelMetadata(0.8f, 6f, 0.9f),
            GainChannelMetadata(1f, 16f, 1.4f),
            GainChannelMetadata(0.7f, 8f, 1.1f),
            GainChannelMetadata(1f, 10f, 1f),
        )
        val cleanCodes = intArrayOf(32, 64, 96, 144, 192, 240)
        val background = FloatArray(cleanCodes.size) { i -> GainMapMath.toLogGain(cleanCodes[i], metadata[i]) }
        val slope = 0.82f
        val intercept = -0.055f
        val observed = FloatArray(background.size) { i -> slope * background[i] + intercept }
        val fit = GainMapMath.fit(
            background, background, background,
            observed, observed, observed,
            training, validation,
        )!!
        assertEquals(slope, fit.slopeR, 1e-5f)
        assertEquals(intercept, fit.interceptR, 1e-5f)
        assertTrue(fit.validationMaxError < 1e-5f)

        for (i in observed.indices) {
            val recovered = GainMapMath.invertLogGain(observed[i], fit.slopeR, fit.interceptR)
            assertEquals(background[i], recovered, 1e-5f)
        }
    }

    @Test
    fun reconstructedHdr_matchesDocumentedSdrBaseFormula() {
        val hdr = GainMapMath.reconstructedHdr(
            baseLinear = 0.25f,
            logGain = kotlin.math.ln(4.0).toFloat(),
            epsilonSdr = 0f,
            epsilonHdr = 0f,
        )
        assertEquals(1f, hdr, 1e-5f)
    }
}
