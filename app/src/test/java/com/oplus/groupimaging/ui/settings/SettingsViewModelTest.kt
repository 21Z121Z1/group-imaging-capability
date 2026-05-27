package com.oplus.groupimaging.ui.settings

import com.oplus.groupimaging.MainDispatcherRule
import com.oplus.groupimaging.TestRepository
import com.oplus.groupimaging.domain.usecase.ObserveDeviceProfiles
import com.oplus.groupimaging.domain.usecase.ObserveScanDirectories
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
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refresh loads latest scan directory summary and profile count`() = runTest {
        val repository = TestRepository().apply {
            latestScanJobValue = InsightTestFixtures.completedScanJob()
            deviceProfiles = InsightTestFixtures.deviceProfiles()
            extraScanDirectories = listOf("DCIM/WeChat/", "Pictures/Screenshots/")
        }

        val viewModel = SettingsViewModel(
            observeScanProgress = ObserveScanProgress(repository),
            observeDeviceProfiles = ObserveDeviceProfiles(repository),
            observeScanDirectories = ObserveScanDirectories(repository),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.deviceProfileCount)
        assertEquals(listOf("DCIM/WeChat/", "Pictures/Screenshots/"), state.extraDirectories)
        assertEquals("COMPLETED", state.latestScan?.status?.name)
    }

    @Test
    fun `refresh exposes error when summary load fails`() = runTest {
        val repository = TestRepository().apply {
            deviceProfilesFailure = IllegalStateException("profile table unavailable")
        }

        val viewModel = SettingsViewModel(
            observeScanProgress = ObserveScanProgress(repository),
            observeDeviceProfiles = ObserveDeviceProfiles(repository),
            observeScanDirectories = ObserveScanDirectories(repository),
        )

        advanceUntilIdle()

        assertEquals("profile table unavailable", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `run full scan action emits navigation effect`() = runTest {
        val repository = TestRepository()
        val viewModel = SettingsViewModel(
            observeScanProgress = ObserveScanProgress(repository),
            observeDeviceProfiles = ObserveDeviceProfiles(repository),
            observeScanDirectories = ObserveScanDirectories(repository),
        )
        advanceUntilIdle()

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onAction(SettingsAction.OnRunFullScan)

        assertEquals(SettingsEffect.NavigateToFullScan, effectDeferred.await())
    }
}
