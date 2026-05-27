package com.oplus.groupimaging.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.oplus.groupimaging.ui.album.preview.MoveConfirmDialog
import com.oplus.groupimaging.ui.album.preview.MoveConfirmUiState
import com.oplus.groupimaging.ui.album.preview.MoveProgressScreen
import com.oplus.groupimaging.ui.album.preview.MoveProgressUiState
import com.oplus.groupimaging.ui.components.PermissionDialog
import com.oplus.groupimaging.ui.icons.GroupImagingIcons
import com.oplus.groupimaging.ui.insight.FilterBottomSheet
import com.oplus.groupimaging.ui.insight.FilterSheetUiState
import com.oplus.groupimaging.ui.insight.InsightFiltersUi
import com.oplus.groupimaging.testing.TestTags

internal const val InsightSeedKey = "insight_seed"

data class FilterSheetRequest(
    val state: FilterSheetUiState,
    val onApply: (InsightFiltersUi) -> Unit,
)

data class MoveConfirmRequest(
    val state: MoveConfirmUiState,
    val onConfirm: () -> Unit,
)

@Composable
fun MainScaffold(
    currentRoute: String?,
    snackbarHostState: SnackbarHostState,
    topLevelDestinations: List<TopLevelDestination>,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    val topLevelRoutes = topLevelDestinations.map { it.route.route }.toSet()
    val showTopLevelNavigation = currentRoute in topLevelRoutes
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showTopLevelNavigation && !useRail) {
                    NavigationBar {
                        topLevelDestinations.forEach { item ->
                            NavigationBarItem(
                                modifier = Modifier.testTag(TestTags.Navigation.forRoute(item.route)),
                                selected = currentRoute == item.route.route,
                                onClick = item.onClick,
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            if (showTopLevelNavigation && useRail) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail {
                        topLevelDestinations.forEach { item ->
                            NavigationRailItem(
                                modifier = Modifier.testTag(TestTags.Navigation.forRoute(item.route)),
                                selected = currentRoute == item.route.route,
                                onClick = item.onClick,
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        content(innerPadding)
                    }
                }
            } else {
                content(innerPadding)
            }
        }
    }
}

data class TopLevelDestination(
    val route: AppRoute,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

val topLevelRouteDefinitions = listOf(
    Triple(AppRoute.Home, "首页", GroupImagingIcons.Home),
    Triple(AppRoute.Calendar, "日历", GroupImagingIcons.CalendarMonth),
    Triple(AppRoute.Insight, "洞察", GroupImagingIcons.Analytics),
    Triple(AppRoute.AlbumGroups, "相册", GroupImagingIcons.Collections),
    Triple(AppRoute.Settings, "设置", GroupImagingIcons.Settings),
)

@Composable
fun GlobalSheetsAndDialogs(
    filterSheetRequest: FilterSheetRequest?,
    onDismissFilterSheet: () -> Unit,
    moveConfirmRequest: MoveConfirmRequest?,
    onDismissMoveConfirm: () -> Unit,
    moveProgressState: MoveProgressUiState?,
    onDismissMoveProgress: () -> Unit,
    showPermissionDialog: Boolean,
    onDismissPermissionDialog: () -> Unit,
    onConfirmPermissionDialog: () -> Unit,
) {
    if (filterSheetRequest != null) {
        FilterBottomSheet(
            state = filterSheetRequest.state,
            onDismiss = onDismissFilterSheet,
            onApply = { filters ->
                filterSheetRequest.onApply(filters)
                onDismissFilterSheet()
            },
        )
    }
    if (moveConfirmRequest != null) {
        MoveConfirmDialog(
            state = moveConfirmRequest.state,
            onDismiss = onDismissMoveConfirm,
            onConfirm = {
                moveConfirmRequest.onConfirm()
                onDismissMoveConfirm()
            },
        )
    }
    if (moveProgressState != null) {
        MoveProgressScreen(
            state = moveProgressState,
            onDismiss = onDismissMoveProgress,
        )
    }
    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = onDismissPermissionDialog,
            onConfirm = onConfirmPermissionDialog,
        )
    }
}
