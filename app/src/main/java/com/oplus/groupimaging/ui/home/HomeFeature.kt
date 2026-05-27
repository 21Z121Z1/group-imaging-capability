package com.oplus.groupimaging.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.oplus.groupimaging.domain.HomeSummary
import com.oplus.groupimaging.domain.LensClass
import com.oplus.groupimaging.domain.ScanJob
import com.oplus.groupimaging.navigation.InsightFilterSeed
import com.oplus.groupimaging.ui.base.BaseViewModel
import com.oplus.groupimaging.ui.base.UiAction
import com.oplus.groupimaging.ui.base.UiEffect
import com.oplus.groupimaging.ui.base.UiState
import com.oplus.groupimaging.ui.components.AppTopBar
import com.oplus.groupimaging.ui.components.EmptyStateView
import com.oplus.groupimaging.ui.components.ErrorStateView
import com.oplus.groupimaging.ui.components.HeroSummaryCard
import com.oplus.groupimaging.ui.components.SectionContainer
import com.oplus.groupimaging.ui.components.StatCard
import com.oplus.groupimaging.testing.TestTags
import com.oplus.groupimaging.domain.usecase.ObserveHomeSummary
import com.oplus.groupimaging.domain.usecase.ObserveScanProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val hasIndex: Boolean = false,
    val summary: HomeSummary? = null,
    val scanStatus: ScanJob? = null,
    val error: String? = null,
) : UiState

sealed interface HomeAction : UiAction {
    data object OnRefresh : HomeAction
    data object OnStartScanClick : HomeAction
    data object OnScanStatusClick : HomeAction
    data class OnLensClick(val lens: LensClass) : HomeAction
    data object OnLiveClick : HomeAction
    data object OnRawClick : HomeAction
}

sealed interface HomeEffect : UiEffect {
    data object NavigateToScanOnboarding : HomeEffect
    data object NavigateToIncrementalScan : HomeEffect
    data object ShowScanStatusSheet : HomeEffect
    data class NavigateToInsightWithFilter(val filter: InsightFilterSeed) : HomeEffect
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeHomeSummary: ObserveHomeSummary,
    private val observeScanProgress: ObserveScanProgress,
) : BaseViewModel<HomeUiState, HomeAction, HomeEffect>(HomeUiState(isLoading = true)) {
    init {
        refresh()
    }

    override fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnRefresh -> emitEffect(HomeEffect.NavigateToIncrementalScan)
            HomeAction.OnScanStatusClick -> emitEffect(HomeEffect.ShowScanStatusSheet)
            HomeAction.OnStartScanClick -> emitEffect(HomeEffect.NavigateToScanOnboarding)
            is HomeAction.OnLensClick -> emitEffect(HomeEffect.NavigateToInsightWithFilter(InsightFilterSeed(lenses = setOf(action.lens))))
            HomeAction.OnLiveClick -> emitEffect(HomeEffect.NavigateToInsightWithFilter(InsightFilterSeed(isLiveOnly = true)))
            HomeAction.OnRawClick -> emitEffect(HomeEffect.NavigateToInsightWithFilter(InsightFilterSeed(isRawOnly = true)))
        }
    }

    fun refresh() {
        launch {
            updateState { copy(isLoading = true, error = null) }
            runCatching {
                val home = observeHomeSummary()
                val scan = observeScanProgress()
                updateState {
                    copy(
                        isLoading = false,
                        hasIndex = home.totalCaptures > 0,
                        summary = home,
                        scanStatus = scan,
                    )
                }
            }.onFailure { error ->
                updateState { copy(isLoading = false, error = error.message) }
            }
        }
    }
}

@Composable
fun HomeRoute(
    contentPadding: PaddingValues,
    onNavigateToScanOnboarding: () -> Unit,
    onNavigateToIncrementalScan: () -> Unit,
    onNavigateToInsight: (InsightFilterSeed) -> Unit,
    onShowScanStatus: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeEffect.NavigateToScanOnboarding -> onNavigateToScanOnboarding()
                HomeEffect.NavigateToIncrementalScan -> onNavigateToIncrementalScan()
                is HomeEffect.NavigateToInsightWithFilter -> onNavigateToInsight(effect.filter)
                HomeEffect.ShowScanStatusSheet -> onShowScanStatus()
            }
        }
    }
    HomeScreen(
        state = state,
        contentPadding = contentPadding,
        onAction = viewModel::onAction,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    contentPadding: PaddingValues,
    onAction: (HomeAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(TestTags.Screen.HOME),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AppTopBar(
                title = "你的摄影库",
                subtitle = state.scanStatus?.let { "最近一次扫描：${it.scannedCount}/${it.totalCount}" } ?: "等待扫描",
            )
        }
        when {
            state.error != null -> item {
                ErrorStateView("索引读取失败", state.error, onRetry = { onAction(HomeAction.OnRefresh) })
            }
            !state.hasIndex -> item {
                EmptyStateView(
                    title = "还没有建立摄影索引",
                    body = "扫描本机拍摄的照片后，这里会显示镜头、焦距、RAW、Live 和机型趋势。",
                    primaryAction = "开始扫描",
                    onPrimaryAction = { onAction(HomeAction.OnStartScanClick) },
                    modifier = Modifier.testTag(TestTags.Home.EMPTY_STATE),
                    primaryActionModifier = Modifier.testTag(TestTags.Home.START_SCAN),
                )
            }
            else -> {
                item {
                    HeroSummaryCard(
                        title = "总照片数",
                        headline = state.summary?.totalCaptures?.toString() ?: "0",
                        supporting = "日均 ${"%.1f".format(state.summary?.averageDailyCaptures ?: 0.0)} · 上次扫描状态 ${state.scanStatus?.status ?: "未知"}",
                        modifier = Modifier.testTag(TestTags.Home.TOTAL_CARD),
                        onClick = { onAction(HomeAction.OnScanStatusClick) },
                    )
                }
                item {
                    SectionContainer("镜头与焦距", actionLabel = "查看详情", onAction = {
                        onAction(HomeAction.OnLensClick(LensClass.MAIN))
                    }) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("主摄", state.summary?.lensBreakdown?.find { it.label == LensClass.MAIN.name }?.count?.toString() ?: "0", Modifier.weight(1f).testTag(TestTags.Home.MAIN_LENS)) {
                                onAction(HomeAction.OnLensClick(LensClass.MAIN))
                            }
                            StatCard("超广角", state.summary?.lensBreakdown?.find { it.label == LensClass.ULTRA_WIDE.name }?.count?.toString() ?: "0", Modifier.weight(1f).testTag(TestTags.Home.ULTRA_WIDE_LENS)) {
                                onAction(HomeAction.OnLensClick(LensClass.ULTRA_WIDE))
                            }
                            StatCard("长焦", state.summary?.lensBreakdown?.find { it.label == LensClass.TELE.name }?.count?.toString() ?: "0", Modifier.weight(1f).testTag(TestTags.Home.TELE_LENS)) {
                                onAction(HomeAction.OnLensClick(LensClass.TELE))
                            }
                        }
                    }
                }
                item {
                    SectionContainer("Live / RAW") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Live", "${state.summary?.liveRatio ?: 0.0}%", Modifier.weight(1f).testTag(TestTags.Home.LIVE_CARD)) { onAction(HomeAction.OnLiveClick) }
                            StatCard("RAW", "${state.summary?.rawRatio ?: 0.0}%", Modifier.weight(1f).testTag(TestTags.Home.RAW_CARD)) { onAction(HomeAction.OnRawClick) }
                        }
                    }
                }
                item {
                    SectionContainer("机型与趋势") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.summary?.deviceBreakdown?.take(3)?.forEach { bucket ->
                                StatCard(bucket.label, bucket.count.toString())
                            }
                        }
                    }
                }
                item {
                    SectionContainer("快捷操作") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("增量扫描", "执行", Modifier.weight(1f).testTag(TestTags.Home.INCREMENTAL_SCAN)) { onAction(HomeAction.OnRefresh) }
                            StatCard("扫描状态", "查看", Modifier.weight(1f).testTag(TestTags.Home.SCAN_STATUS)) { onAction(HomeAction.OnScanStatusClick) }
                        }
                    }
                }
            }
        }
    }
}
