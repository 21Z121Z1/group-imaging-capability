package com.oplus.groupimaging.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oplus.groupimaging.domain.ScanType
import com.oplus.groupimaging.ui.album.groups.AlbumGroupsRoute
import com.oplus.groupimaging.ui.album.preview.MoveProgressUiState
import com.oplus.groupimaging.ui.album.preview.RulePreviewRoute
import com.oplus.groupimaging.ui.calendar.CalendarRoute
import com.oplus.groupimaging.ui.deviceprofiles.DeviceProfilesRoute
import com.oplus.groupimaging.ui.failed.FailedItemsRoute
import com.oplus.groupimaging.ui.home.HomeRoute
import com.oplus.groupimaging.ui.insight.FilterSheetUiState
import com.oplus.groupimaging.ui.insight.InsightRoute
import com.oplus.groupimaging.ui.scan.onboarding.ScanOnboardingRoute
import com.oplus.groupimaging.ui.scan.progress.ScanProgressRoute
import com.oplus.groupimaging.ui.settings.DirectoryManagerRoute
import com.oplus.groupimaging.ui.settings.ScopeExplanationRoute
import com.oplus.groupimaging.ui.settings.SettingsRoute
import com.oplus.groupimaging.ui.theme.OplusInsightTheme
import androidx.compose.material3.SnackbarHostState

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
    val topLevelDestinations = remember(navController) {
        topLevelRouteDefinitions.map { (route, label, icon) ->
            TopLevelDestination(
                route = route,
                label = label,
                icon = icon,
                onClick = {
                    navController.navigate(route.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
    var filterSheetRequest by remember { mutableStateOf<FilterSheetRequest?>(null) }
    var moveConfirmRequest by remember { mutableStateOf<MoveConfirmRequest?>(null) }
    var moveProgressState by rememberSaveable(stateSaver = nullableMoveProgressUiStateSaver()) {
        mutableStateOf<MoveProgressUiState?>(null)
    }
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = hasMediaReadAccess(context) || results.any { (permission, isGranted) ->
            isGranted && permission in mediaReadPermissions()
        }
        if (granted) {
            showPermissionDialog = false
            pendingPermissionAction?.invoke()
            pendingPermissionAction = null
        } else {
            showPermissionDialog = true
        }
    }
    val requestMediaPermissionThen: (() -> Unit) -> Unit = { action ->
        if (hasMediaReadAccess(context)) {
            action()
        } else {
            pendingPermissionAction = action
            permissionLauncher.launch(mediaReadPermissions())
        }
    }

    OplusInsightTheme {
        MainScaffold(
            currentRoute = currentDestination,
            snackbarHostState = snackbarHostState,
            topLevelDestinations = topLevelDestinations,
            content = { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Home.route,
                ) {
                    composable(AppRoute.Home.route) {
                        HomeRoute(
                            contentPadding = innerPadding,
                            onNavigateToScanOnboarding = { navController.navigate(AppRoute.ScanOnboarding.route) },
                            onNavigateToIncrementalScan = {
                                requestMediaPermissionThen {
                                    navController.navigate(AppRoute.ScanProgress.create(ScanType.INCREMENTAL))
                                }
                            },
                            onNavigateToInsight = { seed ->
                                navController.currentBackStackEntry?.savedStateHandle?.set(InsightSeedKey, seed)
                                navController.navigate(AppRoute.Insight.route)
                            },
                            onShowScanStatus = { navController.navigate(AppRoute.ScanProgress.create(null)) },
                        )
                    }
                    composable(AppRoute.Calendar.route) {
                        CalendarRoute(
                            contentPadding = innerPadding,
                            onNavigateToInsight = { seed ->
                                navController.currentBackStackEntry?.savedStateHandle?.set(InsightSeedKey, seed)
                                navController.navigate(AppRoute.Insight.route)
                            },
                        )
                    }
                    composable(AppRoute.Insight.route) {
                        val initialSeed = navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.remove<InsightFilterSeed>(InsightSeedKey)
                        InsightRoute(
                            contentPadding = innerPadding,
                            initialSeed = initialSeed,
                            onOpenFilterSheet = { request -> filterSheetRequest = request },
                        )
                    }
                    composable(AppRoute.AlbumGroups.route) {
                        AlbumGroupsRoute(
                            contentPadding = innerPadding,
                            onNavigateToRulePreview = { ruleId -> navController.navigate(AppRoute.RulePreview.create(ruleId)) },
                        )
                    }
                    composable(AppRoute.Settings.route) {
                        SettingsRoute(
                            contentPadding = innerPadding,
                            onNavigateToFailedItems = { navController.navigate(AppRoute.FailedItems.route) },
                            onNavigateToDeviceProfiles = { navController.navigate(AppRoute.DeviceProfiles.route) },
                            onNavigateToDirectoryManager = { navController.navigate(AppRoute.DirectoryManager.route) },
                            onNavigateToScopeExplanation = { navController.navigate(AppRoute.ScopeExplanation.route) },
                            onNavigateToFullScan = {
                                requestMediaPermissionThen {
                                    navController.navigate(AppRoute.ScanProgress.create(ScanType.FULL))
                                }
                            },
                        )
                    }
                    composable(AppRoute.ScanOnboarding.route) {
                        ScanOnboardingRoute(
                            contentPadding = innerPadding,
                            onStartScan = {
                                requestMediaPermissionThen {
                                    navController.navigate(AppRoute.ScanProgress.create(ScanType.FULL))
                                }
                            },
                        )
                    }
                    composable(
                        route = AppRoute.ScanProgress.route,
                        arguments = listOf(
                            navArgument("scanType") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) {
                        ScanProgressRoute(
                            contentPadding = innerPadding,
                            onFinish = {
                                val popped = navController.popBackStack(AppRoute.Home.route, inclusive = false)
                                if (!popped) {
                                    navController.navigate(AppRoute.Home.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            onNavigateToFailedItems = { navController.navigate(AppRoute.FailedItems.route) },
                        )
                    }
                    composable(AppRoute.RulePreview.route) { backStackEntry ->
                        RulePreviewRoute(
                            contentPadding = innerPadding,
                            ruleId = backStackEntry.arguments?.getString("ruleId").orEmpty(),
                            onBack = { navController.popBackStack() },
                            onShowMoveConfirm = { request -> moveConfirmRequest = request },
                            onMoveProgress = { state -> moveProgressState = state },
                        )
                    }
                    composable(AppRoute.FailedItems.route) {
                        FailedItemsRoute(contentPadding = innerPadding)
                    }
                    composable(AppRoute.DeviceProfiles.route) {
                        DeviceProfilesRoute(contentPadding = innerPadding)
                    }
                    composable(AppRoute.DirectoryManager.route) {
                        DirectoryManagerRoute(contentPadding = innerPadding)
                    }
                    composable(AppRoute.ScopeExplanation.route) {
                        ScopeExplanationRoute(contentPadding = innerPadding)
                    }
                }
            },
        )

        GlobalSheetsAndDialogs(
            filterSheetRequest = filterSheetRequest,
            onDismissFilterSheet = { filterSheetRequest = null },
            moveConfirmRequest = moveConfirmRequest,
            onDismissMoveConfirm = { moveConfirmRequest = null },
            moveProgressState = moveProgressState,
            onDismissMoveProgress = { moveProgressState = null },
            showPermissionDialog = showPermissionDialog,
            onDismissPermissionDialog = { showPermissionDialog = false },
            onConfirmPermissionDialog = { permissionLauncher.launch(mediaReadPermissions()) },
        )
    }
}

private fun mediaReadPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun hasMediaReadAccess(context: Context): Boolean =
    mediaReadPermissions().any { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

private fun nullableMoveProgressUiStateSaver(): Saver<MoveProgressUiState?, Map<String, Any?>> =
    Saver(
        save = { state: MoveProgressUiState? ->
            if (state == null) {
                mapOf("present" to false)
            } else {
                mapOf(
                    "present" to true,
                    "progress" to state.progress,
                    "movedCount" to state.movedCount,
                    "failedCount" to state.failedCount,
                    "totalCount" to state.totalCount,
                    "currentFileName" to state.currentFileName,
                    "isCompleted" to state.isCompleted,
                )
            }
        },
        restore = { saved: Map<String, Any?> ->
            if (saved["present"] != true) {
                null
            } else {
                MoveProgressUiState(
                    progress = saved.getValue("progress") as Float,
                    movedCount = saved.getValue("movedCount") as Int,
                    failedCount = saved.getValue("failedCount") as Int,
                    totalCount = saved.getValue("totalCount") as Int,
                    currentFileName = saved["currentFileName"] as String?,
                    isCompleted = saved.getValue("isCompleted") as Boolean,
                )
            }
        },
    )
