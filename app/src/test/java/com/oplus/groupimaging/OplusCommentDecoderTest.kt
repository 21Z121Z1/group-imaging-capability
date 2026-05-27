package com.oplus.groupimaging

import com.oplus.groupimaging.core.OplusCommentDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OplusCommentDecoderTest {
    @Test
    fun professionalMapsToMasterLabel() {
        val decoded = OplusCommentDecoder.decode("Oplus_256")
        assertEquals("大师", decoded?.captureModeLabel)
    }

    @Test
    fun livePhotoFlagIsDetected() {
        val decoded = OplusCommentDecoder.decode("Oplus_8388609")
        assertTrue(decoded?.livePhotoFlag == true)
    }

    @Test
    fun exifFlagsAreNamed() {
        val decoded = OplusCommentDecoder.decode("Oplus_3146016")
        assertTrue(decoded?.namedExifFlags?.contains("EXIF_TAG_ULTRA_HDR") == true)
    }
}
