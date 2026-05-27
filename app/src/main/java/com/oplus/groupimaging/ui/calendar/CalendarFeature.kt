package com.oplus.groupimaging.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.oplus.groupimaging.domain.InsightBucket
import com.oplus.groupimaging.domain.usecase.ObserveCalendarData
import com.oplus.groupimaging.navigation.InsightFilterSeed
import com.oplus.groupimaging.ui.base.BaseViewModel
import com.oplus.groupimaging.ui.base.UiAction
import com.oplus.groupimaging.ui.base.UiEffect
import com.oplus.groupimaging.ui.base.UiState
import com.oplus.groupimaging.ui.components.AppTopBar
import com.oplus.groupimaging.ui.components.ErrorStateView
import com.oplus.groupimaging.ui.components.MonthGrid
import com.oplus.groupimaging.ui.components.SectionContainer
import com.oplus.groupimaging.ui.components.StatCard
import com.oplus.groupimaging.testing.TestTags
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject

enum class CalendarMode { YEAR, MONTH, DAY }

data class CalendarUiState(
    val isLoading: Boolean = false,
    val mode: CalendarMode = CalendarMode.MONTH,
    val selectedDate: LocalDate? = null,
    val monthData: List<InsightBucket> = emptyList(),
    val visibleBuckets: List<InsightBucket> = emptyList(),
    val daySummary: InsightBucket? = null,
    val error: String? = null,
) : UiState

sealed interface CalendarAction : UiAction {
    data class OnModeChange(val mode: CalendarMode) : CalendarAction
    data class OnDateClick(val bucket: InsightBucket) : CalendarAction
    data object OnViewDayStatsClick : CalendarAction
}

sealed interface CalendarEffect : UiEffect {
    data class NavigateToInsightWithDate(val filter: InsightFilterSeed) : CalendarEffect
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val observeCalendarData: ObserveCalendarData,
) : BaseViewModel<CalendarUiState, CalendarAction, CalendarEffect>(CalendarUiState(isLoading = true)) {
    init {
        launch {
            runCatching {
                val data = observeCalendarData()
                updateState {
                    copy(
                        isLoading = false,
                        monthData = data,
                        visibleBuckets = visibleCalendarBuckets(data, mode),
                    )
                }
            }.onFailure { updateState { copy(isLoading = false, error = it.message) } }
        }
    }

    override fun onAction(action: CalendarAction) {
        when (action) {
            is CalendarAction.OnModeChange -> updateState {
                copy(
                    mode = action.mode,
                    selectedDate = null,
                    daySummary = null,
                    visibleBuckets = visibleCalendarBuckets(monthData, action.mode),
                )
            }
            is CalendarAction.OnDateClick -> updateState {
                copy(
                    selectedDate = if (mode == CalendarMode.DAY) {
                        runCatching { LocalDate.parse(action.bucket.label) }.getOrNull()
                    } else {
                        null
                    },
                    daySummary = action.bucket,
                )
            }
            CalendarAction.OnViewDayStatsClick -> {
                currentState().toInsightSeed()?.let { seed ->
                    emitEffect(CalendarEffect.NavigateToInsightWithDate(seed))
                }
            }
        }
    }

    private fun CalendarUiState.toInsightSeed(): InsightFilterSeed? {
        val selected = daySummary ?: return null
        return when (mode) {
            CalendarMode.YEAR -> selected.label.toIntOrNull()
                ?.let { InsightFilterSeed(years = setOf(it)) }
            CalendarMode.MONTH -> selected.label.takeIf(::isYearMonthLabel)
                ?.let { InsightFilterSeed(yearMonths = setOf(it)) }
            CalendarMode.DAY -> selectedDate
                ?.let { InsightFilterSeed(dates = setOf(it)) }
        }
    }
}

@Composable
fun CalendarRoute(
    contentPadding: PaddingValues,
    onNavigateToInsight: (InsightFilterSeed) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CalendarEffect.NavigateToInsightWithDate -> onNavigateToInsight(effect.filter)
            }
        }
    }
    CalendarScreen(state, contentPadding, viewModel::onAction)
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    contentPadding: PaddingValues,
    onAction: (CalendarAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(TestTags.Screen.CALENDAR),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppTopBar("日历", "按年 / 月 / 日复盘拍摄频率") }
        if (state.error != null) {
            item {
                ErrorStateView(
                    title = "日历索引加载失败",
                    body = state.error,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalendarMode.entries.forEach { mode ->
                    StatCard(mode.name, if (state.mode == mode) "当前" else "切换", Modifier.weight(1f).testTag(TestTags.Calendar.mode(mode.name))) {
                        onAction(CalendarAction.OnModeChange(mode))
                    }
                }
            }
        }
        item {
            SectionContainer(
                when (state.mode) {
                    CalendarMode.YEAR -> "年度分布"
                    CalendarMode.MONTH -> "月视图"
                    CalendarMode.DAY -> "日视图"
                },
            ) {
                MonthGrid(items = state.visibleBuckets.take(12)) { bucket ->
                    onAction(CalendarAction.OnDateClick(bucket))
                }
            }
        }
        if (state.daySummary != null) {
            item {
                SectionContainer(
                    title = when (state.mode) {
                        CalendarMode.YEAR -> "年度摘要"
                        CalendarMode.MONTH -> "月份摘要"
                        CalendarMode.DAY -> "当天摘要"
                    },
                    actionLabel = "查看统计",
                    onAction = { onAction(CalendarAction.OnViewDayStatsClick) },
                    actionModifier = Modifier.testTag(TestTags.Calendar.VIEW_DAY_STATS),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(state.daySummary.label, "${state.daySummary.count} 张")
                    }
                }
            }
        }
    }
}

private fun visibleCalendarBuckets(
    dayBuckets: List<InsightBucket>,
    mode: CalendarMode,
): List<InsightBucket> = when (mode) {
    CalendarMode.YEAR -> aggregateCalendarBuckets(dayBuckets, labelLength = 4)
    CalendarMode.MONTH -> aggregateCalendarBuckets(dayBuckets, labelLength = 7)
    CalendarMode.DAY -> dayBuckets
        .filter { runCatching { LocalDate.parse(it.label) }.isSuccess }
        .sortedByDescending { it.label }
}

private fun aggregateCalendarBuckets(
    dayBuckets: List<InsightBucket>,
    labelLength: Int,
): List<InsightBucket> =
    dayBuckets
        .mapNotNull { bucket ->
            runCatching { LocalDate.parse(bucket.label) }
                .getOrNull()
                ?.let { bucket.label.take(labelLength) to bucket.count }
        }
        .groupBy({ it.first }, { it.second })
        .map { (label, counts) -> InsightBucket(label, counts.sum()) }
        .sortedByDescending { it.label }

private fun isYearMonthLabel(value: String): Boolean =
    value.length == 7 && runCatching { java.time.YearMonth.parse(value) }.isSuccess
