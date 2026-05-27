package com.oplus.groupimaging.data

import com.oplus.groupimaging.core.ScanCursorPosition
import com.oplus.groupimaging.domain.ScanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupImagingRepositoryHelpersTest {
    @Test
    fun `incremental falls back to full on first scan`() {
        val execution = determineScanExecution(
            requestedType = ScanType.INCREMENTAL,
            scopeRootsJson = "[\"DCIM/Camera\"]",
            parseVersion = 3,
            cursorRootsJson = null,
            cursorParseVersion = null,
            activeJobId = null,
            resumePosition = null,
            lastScannedPosition = null,
        )

        assertEquals(ScanType.FULL, execution.effectiveType)
        assertNull(execution.startAfter)
        assertEquals("first_scan", execution.reason)
    }

    @Test
    fun `incremental resumes from last scanned position`() {
        val position = ScanCursorPosition(modifiedAt = 123_000L, mediaStoreId = 9L)
        val execution = determineScanExecution(
            requestedType = ScanType.INCREMENTAL,
            scopeRootsJson = "[\"DCIM/Camera\"]",
            parseVersion = 3,
            cursorRootsJson = "[\"DCIM/Camera\"]",
            cursorParseVersion = 3,
            activeJobId = null,
            resumePosition = null,
            lastScannedPosition = position,
        )

        assertEquals(ScanType.INCREMENTAL, execution.effectiveType)
        assertEquals(position, execution.startAfter)
        assertEquals("incremental", execution.reason)
    }

    @Test
    fun `incremental uses resume cursor for interrupted scan`() {
        val resume = ScanCursorPosition(modifiedAt = 456_000L, mediaStoreId = 18L)
        val execution = determineScanExecution(
            requestedType = ScanType.INCREMENTAL,
            scopeRootsJson = "[\"DCIM/Camera\"]",
            parseVersion = 3,
            cursorRootsJson = "[\"DCIM/Camera\"]",
            cursorParseVersion = 3,
            activeJobId = "job-1",
            resumePosition = resume,
            lastScannedPosition = ScanCursorPosition(modifiedAt = 123_000L, mediaStoreId = 9L),
        )

        assertEquals(ScanType.INCREMENTAL, execution.effectiveType)
        assertEquals(resume, execution.startAfter)
        assertEquals("resume", execution.reason)
    }

    @Test
    fun `incremental does not rescan skipped tail when last scanned is newer than last parsed`() {
        val skippedTail = ScanCursorPosition(modifiedAt = 456_000L, mediaStoreId = 18L)
        val execution = determineScanExecution(
            requestedType = ScanType.INCREMENTAL,
            scopeRootsJson = "[\"DCIM/Camera\"]",
            parseVersion = 3,
            cursorRootsJson = "[\"DCIM/Camera\"]",
            cursorParseVersion = 3,
            activeJobId = null,
            resumePosition = null,
            lastScannedPosition = skippedTail,
        )

        assertEquals(ScanType.INCREMENTAL, execution.effectiveType)
        assertEquals(skippedTail, execution.startAfter)
        assertEquals("incremental", execution.reason)
    }

    @Test
    fun `move target keeps absolute and relative paths aligned`() {
        val target = resolveMoveTarget("/storage/emulated/0/DCIM/myalbums")

        assertEquals("/storage/emulated/0/DCIM/myalbums/", target.absolutePath)
        assertEquals("DCIM/myalbums/", target.relativePath)
    }

    @Test
    fun `rule move target creates rule album under myalbums`() {
        val target = resolveRuleMoveTarget("/storage/emulated/0/DCIM/myalbums/", "RAW")

        assertEquals("/storage/emulated/0/DCIM/myalbums/RAW/", target.absolutePath)
        assertEquals("DCIM/myalbums/RAW/", target.relativePath)
    }

    @Test
    fun `rule move target sanitizes nested rule names`() {
        val target = resolveRuleMoveTarget("/storage/emulated/0/DCIM/myalbums/", "主摄/长焦")

        assertEquals("/storage/emulated/0/DCIM/myalbums/主摄-长焦/", target.absolutePath)
        assertEquals("DCIM/myalbums/主摄-长焦/", target.relativePath)
    }

    @Test
    fun `write request uri chunks stay below platform prompt limit`() {
        val uris = (1..2_001).map { "content://media/external/images/media/$it" }

        val chunks = chunkWriteRequestUris(uris)

        assertEquals(3, chunks.size)
        assertEquals(1_000, chunks[0].size)
        assertEquals(1_000, chunks[1].size)
        assertEquals(1, chunks[2].size)
        assertEquals("content://media/external/images/media/1", chunks.first().first())
    }

    @Test
    fun `legacy files uri is normalized to image media uri for write requests`() {
        assertEquals(
            "content://media/external/images/media/42",
            mediaWriteUriString("content://media/external/file/42"),
        )
    }
}
