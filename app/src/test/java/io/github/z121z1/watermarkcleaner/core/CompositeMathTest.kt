package io.github.z121z1.watermarkcleaner.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

class CompositeMathTest {
    private val training = intArrayOf(0, 1, 3, 5)
    private val validation = intArrayOf(2, 4)
    private val baseline = intArrayOf(0, 32, 64, 128, 192, 255)

    @Test
    fun oplusEncodedSurface_allRasterizedAlphaBytesPassExactValidator() {
        // WatermarkExtImpl sets textColor=0x0FB8B8B8 and shadowRadius=0. Glyph AA can only
        // lower the effective alpha. Include premultiplied-surface quantization before screenshot.
        for (alphaByte in 1..CompositeMath.OPLUS_BETA_TEXT_ALPHA_BYTE) {
            val observed = premultipliedOplusSurface(baseline, alphaByte)
            val fit = fitNeutral(observed, BlendSpace.ENCODED_SRGB)
            assertTrue("alphaByte=$alphaByte fit=$fit", CompositeMath.isOplusBetaWatermark(fit))
            assertTrue("alphaByte=$alphaByte fit=$fit", fit.trainingRmse <= .55f)
            assertTrue("alphaByte=$alphaByte fit=$fit", fit.validationMaxError <= 1.10f)
        }
    }

    @Test
    fun oplusValidator_rejectsWhiteUiEdgeAndTooOpaqueGrayOverlay() {
        val whiteUi = encodedComposite(baseline, alpha = .04f, overlay = 255f)
        assertFalse(CompositeMath.isOplusBetaWatermark(fitNeutral(whiteUi, BlendSpace.ENCODED_SRGB)))

        val tooOpaqueGray = encodedComposite(
            baseline,
            alpha = .15f,
            overlay = CompositeMath.OPLUS_BETA_TEXT_CODE.toFloat(),
        )
        assertFalse(CompositeMath.isOplusBetaWatermark(fitNeutral(tooOpaqueGray, BlendSpace.ENCODED_SRGB)))
    }

    @Test
    fun inversion_isWithinOneCodeValueForQuantizedOplusSurface() {
        val observations = premultipliedOplusSurface(baseline, CompositeMath.OPLUS_BETA_TEXT_ALPHA_BYTE)
        val fit = fitNeutral(observations, BlendSpace.ENCODED_SRGB)
        val alpha = CompositeMath.OPLUS_BETA_TEXT_ALPHA
        val premultiplied = (
            CompositeMath.OPLUS_BETA_TEXT_ALPHA_BYTE * CompositeMath.OPLUS_BETA_TEXT_CODE / 255f
        ).roundToInt()

        for (input in 0..255) {
            val observed = (premultiplied + input * (1f - alpha)).roundToInt().coerceIn(0, 255)
            val restored = CompositeMath.invert(observed, fit.slopeR, fit.interceptR, fit.blendSpace)
            assertTrue("input=$input observed=$observed restored=$restored", abs(restored - input) <= 1)
        }
    }

    @Test
    fun linearOplusSourceOver_isDetectedByIndependentHoldoutLevels() {
        val observed = linearComposite(
            baseline,
            alpha = CompositeMath.OPLUS_BETA_TEXT_ALPHA.toDouble(),
            overlayCode = CompositeMath.OPLUS_BETA_TEXT_CODE,
        )
        val encodedFit = fitNeutral(observed, BlendSpace.ENCODED_SRGB)
        val linearFit = fitNeutral(observed, BlendSpace.LINEAR_SRGB)
        assertTrue(CompositeMath.isOplusBetaWatermark(linearFit))
        assertTrue(linearFit.validationMaxError <= 1.10f)
        assertTrue(linearFit.validationMaxError < encodedFit.validationMaxError)
    }

    @Test
    fun fitUsesObservedScreenshotBaseline_notNominalRequestedGray() {
        val shiftedBaseline = intArrayOf(1, 31, 63, 127, 191, 253)
        val observed = encodedComposite(
            shiftedBaseline,
            alpha = .04f,
            overlay = CompositeMath.OPLUS_BETA_TEXT_CODE.toFloat(),
        )
        val fit = CompositeMath.fitNeutralOverlay(
            shiftedBaseline, shiftedBaseline, shiftedBaseline,
            observed, observed, observed,
            training, validation, BlendSpace.ENCODED_SRGB,
        )!!
        assertTrue(CompositeMath.isOplusBetaWatermark(fit))
        assertTrue(fit.validationMaxError <= 1.10f)
    }

    private fun fitNeutral(observed: IntArray, space: BlendSpace): CompositeFit = CompositeMath.fitNeutralOverlay(
        baseline, baseline, baseline,
        observed, observed, observed,
        training, validation, space,
    )!!

    private fun premultipliedOplusSurface(input: IntArray, alphaByte: Int): IntArray {
        val alpha = alphaByte / 255f
        val premultiplied = (alphaByte * CompositeMath.OPLUS_BETA_TEXT_CODE / 255f).roundToInt()
        return IntArray(input.size) { index ->
            (premultiplied + input[index] * (1f - alpha)).roundToInt().coerceIn(0, 255)
        }
    }

    private fun encodedComposite(input: IntArray, alpha: Float, overlay: Float): IntArray =
        IntArray(input.size) { index ->
            ((1f - alpha) * input[index] + alpha * overlay).roundToInt().coerceIn(0, 255)
        }

    private fun linearComposite(input: IntArray, alpha: Double, overlayCode: Int): IntArray =
        IntArray(input.size) { index ->
            val out = (1.0 - alpha) * srgbToLinear(input[index]) + alpha * srgbToLinear(overlayCode)
            linearToSrgb(out)
        }

    private fun srgbToLinear(code: Int): Double {
        val encoded = code / 255.0
        return if (encoded <= 0.04045) encoded / 12.92 else ((encoded + 0.055) / 1.055).pow(2.4)
    }

    private fun linearToSrgb(value: Double): Int {
        val linear = value.coerceIn(0.0, 1.0)
        val encoded = if (linear <= 0.0031308) linear * 12.92 else 1.055 * linear.pow(1.0 / 2.4) - 0.055
        return (encoded * 255.0).roundToInt().coerceIn(0, 255)
    }
}
