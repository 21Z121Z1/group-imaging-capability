package com.oplus.groupimaging.ui.scan.onboarding

import com.oplus.groupimaging.MainDispatcherRule
import com.oplus.groupimaging.TestRepository
import com.oplus.groupimaging.core.MediaScanner
import com.oplus.groupimaging.domain.usecase.ObserveScanDirectories
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanOnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `selected directories include defaults and extras`() = runTest {
        val repository = TestRepository().apply {
            extraScanDirectories = listOf("DCIM/WeChat/")
        }

        val viewModel = ScanOnboardingViewModel(
            observeScanDirectories = ObserveScanDirectories(repository),
        )

        advanceUntilIdle()

        assertEquals(
            MediaScanner.defaultPrefixes + listOf("DCIM/WeChat/"),
            viewModel.uiState.value.selectedDirectories,
        )
    }
}
