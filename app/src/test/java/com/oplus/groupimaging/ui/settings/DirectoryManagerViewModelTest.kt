package com.oplus.groupimaging.ui.settings

import com.oplus.groupimaging.MainDispatcherRule
import com.oplus.groupimaging.TestRepository
import com.oplus.groupimaging.domain.usecase.ObserveScanDirectories
import com.oplus.groupimaging.domain.usecase.SaveScanDirectories
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DirectoryManagerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `add directory normalizes and persists`() = runTest {
        val repository = TestRepository().apply {
            normalizeExtraDirectoriesOnSave = true
        }
        val viewModel = DirectoryManagerViewModel(
            observeScanDirectories = ObserveScanDirectories(repository),
            saveScanDirectories = SaveScanDirectories(repository),
        )

        advanceUntilIdle()
        viewModel.onAction(DirectoryManagerAction.OnPendingDirectoryChanged("/storage/emulated/0/DCIM/WeChat"))
        viewModel.onAction(DirectoryManagerAction.OnAddDirectoryClick)
        advanceUntilIdle()

        assertEquals(listOf("DCIM/WeChat/"), repository.extraScanDirectories)
        assertEquals(listOf("DCIM/WeChat/"), viewModel.uiState.value.extraDirectories)
        assertEquals("", viewModel.uiState.value.pendingDirectory)
    }

    @Test
    fun `remove directory persists updated set`() = runTest {
        val repository = TestRepository().apply {
            extraScanDirectories = listOf("DCIM/WeChat/", "Pictures/Screenshots/")
            normalizeExtraDirectoriesOnSave = true
        }
        val viewModel = DirectoryManagerViewModel(
            observeScanDirectories = ObserveScanDirectories(repository),
            saveScanDirectories = SaveScanDirectories(repository),
        )

        advanceUntilIdle()
        viewModel.onAction(DirectoryManagerAction.OnRemoveDirectoryClick("DCIM/WeChat/"))
        advanceUntilIdle()

        assertEquals(listOf("Pictures/Screenshots/"), repository.extraScanDirectories)
        assertEquals(listOf("Pictures/Screenshots/"), viewModel.uiState.value.extraDirectories)
    }
}
