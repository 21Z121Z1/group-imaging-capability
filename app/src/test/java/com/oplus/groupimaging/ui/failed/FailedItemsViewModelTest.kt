package com.oplus.groupimaging.ui.failed

import com.oplus.groupimaging.MainDispatcherRule
import com.oplus.groupimaging.TestRepository
import com.oplus.groupimaging.domain.usecase.ObserveFailedItems
import com.oplus.groupimaging.testing.InsightTestFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FailedItemsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads failed items`() = runTest {
        val repository = TestRepository().apply {
            failedItems = InsightTestFixtures.failedItems()
        }

        val viewModel = FailedItemsViewModel(
            observeFailedItems = ObserveFailedItems(repository),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.items.size)
        assertEquals("IMG_NO_EXIF.jpg", viewModel.uiState.value.items.first().title)
    }

    @Test
    fun `retry recovers after initial load failure`() = runTest {
        val repository = TestRepository().apply {
            failedItemsFailure = IllegalStateException("failed item ledger missing")
        }

        val viewModel = FailedItemsViewModel(
            observeFailedItems = ObserveFailedItems(repository),
        )

        advanceUntilIdle()
        assertEquals("failed item ledger missing", viewModel.uiState.value.error)

        repository.failedItemsFailure = null
        repository.failedItems = InsightTestFixtures.failedItems()
        viewModel.onAction(FailedItemsAction.OnRetry)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.error)
        assertEquals(2, viewModel.uiState.value.items.size)
    }
}
