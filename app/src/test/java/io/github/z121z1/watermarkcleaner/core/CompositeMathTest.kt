package io.github.z121z1.watermarkcleaner.core

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
    fun encodedSourceOver_recoversAospDefaultShadowTextAndOverlap() {
        // AOSP Watermark defaults: text=0x60ffffff, shadow=0xb0000000.
        val shadowAlpha = 0xb0 / 255f
        val textAlpha = 0x60 / 255f

        val blackShadow = encodedComposite(baseline, alpha = shadowAlpha, overlay = 0f)
        val shadowFit = fitEncoded(blackShadow)
        assertTrue(CompositeMath.isPhysicalSourceOver(shadowFit))
        assertTrue(shadowFit.validationMaxError <= 1f)

        val whiteText = encodedComposite(baseline, alpha = textAlpha, overlay = 255f)
        val textFit = fitEncoded(whiteText)
        assertTrue(CompositeMath.isPhysicalSourceOver(textFit))
        assertTrue(textFit.validationMaxError <= 1f)

        // A fixed shadow followed by fixed text is still exactly representable as one source-over
        // composite per pixel. This covers the glyph/shadow overlap region, not only their edges.
        val overlap = encodedComposite(
            encodedComposite(baseline, alpha = shadowAlpha, overlay = 0f),
            alpha = textAlpha,
            overlay = 255f,
        )
        val overlapFit = fitEncoded(overlap)
        assertTrue(CompositeMath.isPhysicalSourceOver(overlapFit))
        assertTrue(overlapFit.validationMaxError <= 1f)
    }

    @Test
    fun inversion_isWithinOneCodeValueForQuantizedSourceOver() {
        val observations = encodedComposite(baseline, alpha = .24f, overlay = 0f)
        val fit = fitEncoded(observations)
        for (input in 0..255) {
            val observed = ((1f - .24f) * input).roundToInt().coerceIn(0, 255)
            val restored = CompositeMath.invert(observed, fit.slopeR, fit.interceptR, fit.blendSpace)
            assertTrue("input=$input observed=$observed restored=$restored", abs(restored - input) <= 1)
        }
    }

    @Test
    fun linearSourceOver_isDetectedByIndependentHoldoutLevels() {
        val observed = linearComposite(baseline, alpha = .18, overlayLinear = 1.0)
        val encodedFit = CompositeMath.fit(
            baseline, baseline, baseline,
            observed, observed, observed,
            training, validation, BlendSpace.ENCODED_SRGB,
        )!!
        val linearFit = CompositeMath.fit(
            baseline, baseline, baseline,
            observed, observed, observed,
            training, validation, BlendSpace.LINEAR_SRGB,
        )!!
        assertTrue(CompositeMath.isPhysicalSourceOver(linearFit))
        assertTrue(linearFit.validationMaxError <= 1f)
        assertTrue(linearFit.validationMaxError < encodedFit.validationMaxError)
    }

    @Test
    fun fitUsesObservedScreenshotBaseline_notNominalRequestedGray() {
        val shiftedBaseline = intArrayOf(1, 31, 63, 127, 191, 253)
        val observed = encodedComposite(shiftedBaseline, alpha = .11f, overlay = 255f)
        val fit = CompositeMath.fit(
            shiftedBaseline, shiftedBaseline, shiftedBaseline,
            observed, observed, observed,
            training, validation, BlendSpace.ENCODED_SRGB,
        )!!
        assertTrue(CompositeMath.isPhysicalSourceOver(fit))
        assertTrue(fit.validationMaxError <= 1f)
    }

    private fun fitEncoded(observed: IntArray): CompositeFit = CompositeMath.fit(
        baseline, baseline, baseline,
        observed, observed, observed,
        training, validation, BlendSpace.ENCODED_SRGB,
    )!!

    private fun encodedComposite(input: IntArray, alpha: Float, overlay: Float): IntArray =
        IntArray(input.size) { index ->
            ((1f - alpha) * input[index] + alpha * overlay).roundToInt().coerceIn(0, 255)
        }

    private fun linearComposite(input: IntArray, alpha: Double, overlayLinear: Double): IntArray =
        IntArray(input.size) { index ->
            val out = (1.0 - alpha) * srgbToLinear(input[index]) + alpha * overlayLinear
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
