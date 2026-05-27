package com.oplus.groupimaging.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oplus.groupimaging.core.MediaScanner
import com.oplus.groupimaging.domain.usecase.ObserveScanDirectories
import com.oplus.groupimaging.domain.usecase.SaveScanDirectories
import com.oplus.groupimaging.ui.base.BaseViewModel
import com.oplus.groupimaging.ui.base.UiAction
import com.oplus.groupimaging.ui.base.UiEffect
import com.oplus.groupimaging.ui.base.UiState
import com.oplus.groupimaging.ui.components.AppTopBar
import com.oplus.groupimaging.ui.components.ErrorStateView
import com.oplus.groupimaging.ui.components.FilterChipRow
import com.oplus.groupimaging.ui.components.SectionContainer
import com.oplus.groupimaging.ui.components.StatCard
import com.oplus.groupimaging.testing.TestTags
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class DirectoryManagerUiState(
    val isLoading: Boolean = false,
    val defaultDirectories: List<String> = MediaScanner.defaultPrefixes,
    val pendingDirectory: String = "",
    val extraDirectories: List<String> = emptyList(),
    val errorMessage: String? = null,
) : UiState

sealed interface DirectoryManagerAction : UiAction {
    data class OnPendingDirectoryChanged(val value: String) : DirectoryManagerAction
    data object OnAddDirectoryClick : DirectoryManagerAction
    data class OnRemoveDirectoryClick(val directory: String) : DirectoryManagerAction
}

@HiltViewModel
class DirectoryManagerViewModel @Inject constructor(
    private val observeScanDirectories: ObserveScanDirectories,
    private val saveScanDirectories: SaveScanDirectories,
) :
    BaseViewModel<DirectoryManagerUiState, DirectoryManagerAction, UiEffect>(DirectoryManagerUiState(isLoading = true)) {
    init {
        load()
    }

    override fun onAction(action: DirectoryManagerAction) {
        when (action) {
            DirectoryManagerAction.OnAddDirectoryClick -> addDirectory()
            is DirectoryManagerAction.OnPendingDirectoryChanged -> updateState {
                copy(
                    pendingDirectory = action.value,
                    errorMessage = null,
                )
            }
            is DirectoryManagerAction.OnRemoveDirectoryClick -> removeDirectory(action.directory)
        }
    }

    private fun load() {
        launch {
            val directories = observeScanDirectories()
            updateState {
                copy(
                    isLoading = false,
                    extraDirectories = directories,
                )
            }
        }
    }

    private fun addDirectory() {
        launch {
            runCatching {
                saveScanDirectories(currentState().extraDirectories + currentState().pendingDirectory)
            }.onSuccess { directories ->
                updateState {
                    copy(
                        extraDirectories = directories,
                        pendingDirectory = "",
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                updateState { copy(errorMessage = error.message ?: "目录保存失败") }
            }
        }
    }

    private fun removeDirectory(directory: String) {
        launch {
            runCatching {
                saveScanDirectories(currentState().extraDirectories - directory)
            }.onSuccess { directories ->
                updateState {
                    copy(
                        extraDirectories = directories,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                updateState { copy(errorMessage = error.message ?: "目录移除失败") }
            }
        }
    }
}

@Composable
fun DirectoryManagerRoute(
    contentPadding: PaddingValues,
    viewModel: DirectoryManagerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DirectoryManagerScreen(
        state = state,
        contentPadding = contentPadding,
        onAction = viewModel::onAction,
    )
}

@Composable
fun DirectoryManagerScreen(
    state: DirectoryManagerUiState,
    contentPadding: PaddingValues,
    onAction: (DirectoryManagerAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .testTag(TestTags.Screen.DIRECTORY_MANAGER),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppTopBar("扫描目录管理", "默认目录固定，扩展目录可自定义") }
        state.errorMessage?.let { error ->
            item {
                ErrorStateView(
                    title = "目录配置无效",
                    body = error,
                )
            }
        }
        item {
            SectionContainer("主目录") {
                state.defaultDirectories.forEach { directory ->
                    StatCard("默认目录", directory)
                }
            }
        }
        item {
            SectionContainer("新增扩展目录") {
                OutlinedTextField(
                    value = state.pendingDirectory,
                    onValueChange = { onAction(DirectoryManagerAction.OnPendingDirectoryChanged(it)) },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.DirectoryManager.INPUT),
                    label = { Text("相对路径") },
                    placeholder = { Text("例如 DCIM/WeChat/") },
                    singleLine = true,
                )
                TextButton(
                    onClick = { onAction(DirectoryManagerAction.OnAddDirectoryClick) },
                    modifier = Modifier.testTag(TestTags.DirectoryManager.ADD),
                ) {
                    Text("添加目录")
                }
            }
        }
        item {
            SectionContainer("扩展目录") {
                if (state.extraDirectories.isEmpty()) {
                    StatCard("当前状态", "未配置")
                } else {
                    FilterChipRow(
                        chips = state.extraDirectories,
                        onRemove = { onAction(DirectoryManagerAction.OnRemoveDirectoryClick(it)) },
                    )
                }
            }
        }
    }
}
