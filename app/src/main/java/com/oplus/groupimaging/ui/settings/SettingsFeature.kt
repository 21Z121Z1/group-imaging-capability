package com.oplus.groupimaging.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oplus.groupimaging.core.MediaScanner
import com.oplus.groupimaging.domain.ScanJob
import com.oplus.groupimaging.domain.usecase.ObserveScanDirectories
import com.oplus.groupimaging.domain.usecase.ObserveScanProgress
import com.oplus.groupimaging.ui.base.BaseViewModel
import com.oplus.groupimaging.ui.base.UiAction
import com.oplus.groupimaging.ui.base.UiEffect
import com.oplus.groupimaging.ui.base.UiState
import com.oplus.groupimaging.ui.components.AppTopBar
import com.oplus.groupimaging.ui.components.ErrorStateView
import com.oplus.groupimaging.ui.components.SectionContainer
import com.oplus.groupimaging.ui.components.StatCard
import com.oplus.groupimaging.testing.TestTags
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val latestScan: ScanJob? = null,
    val extraDirectories: List<String> = emptyList(),
    val error: String? = null,
) : UiState

sealed interface SettingsAction : UiAction {
    data object OnManageDirectoriesClick : SettingsAction
    data object OnRunFullScan : SettingsAction
}

sealed interface SettingsEffect : UiEffect {
    data object NavigateToDirectoryManager : SettingsEffect
    data object NavigateToFullScan : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeScanProgress: ObserveScanProgress,
    private val observeScanDirectories: ObserveScanDirectories,
) : BaseViewModel<SettingsUiState, SettingsAction, SettingsEffect>(SettingsUiState(isLoading = true)) {
    init {
        refresh()
    }

    override fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.OnManageDirectoriesClick -> emitEffect(SettingsEffect.NavigateToDirectoryManager)
            SettingsAction.OnRunFullScan -> emitEffect(SettingsEffect.NavigateToFullScan)
        }
    }

    fun refresh() {
        launch {
            updateState { copy(isLoading = true, error = null) }
            runCatching {
                val scan = observeScanProgress()
                val extraDirectories = observeScanDirectories()
                updateState {
                    copy(
                        isLoading = false,
                        latestScan = scan,
                        extraDirectories = extraDirectories,
                    )
                }
            }.onFailure {
                updateState { copy(isLoading = false, error = it.message) }
            }
        }
    }
}

@Composable
fun SettingsRoute(
    contentPadding: PaddingValues,
    onNavigateToDirectoryManager: () -> Unit,
    onNavigateToFullScan: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToDirectoryManager -> onNavigateToDirectoryManager()
                SettingsEffect.NavigateToFullScan -> onNavigateToFullScan()
            }
        }
    }
    SettingsScreen(
        state = state,
        contentPadding = contentPadding,
        onAction = viewModel::onAction,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    contentPadding: PaddingValues,
    onAction: (SettingsAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(TestTags.Screen.SETTINGS),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppTopBar("设置") }
        state.error?.let { error ->
            item {
                ErrorStateView(
                    title = "设置摘要加载失败",
                    body = error,
                )
            }
        }
        item {
            SectionContainer("扫描目录") {
                StatCard("默认目录", MediaScanner.defaultPrefixes.joinToString(" · "))
                StatCard("扩展目录", extraDirectorySummary(state.extraDirectories), modifier = Modifier.testTag(TestTags.Settings.MANAGE_DIRECTORIES)) {
                    onAction(SettingsAction.OnManageDirectoriesClick)
                }
            }
        }
        item {
            SectionContainer("扫描与更新") {
                StatCard("最近扫描", state.latestScan?.let { "${it.status} · ${it.scannedCount}/${it.totalCount}" } ?: "暂无")
                StatCard("重新扫描", "FULL", modifier = Modifier.testTag(TestTags.Settings.RUN_FULL_SCAN)) { onAction(SettingsAction.OnRunFullScan) }
            }
        }
    }
}

private fun extraDirectorySummary(extraDirectories: List<String>): String = when {
    extraDirectories.isEmpty() -> "未配置"
    extraDirectories.size == 1 -> extraDirectories.first()
    else -> "${extraDirectories.first()} 等 ${extraDirectories.size} 项"
}
