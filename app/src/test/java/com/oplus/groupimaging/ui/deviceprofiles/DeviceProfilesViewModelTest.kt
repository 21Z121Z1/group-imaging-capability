package com.oplus.groupimaging.ui.deviceprofiles

import com.oplus.groupimaging.MainDispatcherRule
import com.oplus.groupimaging.TestRepository
import com.oplus.groupimaging.domain.usecase.ObserveDeviceProfiles
import com.oplus.groupimaging.testing.InsightTestFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceProfilesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads device profiles`() = runTest {
        val repository = TestRepository().apply {
            deviceProfiles = InsightTestFixtures.deviceProfiles()
        }

        val viewModel = DeviceProfilesViewModel(
            observeDeviceProfiles = ObserveDeviceProfiles(repository),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.items.size)
        assertEquals("OPPO Find X8 Ultra", viewModel.uiState.value.items.first().deviceModel)
    }

    @Test
    fun `retry recovers after initial load failure`() = runTest {
        val repository = TestRepository().apply {
            deviceProfilesFailure = IllegalStateException("device profiles missing")
        }

        val viewModel = DeviceProfilesViewModel(
            observeDeviceProfiles = ObserveDeviceProfiles(repository),
        )

        advanceUntilIdle()
        assertEquals("device profiles missing", viewModel.uiState.value.error)

        repository.deviceProfilesFailure = null
        repository.deviceProfiles = InsightTestFixtures.deviceProfiles()
        viewModel.onAction(DeviceProfilesAction.OnRetry)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.error)
        assertEquals(2, viewModel.uiState.value.items.size)
    }
}
