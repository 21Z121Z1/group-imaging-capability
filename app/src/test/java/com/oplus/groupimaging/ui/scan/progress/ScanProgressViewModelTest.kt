package com.oplus.groupimaging.ui.scan.progress

import com.oplus.groupimaging.MainDispatcherRule
import com.oplus.groupimaging.TestRepository
import com.oplus.groupimaging.domain.ScanType
import com.oplus.groupimaging.domain.usecase.ObserveScanDirectories
import com.oplus.groupimaging.domain.usecase.ObserveScanProgress
import com.oplus.groupimaging.domain.usecase.ScheduleScan
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanProgressViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `scan completes when refresh succeeds`() = runTest {
        val repository = TestRepository().apply {
            extraScanDirectories = listOf("DCIM/WeChat/", "Pictures/Screenshots/")
        }

        val viewModel = ScanProgressViewModel(
            savedStateHandle = SavedStateHandle(mapOf("scanType" to ScanType.FULL.name)),
            scheduleScan = ScheduleScan(repository),
            observeScanDirectories = ObserveScanDirectories(repository),
            observeScanProgress = ObserveScanProgress(repository),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, repository.refreshCallCount)
        assertEquals(ScanType.FULL, repository.lastRequestedScanType)
        assertEquals(setOf("DCIM/WeChat/", "Pictures/Screenshots/"), repository.lastRequestedExtraRoots)
        assertEquals(ScanStage.DONE, state.stage)
        assertTrue(state.isCompleted)
        assertEquals(state.totalCount, state.successCount)
        assertEquals(0, state.failedCount)
        assertEquals(100, state.scannedCount)
    }

    @Test
    fun `scan exposes failure state when refresh fails`() = runTest {
        val repository = TestRepository().apply {
            refreshFailure = IllegalStateException("MediaStore query failed")
        }

        val viewModel = ScanProgressViewModel(
            savedStateHandle = SavedStateHandle(mapOf("scanType" to ScanType.FULL.name)),
            scheduleScan = ScheduleScan(repository),
            observeScanDirectories = ObserveScanDirectories(repository),
            observeScanProgress = ObserveScanProgress(repository),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, repository.refreshCallCount)
        assertEquals(ScanStage.FAILED, state.stage)
        assertFalse(state.isCompleted)
        assertEquals(1, state.failedCount)
        assertEquals("MediaStore query failed", state.errorMessage)
    }

    @Test
    fun `finish action emits navigate back to home effect`() = runTest {
        val repository = TestRepository()
        val viewModel = ScanProgressViewModel(
            savedStateHandle = SavedStateHandle(mapOf("scanType" to ScanType.FULL.name)),
            scheduleScan = ScheduleScan(repository),
            observeScanDirectories = ObserveScanDirectories(repository),
            observeScanProgress = ObserveScanProgress(repository),
        )

        advanceUntilIdle()

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onAction(ScanProgressAction.OnFinishClick)

        assertEquals(ScanProgressEffect.NavigateBackToHome, effectDeferred.await())
    }
}
