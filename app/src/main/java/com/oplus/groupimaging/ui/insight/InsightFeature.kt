package com.oplus.groupimaging.ui.insight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oplus.groupimaging.domain.FilterSpec
import com.oplus.groupimaging.domain.InsightBucket
import com.oplus.groupimaging.domain.InsightDimension
import com.oplus.groupimaging.domain.LensClass
import com.oplus.groupimaging.domain.usecase.ObserveInsight
import com.oplus.groupimaging.navigation.FilterSheetRequest
import com.oplus.groupimaging.navigation.InsightFilterSeed
import com.oplus.groupimaging.ui.base.BaseViewModel
import com.oplus.groupimaging.ui.base.UiAction
import com.oplus.groupimaging.ui.base.UiEffect
import com.oplus.groupimaging.ui.base.UiState
import com.oplus.groupimaging.ui.components.AppTopBar
import com.oplus.groupimaging.ui.components.EmptyStateView
import com.oplus.groupimaging.ui.components.ErrorStateView
import com.oplus.groupimaging.ui.components.FilterChipRow
import com.oplus.groupimaging.ui.components.SectionContainer
import com.oplus.groupimaging.ui.components.StatCard
import com.oplus.groupimaging.ui.components.TrendMiniChartCard
import com.oplus.groupimaging.testing.TestTags
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class FocalRangeUi(val label: String)

data class InsightFiltersUi(
    val years: Set<Int> = emptySet(),
    val months: Set<YearMonth> = emptySet(),
    val dates: Set<LocalDate> = emptySet(),
    val devices: Set<String> = emptySet(),
    val lenses: Set<LensClass> = emptySet(),
    val isLiveOnly: Boolean = false,
    val isRawOnly: Boolean = false,
    val focalRanges: Set<FocalRangeUi> = emptySet(),
)

data class InsightUiState(
    val isLoading: Boolean = false,
    val filters: InsightFiltersUi = InsightFiltersUi(),
    val resultCount: Int = 0,
    val lensStats: List<InsightBucket> = emptyList(),
    val focalStats: List<InsightBucket> = emptyList(),
    val liveStats: List<InsightBucket> = emptyList(),
    val rawStats: List<InsightBucket> = emptyList(),
    val deviceStats: List<InsightBucket> = emptyList(),
    val monthlyTrend: List<InsightBucket> = emptyList(),
    val yearlyTrend: List<InsightBucket> = emptyList(),
    val emptyReason: String? = null,
    val error: String? = null,
) : UiState

sealed interface InsightAction : UiAction {
    data object OnFilterClick : InsightAction
    data class OnRemoveFilterChip(val chipId: String) : InsightAction
    data object OnClearAllFilters : InsightAction
    data class OnLensSegmentClick(val lens: LensClass) : InsightAction
    data class OnFocalRangeClick(val range: FocalRangeUi) : InsightAction
    data object OnLiveCardClick : InsightAction
    data object OnRawCardClick : InsightAction
    data class OnApplyFilters(val filters: InsightFiltersUi) : InsightAction
}

sealed interface InsightEffect : UiEffect {
    data class ShowFilterSheet(val state: FilterSheetUiState) : InsightEffect
}

data class FilterSheetUiState(
    val draft: InsightFiltersUi = InsightFiltersUi(),
    val availableYears: List<Int> = emptyList(),
    val availableMonths: List<YearMonth> = emptyList(),
    val availableDevices: List<String> = emptyList(),
    val availableFocalRanges: List<FocalRangeUi> = emptyList(),
)

@HiltViewModel
class InsightViewModel @Inject constructor(
    private val observeInsight: ObserveInsight,
) : BaseViewModel<InsightUiState, InsightAction, InsightEffect>(InsightUiState(isLoading = true)) {
    init {
        load()
    }

    override fun onAction(action: InsightAction) {
        when (action) {
            InsightAction.OnFilterClick -> emitEffect(
                InsightEffect.ShowFilterSheet(
                    FilterSheetUiState(
                        draft = currentState().filters,
                        availableYears = currentState().yearlyTrend.mapNotNull { it.label.toIntOrNull() },
                        availableMonths = currentState().monthlyTrend.mapNotNull { runCatching { YearMonth.parse(it.label) }.getOrNull() }
                            .sortedDescending(),
                        availableDevices = currentState().deviceStats.map { it.label },
                        availableFocalRanges = currentState().focalStats.map { FocalRangeUi(it.label) },
                    ),
                ),
            )
            InsightAction.OnClearAllFilters -> {
                updateState { copy(filters = InsightFiltersUi()) }
                load()
            }
            is InsightAction.OnRemoveFilterChip -> {
                val next = currentState().filters.removeChip(action.chipId)
                updateState { copy(filters = next) }
                load()
            }
            is InsightAction.OnLensSegmentClick -> {
                updateState { copy(filters = filters.copy(lenses = setOf(action.lens))) }
                load()
            }
            is InsightAction.OnFocalRangeClick -> {
                updateState { copy(filters = filters.copy(focalRanges = setOf(action.range))) }
                load()
            }
            InsightAction.OnLiveCardClick -> {
                updateState { copy(filters = filters.copy(isLiveOnly = true)) }
                load()
            }
            InsightAction.OnRawCardClick -> {
                updateState { copy(filters = filters.copy(isRawOnly = true)) }
                load()
            }
            is InsightAction.OnApplyFilters -> {
                updateState { copy(filters = action.filters) }
                load()
            }
        }
    }

    fun applySeed(seed: InsightFilterSeed?) {
        if (seed == null) return
        updateState {
            copy(
                filters = filters.copy(
                    years = seed.years,
                    months = seed.yearMonths.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }.toSet(),
                    dates = seed.dates,
                    devices = seed.devices,
                    lenses = seed.lenses,
                    isLiveOnly = seed.isLiveOnly,
                    isRawOnly = seed.isRawOnly,
                    focalRanges = seed.focalRanges.map(::FocalRangeUi).toSet(),
                ),
            )
        }
        load()
    }

    private fun load() {
        launch {
            updateState { copy(isLoading = true) }
            val filterSpec = currentState().filters.toFilterSpec()
            runCatching {
                val results = coroutineScope {
                    val deferred = InsightDimension.entries.associateWith { dim ->
                        async { observeInsight(dim, filterSpec) }
                    }
                    deferred.mapValues { it.value.await() }
                }
                val lens = results[InsightDimension.LENS].orEmpty()
                val focal = results[InsightDimension.FOCAL_EQ].orEmpty()
                val live = results[InsightDimension.LIVE].orEmpty()
                val raw = results[InsightDimension.RAW].orEmpty()
                val device = results[InsightDimension.DEVICE].orEmpty()
                val monthly = results[InsightDimension.MONTH].orEmpty()
                val yearly = results[InsightDimension.YEAR].orEmpty()
                updateState {
                    copy(
                        isLoading = false,
                        resultCount = device.sumOf { it.count }.takeIf { it > 0 } ?: lens.sumOf { it.count },
                        lensStats = lens,
                        focalStats = focal,
                        liveStats = live,
                        rawStats = raw,
                        deviceStats = device,
                        monthlyTrend = monthly,
                        yearlyTrend = yearly,
                        emptyReason = if (lens.isEmpty() && device.isEmpty()) "没有符合当前条件的本机拍摄照片" else null,
                        error = null,
                    )
                }
            }.onFailure {
                updateState { copy(isLoading = false, error = it.message) }
            }
        }
    }
}

@Composable
fun InsightRoute(
    contentPadding: PaddingValues,
    initialSeed: InsightFilterSeed? = null,
    onOpenFilterSheet: (FilterSheetRequest) -> Unit,
    viewModel: InsightViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialSeed) {
        viewModel.applySeed(initialSeed)
    }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is InsightEffect.ShowFilterSheet -> onOpenFilterSheet(
                    FilterSheetRequest(
                        state = effect.state,
                        onApply = { filters -> viewModel.onAction(InsightAction.OnApplyFilters(filters)) },
                    ),
                )
            }
        }
    }
    InsightScreen(state, contentPadding, viewModel::onAction)
}

@Composable
fun InsightScreen(
    state: InsightUiState,
    contentPadding: PaddingValues,
    onAction: (InsightAction) -> Unit,
) {
    val chips = buildList {
        addAll(state.filters.years.map(Int::toString))
        addAll(state.filters.months.map(YearMonth::toString))
        addAll(state.filters.dates.map(LocalDate::toString))
        addAll(state.filters.devices)
        addAll(state.filters.lenses.map { it.name })
        addAll(state.filters.focalRanges.map { it.label })
        if (state.filters.isLiveOnly) add("Live")
        if (state.filters.isRawOnly) add("RAW")
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).testTag(TestTags.Screen.INSIGHT),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppTopBar("洞察", "当前命中 ${state.resultCount}") }
        item { FilterChipRow(chips = chips, onRemove = { onAction(InsightAction.OnRemoveFilterChip(it)) }, onClear = { onAction(InsightAction.OnClearAllFilters) }) }
        item {
            TextButton(
                onClick = { onAction(InsightAction.OnFilterClick) },
                modifier = Modifier.testTag(TestTags.Insight.OPEN_FILTER),
            ) { Text("打开筛选") }
        }
        when {
            state.error != null -> item {
                ErrorStateView(
                    title = "洞察读取失败",
                    body = state.error.orEmpty(),
                )
            }
            state.emptyReason != null -> item {
                EmptyStateView(
                    "没有结果",
                    state.emptyReason.orEmpty(),
                    primaryAction = "清空筛选",
                    onPrimaryAction = { onAction(InsightAction.OnClearAllFilters) },
                )
            }
            else -> {
                item {
                    SectionContainer("镜头类别分布") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.lensStats.forEach { bucket ->
                                StatCard(bucket.label, bucket.count.toString()) {
                                    bucket.label.toLensClassOrNull()?.let { onAction(InsightAction.OnLensSegmentClick(it)) }
                                }
                            }
                        }
                    }
                }
                item {
                    SectionContainer("等效焦距分布") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.focalStats.forEach { bucket ->
                                StatCard(bucket.label, bucket.count.toString()) { onAction(InsightAction.OnFocalRangeClick(FocalRangeUi(bucket.label))) }
                            }
                        }
                    }
                }
                item {
                    SectionContainer("Live / RAW") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.liveStats.forEach { bucket -> StatCard(bucket.label, bucket.count.toString()) { onAction(InsightAction.OnLiveCardClick) } }
                            state.rawStats.forEach { bucket -> StatCard(bucket.label, bucket.count.toString()) { onAction(InsightAction.OnRawCardClick) } }
                        }
                    }
                }
                item { TrendMiniChartCard("月度趋势", state.monthlyTrend) }
                item { TrendMiniChartCard("年度趋势", state.yearlyTrend) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    state: FilterSheetUiState,
    onDismiss: () -> Unit,
    onApply: (InsightFiltersUi) -> Unit,
) {
    var draft by rememberSaveable(stateSaver = insightFiltersUiSaver()) {
        mutableStateOf(state.draft)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.semantics { paneTitle = "筛选" },
    ) {
        FilterSheetContent(
            state = state,
            draft = draft,
            onDraftChange = { draft = it },
            onDismiss = onDismiss,
            onApply = onApply,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterSheetContent(
    state: FilterSheetUiState,
    draft: InsightFiltersUi,
    onDraftChange: (InsightFiltersUi) -> Unit,
    onDismiss: () -> Unit,
    onApply: (InsightFiltersUi) -> Unit,
) {
    val chipModifier = Modifier.heightIn(min = 48.dp)
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag(TestTags.Insight.FILTER_BOTTOM_SHEET),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("筛选")
        Text("年份")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.availableYears.forEach { year ->
                AssistChip(
                    onClick = { onDraftChange(draft.toggleYear(year)) },
                    label = { Text(if (year in draft.years) "$year 已选" else year.toString()) },
                    modifier = chipModifier,
                )
            }
        }
        Text("月份")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.availableMonths.forEach { month ->
                AssistChip(
                    onClick = { onDraftChange(draft.toggleMonth(month)) },
                    label = { Text(if (month in draft.months) "$month 已选" else month.toString()) },
                    modifier = chipModifier,
                )
            }
        }
        Text("设备")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.availableDevices.forEach { device ->
                AssistChip(
                    onClick = { onDraftChange(draft.toggleDevice(device)) },
                    label = { Text(if (device in draft.devices) "$device 已选" else device) },
                    modifier = chipModifier,
                )
            }
        }
        Text("焦段")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.availableFocalRanges.forEach { range ->
                AssistChip(
                    onClick = { onDraftChange(draft.toggleFocalRange(range)) },
                    label = { Text(if (range in draft.focalRanges) "${range.label} 已选" else range.label) },
                    modifier = chipModifier,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LensClass.entries.filterNot { it == LensClass.UNKNOWN }.forEach { lens ->
                AssistChip(
                    onClick = { onDraftChange(draft.toggleLens(lens)) },
                    label = { Text(if (lens in draft.lenses) "${lens.name} 已选" else lens.name) },
                    modifier = chipModifier,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { onDraftChange(draft.copy(isLiveOnly = !draft.isLiveOnly)) },
                label = { Text(if (draft.isLiveOnly) "仅 Live" else "Live") },
                modifier = chipModifier,
            )
            AssistChip(
                onClick = { onDraftChange(draft.copy(isRawOnly = !draft.isRawOnly)) },
                label = { Text(if (draft.isRawOnly) "仅 RAW" else "RAW") },
                modifier = chipModifier,
            )
        }
        TextButton(
            onClick = { onApply(draft) },
            modifier = Modifier.testTag(TestTags.Insight.FILTER_APPLY),
        ) { Text("应用筛选") }
        TextButton(onClick = onDismiss) { Text("关闭") }
    }
}

private fun insightFiltersUiSaver() = listSaver<InsightFiltersUi, Any>(
    save = {
        listOf(
            it.years.toList(),
            it.months.map(YearMonth::toString),
            it.dates.map(LocalDate::toString),
            it.devices.toList(),
            it.lenses.map(LensClass::name),
            it.isLiveOnly,
            it.isRawOnly,
            it.focalRanges.map(FocalRangeUi::label),
        )
    },
    restore = { restored ->
        InsightFiltersUi(
            years = restored.getOrNull(0).asIntList().toSet(),
            months = restored.getOrNull(1).asStringList().mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }.toSet(),
            dates = restored.getOrNull(2).asStringList().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet(),
            devices = restored.getOrNull(3).asStringList().toSet(),
            lenses = restored.getOrNull(4).asStringList().mapNotNull { runCatching { LensClass.valueOf(it) }.getOrNull() }.toSet(),
            isLiveOnly = restored.getOrNull(5) as? Boolean ?: false,
            isRawOnly = restored.getOrNull(6) as? Boolean ?: false,
            focalRanges = restored.getOrNull(7).asStringList().map(::FocalRangeUi).toSet(),
        )
    },
)

private fun Any?.asStringList(): List<String> =
    (this as? List<*>)?.mapNotNull { it as? String }.orEmpty()

private fun Any?.asIntList(): List<Int> =
    (this as? List<*>)?.mapNotNull { it as? Int }.orEmpty()

private fun String.toLensClassOrNull(): LensClass? = when (this) {
    LensClass.MAIN.name -> LensClass.MAIN
    LensClass.ULTRA_WIDE.name -> LensClass.ULTRA_WIDE
    LensClass.TELE.name -> LensClass.TELE
    else -> null
}

private fun InsightFiltersUi.toFilterSpec(): FilterSpec = FilterSpec(
    deviceModels = devices,
    lensClasses = lenses,
    liveOnly = isLiveOnly.takeIf { it },
    rawOnly = isRawOnly.takeIf { it },
    years = years,
    dates = dates,
    yearMonths = months.map(YearMonth::toString).toSet(),
    focalEqRangeLabels = focalRanges.map(FocalRangeUi::label).toSet(),
)

private fun InsightFiltersUi.removeChip(chipId: String): InsightFiltersUi = copy(
    years = years.filterNot { it.toString() == chipId }.toSet(),
    months = months.filterNot { it.toString() == chipId }.toSet(),
    dates = dates.filterNot { it.toString() == chipId }.toSet(),
    devices = devices - chipId,
    lenses = lenses.filterNot { it.name == chipId }.toSet(),
    isLiveOnly = if (chipId == "Live") false else isLiveOnly,
    isRawOnly = if (chipId == "RAW") false else isRawOnly,
    focalRanges = focalRanges.filterNot { it.label == chipId }.toSet(),
)

private fun InsightFiltersUi.toggleYear(year: Int): InsightFiltersUi =
    copy(years = years.toggle(year))

private fun InsightFiltersUi.toggleMonth(month: YearMonth): InsightFiltersUi =
    copy(months = months.toggle(month))

private fun InsightFiltersUi.toggleDevice(device: String): InsightFiltersUi =
    copy(devices = devices.toggle(device))

private fun InsightFiltersUi.toggleLens(lens: LensClass): InsightFiltersUi =
    copy(lenses = lenses.toggle(lens))

private fun InsightFiltersUi.toggleFocalRange(range: FocalRangeUi): InsightFiltersUi =
    copy(focalRanges = focalRanges.toggle(range))

private fun <T> Set<T>.toggle(item: T): Set<T> =
    if (item in this) this - item else this + item
