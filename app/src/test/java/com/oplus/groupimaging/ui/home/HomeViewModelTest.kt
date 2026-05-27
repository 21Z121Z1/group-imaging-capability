package com.oplus.groupimaging.ui.home

import com.oplus.groupimaging.MainDispatcherRule
import com.oplus.groupimaging.TestRepository
import com.oplus.groupimaging.domain.LensClass
import com.oplus.groupimaging.domain.usecase.ObserveHomeSummary
import com.oplus.groupimaging.domain.usecase.ObserveScanProgress
import com.oplus.groupimaging.testing.InsightTestFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads indexed summary and latest scan`() = runTest {
        val repository = TestRepository().apply {
            homeSummary = InsightTestFixtures.indexedHomeSummary()
            latestScanJobValue = InsightTestFixtures.completedScanJob()
        }
        val viewModel = HomeViewModel(
            observeHomeSummary = ObserveHomeSummary(repository),
            observeScanProgress = ObserveScanProgress(repository),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(true, viewModel.uiState.value.hasIndex)
        assertEquals(12_004, viewModel.uiState.value.summary?.totalCaptures)
        assertEquals("COMPLETED", viewModel.uiState.value.scanStatus?.status?.name)
    }

    @Test
    fun `load failure exposes error state`() = runTest {
        val repository = TestRepository().apply {
            homeSummaryFailure = IllegalStateException("home summary unavailable")
        }
        val viewModel = HomeViewModel(
            observeHomeSummary = ObserveHomeSummary(repository),
            observeScanProgress = ObserveScanProgress(repository),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("home summary unavailable", viewModel.uiState.value.error)
    }

    @Test
    fun `lens click emits insight navigation with seed`() = runTest {
        val repository = TestRepository()
        val viewModel = HomeViewModel(
            observeHomeSummary = ObserveHomeSummary(repository),
            observeScanProgress = ObserveScanProgress(repository),
        )
        advanceUntilIdle()

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onAction(HomeAction.OnLensClick(LensClass.TELE))

        assertEquals(
            HomeEffect.NavigateToInsightWithFilter(
                com.oplus.groupimaging.navigation.InsightFilterSeed(lenses = setOf(LensClass.TELE)),
            ),
            effectDeferred.await(),
        )
    }
}
