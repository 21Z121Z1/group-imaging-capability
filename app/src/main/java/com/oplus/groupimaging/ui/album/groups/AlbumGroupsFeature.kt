package com.oplus.groupimaging.ui.album.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oplus.groupimaging.domain.RuleGroupSummary
import com.oplus.groupimaging.domain.usecase.ObserveRuleGroups
import com.oplus.groupimaging.ui.base.BaseViewModel
import com.oplus.groupimaging.ui.base.UiAction
import com.oplus.groupimaging.ui.base.UiEffect
import com.oplus.groupimaging.ui.base.UiState
import com.oplus.groupimaging.ui.components.AppTopBar
import com.oplus.groupimaging.ui.components.ErrorStateView
import com.oplus.groupimaging.ui.components.RuleGroupCard
import com.oplus.groupimaging.testing.TestTags
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class RuleTab { ALL, SYSTEM, CUSTOM }

data class AlbumGroupsUiState(
    val isLoading: Boolean = false,
    val selectedTab: RuleTab = RuleTab.ALL,
    val rules: List<RuleGroupSummary> = emptyList(),
    val error: String? = null,
) : UiState

sealed interface AlbumGroupsAction : UiAction {
    data class OnTabChange(val tab: RuleTab) : AlbumGroupsAction
    data class OnRuleClick(val ruleId: String) : AlbumGroupsAction
}

sealed interface AlbumGroupsEffect : UiEffect {
    data class NavigateToRulePreview(val ruleId: String) : AlbumGroupsEffect
}

@HiltViewModel
class AlbumGroupsViewModel @Inject constructor(
    private val observeRuleGroups: ObserveRuleGroups,
) : BaseViewModel<AlbumGroupsUiState, AlbumGroupsAction, AlbumGroupsEffect>(AlbumGroupsUiState(isLoading = true)) {
    init {
        launch {
            runCatching { observeRuleGroups() }
                .onSuccess { updateState { copy(isLoading = false, rules = it) } }
                .onFailure { updateState { copy(isLoading = false, error = it.message) } }
        }
    }

    override fun onAction(action: AlbumGroupsAction) {
        when (action) {
            is AlbumGroupsAction.OnTabChange -> updateState { copy(selectedTab = action.tab) }
            is AlbumGroupsAction.OnRuleClick -> emitEffect(AlbumGroupsEffect.NavigateToRulePreview(action.ruleId))
        }
    }
}

@Composable
fun AlbumGroupsRoute(
    contentPadding: PaddingValues,
    onNavigateToRulePreview: (String) -> Unit,
    viewModel: AlbumGroupsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AlbumGroupsEffect.NavigateToRulePreview -> onNavigateToRulePreview(effect.ruleId)
            }
        }
    }
    AlbumGroupsScreen(
        state = state,
        contentPadding = contentPadding,
        onAction = viewModel::onAction,
    )
}

@Composable
fun AlbumGroupsScreen(
    state: AlbumGroupsUiState,
    contentPadding: PaddingValues,
    onAction: (AlbumGroupsAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(TestTags.Screen.ALBUM_GROUPS),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppTopBar("相册分组", "规则和归档") }
        state.error?.let { error ->
            item {
                ErrorStateView(
                    title = "规则加载失败",
                    body = error,
                )
            }
        }
        items(state.rules) { rule ->
            RuleGroupCard(item = rule, modifier = Modifier.testTag(TestTags.AlbumGroups.ruleCard(rule.label))) {
                onAction(AlbumGroupsAction.OnRuleClick(rule.label))
            }
        }
    }
}
