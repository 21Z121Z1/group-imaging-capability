package io.github.z121z1.watermarkcleaner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationTargetTest {
    @Test
    fun orientationIsDerivedFromExactScreenshotDimensions() {
        assertEquals(CalibrationOrientation.PORTRAIT, CalibrationOrientation.fromDimensions(1440, 3168))
        assertEquals(CalibrationOrientation.LANDSCAPE, CalibrationOrientation.fromDimensions(3168, 1440))
        assertTrue(CalibrationOrientation.PORTRAIT.matches(1440, 3168))
        assertFalse(CalibrationOrientation.PORTRAIT.matches(3168, 1440))
    }

    @Test(expected = IllegalArgumentException::class)
    fun squareCalibrationIsRejected() {
        CalibrationOrientation.fromDimensions(1440, 1440)
    }
}
