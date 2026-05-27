package com.oplus.groupimaging.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanDirectoryConfigTest {
    @Test
    fun `normalizes relative and absolute extra directory paths`() {
        assertEquals("DCIM/WeChat/", normalizeExtraScanDirectory("DCIM/WeChat"))
        assertEquals("DCIM/WeChat/", normalizeExtraScanDirectory("DCIM/WeChat/"))
        assertEquals(
            "DCIM/WeChat/",
            normalizeExtraScanDirectory("/storage/emulated/0/DCIM/WeChat"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects default directories`() {
        normalizeExtraScanDirectory("DCIM/Camera")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects duplicate directories`() {
        normalizeExtraScanDirectories(listOf("DCIM/WeChat", "DCIM/WeChat/"))
    }
}
