package com.oplus.groupimaging.ui.failed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oplus.groupimaging.domain.FailedItem
import com.oplus.groupimaging.domain.usecase.ObserveFailedItems
import com.oplus.groupimaging.ui.base.BaseViewModel
import com.oplus.groupimaging.ui.base.UiAction
import com.oplus.groupimaging.ui.base.UiEffect
import com.oplus.groupimaging.ui.base.UiState
import com.oplus.groupimaging.ui.components.AppTopBar
import com.oplus.groupimaging.ui.components.EmptyStateView
import com.oplus.groupimaging.ui.components.ErrorStateView
import com.oplus.groupimaging.ui.components.StatCard
import com.oplus.groupimaging.testing.TestTags
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class FailedItemsUiState(
    val isLoading: Boolean = false,
    val items: List<FailedItem> = emptyList(),
    val error: String? = null,
) : UiState

sealed interface FailedItemsAction : UiAction {
    data object OnRetry : FailedItemsAction
}

@HiltViewModel
class FailedItemsViewModel @Inject constructor(
    private val observeFailedItems: ObserveFailedItems,
) : BaseViewModel<FailedItemsUiState, FailedItemsAction, UiEffect>(FailedItemsUiState(isLoading = true)) {
    init {
        load()
    }

    override fun onAction(action: FailedItemsAction) {
        when (action) {
            FailedItemsAction.OnRetry -> load()
        }
    }

    private fun load() {
        launch {
            updateState { copy(isLoading = true, error = null) }
            runCatching { observeFailedItems() }
                .onSuccess { updateState { copy(isLoading = false, items = it) } }
                .onFailure { updateState { copy(isLoading = false, error = it.message) } }
        }
    }
}

@Composable
fun FailedItemsRoute(
    contentPadding: PaddingValues,
    viewModel: FailedItemsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FailedItemsScreen(
        state = state,
        contentPadding = contentPadding,
        onAction = viewModel::onAction,
    )
}

@Composable
fun FailedItemsScreen(
    state: FailedItemsUiState,
    contentPadding: PaddingValues,
    onAction: (FailedItemsAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(TestTags.Screen.FAILED_ITEMS),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppTopBar("失败项") }
        when {
            state.error != null -> item {
                ErrorStateView(
                    title = "失败明细加载失败",
                    body = state.error.orEmpty(),
                    onRetry = { onAction(FailedItemsAction.OnRetry) },
                )
            }
            !state.isLoading && state.items.isEmpty() -> item {
                EmptyStateView(
                    title = "当前没有失败项",
                    body = "解析失败和移动失败会统一收敛到这里。",
                )
            }
        }
        items(state.items) { item ->
            StatCard(item.title, item.detail)
        }
    }
}
