package com.oplus.groupimaging.ui.scan.progress

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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oplus.groupimaging.domain.ScanJob
import com.oplus.groupimaging.domain.ScanStatus
import com.oplus.groupimaging.domain.ScanType
import com.oplus.groupimaging.domain.usecase.ObserveScanDirectories
import com.oplus.groupimaging.domain.usecase.ObserveScanProgress
import com.oplus.groupimaging.domain.usecase.ScheduleScan
import com.oplus.groupimaging.ui.base.BaseViewModel
import com.oplus.groupimaging.ui.base.UiAction
import com.oplus.groupimaging.ui.base.UiEffect
import com.oplus.groupimaging.ui.base.UiState
import com.oplus.groupimaging.ui.components.AppTopBar
import com.oplus.groupimaging.ui.components.ProgressHeroCard
import com.oplus.groupimaging.ui.components.SectionContainer
import com.oplus.groupimaging.ui.components.StatCard
import com.oplus.groupimaging.testing.TestTags
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class ScanStage {
    ENUMERATING,
    BUILDING_INDEX,
    FAILED,
    DONE,
}

data class ScanProgressUiState(
    val stage: ScanStage = ScanStage.ENUMERATING,
    val progress: Float = 0f,
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val currentFileName: String? = null,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface ScanProgressAction : UiAction {
    data object OnFinishClick : ScanProgressAction
}

sealed interface ScanProgressEffect : UiEffect {
    data object NavigateBackToHome : ScanProgressEffect
}

@HiltViewModel
class ScanProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleScan: ScheduleScan,
    private val observeScanDirectories: ObserveScanDirectories,
    private val observeScanProgress: ObserveScanProgress,
) : BaseViewModel<ScanProgressUiState, ScanProgressAction, ScanProgressEffect>(ScanProgressUiState()) {
    private val requestedScanType = savedStateHandle.get<String>("scanType")?.let(ScanType::valueOf)
    private val monitorStartedAt = System.currentTimeMillis()

    init {
        if (requestedScanType != null) {
            monitorScan(minStartedAt = monitorStartedAt)
            startScan(requestedScanType)
        } else {
            monitorScan(minStartedAt = null)
        }
    }

    override fun onAction(action: ScanProgressAction) {
        when (action) {
            ScanProgressAction.OnFinishClick -> emitEffect(ScanProgressEffect.NavigateBackToHome)
        }
    }

    private fun startScan(scanType: ScanType) {
        launch {
            runCatching {
                scheduleScan(
                    scanType = scanType,
                    extraRoots = observeScanDirectories().toSet(),
                )
            }
                .onFailure { error ->
                    updateState {
                        copy(
                            stage = ScanStage.FAILED,
                            isCompleted = false,
                            currentFileName = error.message ?: "扫描失败",
                            failedCount = 1,
                            errorMessage = error.message ?: error.javaClass.simpleName,
                        )
                    }
                }
        }
    }

    private fun monitorScan(minStartedAt: Long?) {
        launch {
            observeScanProgress.flow().collect { latest ->
                val matchesRequestedWindow = minStartedAt == null ||
                    (latest?.startedAt ?: Long.MIN_VALUE) >= minStartedAt ||
                    latest?.status == ScanStatus.RUNNING
                if (matchesRequestedWindow) {
                    latest?.let(::applyScanJob)
                }
            }
        }
    }

    private fun applyScanJob(job: ScanJob) {
        val totalCount = job.totalCount
        val scannedCount = job.scannedCount
        val progress = when {
            totalCount > 0 -> scannedCount.toFloat() / totalCount.toFloat()
            job.status == ScanStatus.RUNNING -> 0.02f
            else -> 0f
        }
        val stage = when (job.status) {
            ScanStatus.RUNNING -> if (totalCount == 0) ScanStage.ENUMERATING else ScanStage.BUILDING_INDEX
            ScanStatus.COMPLETED -> ScanStage.DONE
            ScanStatus.FAILED -> ScanStage.FAILED
            ScanStatus.IDLE -> ScanStage.ENUMERATING
        }
        updateState {
            copy(
                stage = stage,
                progress = progress.coerceIn(0f, 1f),
                scannedCount = scannedCount,
                totalCount = totalCount,
                currentFileName = when (job.status) {
                    ScanStatus.RUNNING -> if (totalCount == 0) "正在枚举媒体库" else "正在建立索引"
                    ScanStatus.COMPLETED -> "扫描完成"
                    ScanStatus.FAILED -> errorMessage ?: "扫描失败"
                    ScanStatus.IDLE -> "等待开始"
                },
                successCount = (scannedCount - job.failureCount).coerceAtLeast(0),
                failedCount = job.failureCount,
                isCompleted = job.status == ScanStatus.COMPLETED,
                errorMessage = if (job.status == ScanStatus.FAILED) errorMessage ?: "请查看日志定位失败阶段" else null,
            )
        }
    }
}

private fun scanStageLabel(stage: ScanStage): String = when (stage) {
    ScanStage.ENUMERATING -> "ENUMERATING"
    ScanStage.BUILDING_INDEX -> "BUILDING_INDEX"
    ScanStage.FAILED -> "FAILED"
    ScanStage.DONE -> "DONE"
}

@Composable
fun ScanProgressRoute(
    contentPadding: PaddingValues,
    onFinish: () -> Unit,
    viewModel: ScanProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ScanProgressEffect.NavigateBackToHome -> onFinish()
            }
        }
    }
    ScanProgressScreen(
        state = state,
        contentPadding = contentPadding,
        onAction = viewModel::onAction,
    )
}

@Composable
fun ScanProgressScreen(
    state: ScanProgressUiState,
    contentPadding: PaddingValues,
    onAction: (ScanProgressAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(TestTags.Screen.SCAN_PROGRESS),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppTopBar("扫描进度", scanStageLabel(state.stage)) }
        item {
            ProgressHeroCard(
                title = "已扫描 ${state.scannedCount}/${state.totalCount}",
                supporting = state.errorMessage ?: state.currentFileName ?: "准备完成",
                progress = state.progress,
                modifier = Modifier.testTag(TestTags.ScanProgress.HERO),
            )
        }
        item {
            SectionContainer("统计") {
                StatCard("成功", state.successCount.toString())
                StatCard("失败", state.failedCount.toString())
            }
        }
        if (state.isCompleted) {
            item { StatCard("完成", "返回首页", modifier = Modifier.testTag(TestTags.ScanProgress.FINISH)) { onAction(ScanProgressAction.OnFinishClick) } }
        } else if (state.stage == ScanStage.FAILED) {
            item { StatCard("失败", "返回首页", modifier = Modifier.testTag(TestTags.ScanProgress.FINISH)) { onAction(ScanProgressAction.OnFinishClick) } }
        }
    }
}
