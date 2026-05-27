package com.oplus.groupimaging.ui.deviceprofiles

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
import com.oplus.groupimaging.domain.DeviceProfile
import com.oplus.groupimaging.domain.usecase.ObserveDeviceProfiles
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

data class DeviceProfilesUiState(
    val isLoading: Boolean = false,
    val items: List<DeviceProfile> = emptyList(),
    val error: String? = null,
) : UiState

sealed interface DeviceProfilesAction : UiAction {
    data object OnRetry : DeviceProfilesAction
}

@HiltViewModel
class DeviceProfilesViewModel @Inject constructor(
    private val observeDeviceProfiles: ObserveDeviceProfiles,
) : BaseViewModel<DeviceProfilesUiState, DeviceProfilesAction, UiEffect>(DeviceProfilesUiState(isLoading = true)) {
    init {
        load()
    }

    override fun onAction(action: DeviceProfilesAction) {
        when (action) {
            DeviceProfilesAction.OnRetry -> load()
        }
    }

    private fun load() {
        launch {
            updateState { copy(isLoading = true, error = null) }
            runCatching { observeDeviceProfiles() }
                .onSuccess { updateState { copy(isLoading = false, items = it) } }
                .onFailure { updateState { copy(isLoading = false, error = it.message) } }
        }
    }
}

@Composable
fun DeviceProfilesRoute(
    contentPadding: PaddingValues,
    viewModel: DeviceProfilesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DeviceProfilesScreen(
        state = state,
        contentPadding = contentPadding,
        onAction = viewModel::onAction,
    )
}

@Composable
fun DeviceProfilesScreen(
    state: DeviceProfilesUiState,
    contentPadding: PaddingValues,
    onAction: (DeviceProfilesAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(TestTags.Screen.DEVICE_PROFILES),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppTopBar("机型配置层") }
        when {
            state.error != null -> item {
                ErrorStateView(
                    title = "机型配置加载失败",
                    body = state.error.orEmpty(),
                    onRetry = { onAction(DeviceProfilesAction.OnRetry) },
                )
            }
            !state.isLoading && state.items.isEmpty() -> item {
                EmptyStateView(
                    title = "没有机型配置",
                    body = "系统会在首次扫描时自动补齐默认 profile。",
                )
            }
        }
        items(state.items) { item ->
            StatCard(item.deviceModel, "配置版本 ${item.profileVersion}")
        }
    }
}
