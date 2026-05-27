package com.oplus.groupimaging.ui.album.preview

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oplus.groupimaging.domain.AssetPreview
import com.oplus.groupimaging.domain.FilterSpec
import com.oplus.groupimaging.domain.MoveExecutionOutcome
import com.oplus.groupimaging.domain.MoveCandidateRole
import com.oplus.groupimaging.domain.MovePreview
import com.oplus.groupimaging.domain.MoveResult
import com.oplus.groupimaging.domain.usecase.ExecuteMovePlan
import com.oplus.groupimaging.domain.usecase.ObserveRuleGroups
import com.oplus.groupimaging.domain.usecase.PreviewMovePlan
import com.oplus.groupimaging.data.resolveRuleMoveTarget
import com.oplus.groupimaging.navigation.MoveConfirmRequest
import com.oplus.groupimaging.ui.base.BaseViewModel
import com.oplus.groupimaging.ui.base.UiAction
import com.oplus.groupimaging.ui.base.UiEffect
import com.oplus.groupimaging.ui.base.UiState
import com.oplus.groupimaging.ui.components.AppTopBar
import com.oplus.groupimaging.ui.components.AssetPreviewItem
import com.oplus.groupimaging.ui.components.EmptyStateView
import com.oplus.groupimaging.ui.components.ErrorStateView
import com.oplus.groupimaging.ui.components.ProgressHeroCard
import com.oplus.groupimaging.ui.components.SectionContainer
import com.oplus.groupimaging.testing.TestTags
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class RulePreviewUiState(
    val isLoading: Boolean = false,
    val ruleName: String? = null,
    val ruleFilters: FilterSpec = FilterSpec(),
    val matchedCount: Int = 0,
    val assets: List<AssetPreview> = emptyList(),
    val latestPreview: MovePreview? = null,
    val latestResult: MoveResult? = null,
    val error: String? = null,
) : UiState

sealed interface RulePreviewAction : UiAction {
    data object OnMoveClick : RulePreviewAction
    data object OnConfirmMove : RulePreviewAction
    data class OnMoveAuthorizationResult(val granted: Boolean) : RulePreviewAction
}

sealed interface RulePreviewEffect : UiEffect {
    data class ShowMoveConfirmDialog(val plan: MoveConfirmUiState) : RulePreviewEffect
    data class ShowMoveProgress(val progress: MoveProgressUiState) : RulePreviewEffect
    data class RequestMoveWriteAccess(val intentSender: android.content.IntentSender) : RulePreviewEffect
}

data class MoveConfirmUiState(
    val targetFolderName: String,
    val totalFiles: Int,
    val includesRawPairs: Boolean,
    val conflictCount: Int,
    val warningText: String,
)

data class MoveProgressUiState(
    val progress: Float,
    val movedCount: Int,
    val failedCount: Int,
    val totalCount: Int,
    val currentFileName: String?,
    val isCompleted: Boolean = false,
)

@HiltViewModel
class RulePreviewViewModel @Inject constructor(
    private val observeRuleGroups: ObserveRuleGroups,
    private val previewMovePlan: PreviewMovePlan,
    private val executeMovePlan: ExecuteMovePlan,
) : BaseViewModel<RulePreviewUiState, RulePreviewAction, RulePreviewEffect>(RulePreviewUiState(isLoading = true)) {
    private val defaultMoveTargetPath = "/storage/emulated/0/DCIM/myalbums/"

    fun load(ruleId: String) {
        if (currentState().ruleName == ruleId && !currentState().isLoading) return
        launch {
            updateState { copy(isLoading = true, error = null) }
            runCatching {
                val groups = observeRuleGroups()
                val group = groups.firstOrNull { it.label == ruleId }
                val filters = group?.filters ?: FilterSpec()
                val preview = previewMovePlan(ruleId, moveTargetPathFor(ruleId), filters)
                updateState {
                    copy(
                        isLoading = false,
                        ruleName = ruleId,
                        ruleFilters = filters,
                        matchedCount = preview.count,
                        assets = preview.candidates.map { candidate ->
                            AssetPreview(
                                assetId = candidate.assetId,
                                assetUri = candidate.sourceUri,
                                fileName = candidate.fileName,
                                captureDate = candidate.captureDate,
                                deviceModel = candidate.deviceModel,
                                focalLengthEq = candidate.focalLengthEq,
                                captureModeLabel = candidate.captureModeLabel,
                                isLivePhoto = candidate.isLivePhoto,
                                isRaw = candidate.isRaw,
                            )
                        },
                        latestPreview = preview,
                    )
                }
            }.onFailure {
                updateState { copy(isLoading = false, error = it.message) }
            }
        }
    }

    override fun onAction(action: RulePreviewAction) {
        when (action) {
            RulePreviewAction.OnMoveClick -> {
                launch {
                    val preview = currentState().latestPreview ?: previewMovePlan(
                        currentState().ruleName ?: "手动归档",
                        moveTargetPathFor(currentState().ruleName ?: "手动归档"),
                        currentState().ruleFilters,
                    ).also { updateState { copy(latestPreview = it) } }
                    if (preview.candidates.isEmpty()) {
                        updateState { copy(error = "没有可移动的文件") }
                        return@launch
                    }
                    emitEffect(
                        RulePreviewEffect.ShowMoveConfirmDialog(
                            MoveConfirmUiState(
                                targetFolderName = preview.targetFolder,
                                totalFiles = preview.count,
                                includesRawPairs = preview.candidates.any { it.role == MoveCandidateRole.RAW_COMPANION },
                                conflictCount = preview.conflicts,
                                warningText = "移动会修改原文件在系统媒体库中的位置。继续前请确认目标目录、RAW 伴随项和冲突改名数量。",
                            ),
                        ),
                    )
                }
            }
            RulePreviewAction.OnConfirmMove -> {
                val preview = currentState().latestPreview ?: return
                launch {
                    executeMove(preview = preview, writeAccessGranted = false)
                }
            }
            is RulePreviewAction.OnMoveAuthorizationResult -> {
                val preview = currentState().latestPreview ?: return
                launch {
                    if (!action.granted) {
                        updateState { copy(error = "移动权限未授予") }
                        return@launch
                    }
                    emitEffect(
                        RulePreviewEffect.ShowMoveProgress(
                            MoveProgressUiState(0.3f, 0, 0, preview.count, preview.candidates.firstOrNull()?.fileName),
                        ),
                    )
                    executeMove(preview = preview, writeAccessGranted = true)
                }
            }
        }
    }

    private suspend fun executeMove(
        preview: MovePreview,
        writeAccessGranted: Boolean,
    ) {
        runCatching { executeMovePlan(preview.token, writeAccessGranted) }
            .onSuccess { outcome ->
                when (outcome) {
                    is MoveExecutionOutcome.Completed -> handleMoveCompleted(preview, outcome.result)
                    is MoveExecutionOutcome.RequiresWriteAccess -> {
                        emitEffect(RulePreviewEffect.RequestMoveWriteAccess(outcome.intentSender))
                    }
                }
            }
            .onFailure {
                updateState { copy(error = it.message) }
            }
    }

    private suspend fun handleMoveCompleted(
        preview: MovePreview,
        result: MoveResult,
    ) {
        updateState { copy(latestResult = result, error = null) }
        emitEffect(
            RulePreviewEffect.ShowMoveProgress(
                MoveProgressUiState(
                    progress = 1f,
                    movedCount = result.movedCount,
                    failedCount = result.failureCount,
                    totalCount = preview.count,
                    currentFileName = null,
                    isCompleted = true,
                ),
            ),
        )
    }

    private fun moveTargetPathFor(ruleName: String): String =
        resolveRuleMoveTarget(defaultMoveTargetPath, ruleName).absolutePath
}

@Composable
fun RulePreviewRoute(
    contentPadding: PaddingValues,
    ruleId: String,
    onBack: () -> Unit,
    onShowMoveConfirm: (MoveConfirmRequest) -> Unit,
    onMoveProgress: (MoveProgressUiState) -> Unit,
    viewModel: RulePreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val moveAuthorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onAction(
            RulePreviewAction.OnMoveAuthorizationResult(
                granted = result.resultCode == Activity.RESULT_OK,
            ),
        )
    }
    LaunchedEffect(ruleId) { viewModel.load(ruleId) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RulePreviewEffect.ShowMoveConfirmDialog -> onShowMoveConfirm(
                    MoveConfirmRequest(
                        state = effect.plan,
                        onConfirm = { viewModel.onAction(RulePreviewAction.OnConfirmMove) },
                    ),
                )
                is RulePreviewEffect.ShowMoveProgress -> onMoveProgress(effect.progress)
                is RulePreviewEffect.RequestMoveWriteAccess -> {
                    moveAuthorizationLauncher.launch(
                        IntentSenderRequest.Builder(effect.intentSender).build(),
                    )
                }
            }
        }
    }
    RulePreviewScreen(
        state = state,
        contentPadding = contentPadding,
        onBack = onBack,
        onAction = viewModel::onAction,
    )
}

@Composable
fun RulePreviewScreen(
    state: RulePreviewUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onAction: (RulePreviewAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(TestTags.Screen.RULE_PREVIEW),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppTopBar(state.ruleName ?: "规则预览", "命中 ${state.matchedCount}") }
        state.error?.let { error ->
            item {
                ErrorStateView(
                    title = "规则预览加载失败",
                    body = error,
                )
            }
        }
        item {
            SectionContainer(
                "规则头部",
                actionLabel = "移动全部".takeIf { state.matchedCount > 0 },
                onAction = { onAction(RulePreviewAction.OnMoveClick) },
                actionModifier = Modifier.testTag(TestTags.RulePreview.MOVE_ALL),
            ) {
                Text("条件命中 ${state.matchedCount} 项")
            }
        }
        if (!state.isLoading && state.assets.isEmpty() && state.error == null) {
            item {
                EmptyStateView(
                    title = "没有可移动的文件",
                    body = "当前规则没有命中任何可归档的媒体文件。",
                )
            }
        }
        items(state.assets) { asset ->
            AssetPreviewItem(item = asset, onClick = onBack)
        }
    }
}

@Composable
fun MoveConfirmDialog(
    state: MoveConfirmUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.Dialogs.MOVE_CONFIRM),
        title = { Text("确认移动原文件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("目标目录：${state.targetFolderName}")
                Text("文件数量：${state.totalFiles}")
                Text("RAW 配对：${if (state.includesRawPairs) "包含" else "无"}")
                Text("同名冲突：${state.conflictCount}")
                Text("系统可能会请求 MediaStore 写入授权。")
                Text(state.warningText)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.Dialogs.MOVE_CONFIRM_CONTINUE),
            ) { Text("继续") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Dialogs.MOVE_CONFIRM_CANCEL),
            ) { Text("取消") }
        },
    )
}

@Composable
fun MoveProgressScreen(
    state: MoveProgressUiState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.Dialogs.MOVE_PROGRESS),
        title = { Text("移动进度") },
        text = {
            ProgressHeroCard(
                title = "已移动 ${state.movedCount}/${state.totalCount}",
                supporting = "失败 ${state.failedCount}",
                progress = state.progress,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Dialogs.MOVE_PROGRESS_CONFIRM),
            ) { Text(if (state.isCompleted) "完成" else "关闭") }
        },
    )
}
