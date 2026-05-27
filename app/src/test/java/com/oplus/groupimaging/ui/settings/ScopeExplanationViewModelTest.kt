package com.oplus.groupimaging.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopeExplanationViewModelTest {
    @Test
    fun `default scope explanation state exposes expected summary rows`() {
        val viewModel = ScopeExplanationViewModel()
        val state = viewModel.uiState.value

        assertEquals("索引与筛选口径", state.title)
        assertEquals("统计、RAW、Live 与授权范围", state.subtitle)
        assertTrue(state.summaryItems.any { it.first == "照片统计" && it.second.contains("CaptureSession") })
        assertTrue(state.summaryItems.any { it.first == "授权范围" && it.second.contains("Selected Photos") })
    }
}
