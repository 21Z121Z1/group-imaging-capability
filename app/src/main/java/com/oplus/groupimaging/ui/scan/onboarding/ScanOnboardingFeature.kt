package com.oplus.groupimaging.ui.scan.onboarding

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
import com.oplus.groupimaging.domain.usecase.ObserveScanDirectories
import com.oplus.groupimaging.ui.base.BaseViewModel
import com.oplus.groupimaging.ui.base.UiAction
import com.oplus.groupimaging.ui.base.UiEffect
import com.oplus.groupimaging.ui.base.UiState
import com.oplus.groupimaging.ui.components.AppTopBar
import com.oplus.groupimaging.ui.components.EmptyStateView
import com.oplus.groupimaging.ui.components.SectionContainer
import com.oplus.groupimaging.ui.components.StatCard
import com.oplus.groupimaging.testing.TestTags
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ScanOnboardingUiState(
    val oplusOnly: Boolean = true,
    val selectedDirectories: List<String> = MediaScanner.defaultPrefixes,
) : UiState

sealed interface ScanOnboardingAction : UiAction {
    data object OnStartScanClick : ScanOnboardingAction
}

sealed interface ScanOnboardingEffect : UiEffect {
    data object NavigateToScanProgress : ScanOnboardingEffect
}

@HiltViewModel
class ScanOnboardingViewModel @Inject constructor(
    private val observeScanDirectories: ObserveScanDirectories,
) :
    BaseViewModel<ScanOnboardingUiState, ScanOnboardingAction, ScanOnboardingEffect>(ScanOnboardingUiState()) {
    init {
        refreshDirectories()
    }

    override fun onAction(action: ScanOnboardingAction) {
        when (action) {
            ScanOnboardingAction.OnStartScanClick -> emitEffect(ScanOnboardingEffect.NavigateToScanProgress)
        }
    }

    fun refreshDirectories() {
        launch {
            val extraDirectories = observeScanDirectories()
            updateState {
                copy(selectedDirectories = MediaScanner.defaultPrefixes + extraDirectories)
            }
        }
    }
}

@Composable
fun ScanOnboardingRoute(
    contentPadding: PaddingValues,
    onStartScan: () -> Unit,
    viewModel: ScanOnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshDirectories() }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ScanOnboardingEffect.NavigateToScanProgress -> onStartScan()
            }
        }
    }
    ScanOnboardingScreen(
        state = state,
        contentPadding = contentPadding,
        onAction = viewModel::onAction,
    )
}

@Composable
fun ScanOnboardingScreen(
    state: ScanOnboardingUiState,
    contentPadding: PaddingValues,
    onAction: (ScanOnboardingAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(TestTags.Screen.SCAN_ONBOARDING),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppTopBar("首次扫描") }
        item {
            EmptyStateView(
                title = "建立摄影索引",
                body = "授权媒体访问后，系统会扫描本机拍摄的照片、提取 EXIF、解析私有 tag，并建立本地索引。",
            )
        }
        item {
            SectionContainer("扫描目录") {
                state.selectedDirectories.forEach { directory ->
                    StatCard("目录", directory)
                }
            }
        }
        item { StatCard("开始扫描", "执行", modifier = Modifier.testTag(TestTags.ScanOnboarding.START_SCAN)) { onAction(ScanOnboardingAction.OnStartScanClick) } }
    }
}
