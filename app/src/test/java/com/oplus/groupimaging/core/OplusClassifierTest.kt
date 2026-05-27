package com.oplus.groupimaging.core

import com.oplus.groupimaging.domain.ParseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OplusClassifierTest {
    private val classifier = OplusClassifier(defaultDeviceProfiles())

    @Test
    fun `classify keeps device photo with required exif`() {
        val asset = classifier.classify(
            candidate = mediaCandidate(),
            metadata = ExtractedMetadata(
                make = "OPPO",
                model = "OPPO Find X8 Ultra",
                focalLengthMm = 6.3,
                focalLengthEq = 23,
                userComment = "oplus_256",
                hasRequiredExif = true,
                capturedAt = 1_000L,
            ),
            parseVersion = 3,
        )

        assertEquals(ParseStatus.PARSED, asset.parseStatus)
        assertTrue(asset.isOplusOriginal)
    }

    @Test
    fun `classify skips photo without required exif`() {
        val asset = classifier.classify(
            candidate = mediaCandidate(),
            metadata = ExtractedMetadata(
                make = "OPPO",
                model = "OPPO Find X8 Ultra",
                focalLengthMm = 6.3,
                focalLengthEq = null,
                userComment = null,
                hasRequiredExif = false,
                capturedAt = 1_000L,
            ),
            parseVersion = 3,
        )

        assertEquals(ParseStatus.SKIPPED, asset.parseStatus)
        assertFalse(asset.isOplusOriginal)
    }

    @Test
    fun `classify skips non device photo even under dcim`() {
        val asset = classifier.classify(
            candidate = mediaCandidate(),
            metadata = ExtractedMetadata(
                make = "Canon",
                model = "EOS R5",
                focalLengthMm = 6.3,
                focalLengthEq = 23,
                userComment = "canon_custom",
                hasRequiredExif = true,
                capturedAt = 1_000L,
            ),
            parseVersion = 3,
        )

        assertEquals(ParseStatus.SKIPPED, asset.parseStatus)
        assertFalse(asset.isOplusOriginal)
    }
}

private fun mediaCandidate() = MediaCandidate(
    id = 1L,
    uri = "content://media/1",
    displayName = "IMG_0001.JPG",
    mimeType = "image/jpeg",
    size = 128L,
    relativePath = "DCIM/Camera/",
    absolutePath = "/storage/emulated/0/DCIM/Camera/IMG_0001.JPG",
    createdAt = 1_000L,
    modifiedAt = 2_000L,
)
