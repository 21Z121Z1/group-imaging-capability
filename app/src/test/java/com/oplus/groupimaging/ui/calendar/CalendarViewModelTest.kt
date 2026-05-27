package com.oplus.groupimaging.ui.calendar

import com.oplus.groupimaging.MainDispatcherRule
import com.oplus.groupimaging.TestRepository
import com.oplus.groupimaging.domain.usecase.ObserveCalendarData
import com.oplus.groupimaging.navigation.InsightFilterSeed
import com.oplus.groupimaging.testing.InsightTestFixtures
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads month buckets`() = runTest {
        val repository = TestRepository().apply {
            calendarSummary = InsightTestFixtures.calendarBuckets()
        }

        val viewModel = CalendarViewModel(
            observeCalendarData = ObserveCalendarData(repository),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(4, viewModel.uiState.value.monthData.size)
        assertEquals("2026-03-21", viewModel.uiState.value.monthData.first().label)
    }

    @Test
    fun `year mode aggregates daily buckets by year`() = runTest {
        val repository = TestRepository().apply {
            calendarSummary = InsightTestFixtures.calendarBucketsSpanningYears()
        }

        val viewModel = CalendarViewModel(
            observeCalendarData = ObserveCalendarData(repository),
        )
        advanceUntilIdle()

        viewModel.onAction(CalendarAction.OnModeChange(CalendarMode.YEAR))

        assertEquals(
            listOf(
                InsightTestFixtures.bucket("2026", 46),
                InsightTestFixtures.bucket("2025", 4),
            ),
            viewModel.uiState.value.visibleBuckets,
        )
    }

    @Test
    fun `month mode aggregates daily buckets by month`() = runTest {
        val repository = TestRepository().apply {
            calendarSummary = InsightTestFixtures.calendarBucketsSpanningYears()
        }

        val viewModel = CalendarViewModel(
            observeCalendarData = ObserveCalendarData(repository),
        )
        advanceUntilIdle()

        assertEquals(
            listOf(
                InsightTestFixtures.bucket("2026-04", 6),
                InsightTestFixtures.bucket("2026-03", 40),
                InsightTestFixtures.bucket("2025-12", 4),
            ),
            viewModel.uiState.value.visibleBuckets,
        )
    }

    @Test
    fun `load failure surfaces error state`() = runTest {
        val repository = TestRepository().apply {
            calendarSummaryFailure = IllegalStateException("calendar index missing")
        }

        val viewModel = CalendarViewModel(
            observeCalendarData = ObserveCalendarData(repository),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("calendar index missing", viewModel.uiState.value.error)
    }

    @Test
    fun `date click and view stats emits insight navigation seed`() = runTest {
        val repository = TestRepository()
        val viewModel = CalendarViewModel(
            observeCalendarData = ObserveCalendarData(repository),
        )
        advanceUntilIdle()

        val bucket = InsightTestFixtures.bucket("2026-03-23", 31)
        val effectDeferred = async { viewModel.effect.first() }

        viewModel.onAction(CalendarAction.OnModeChange(CalendarMode.DAY))
        viewModel.onAction(CalendarAction.OnDateClick(bucket))
        viewModel.onAction(CalendarAction.OnViewDayStatsClick)

        assertEquals(LocalDate.parse("2026-03-23"), viewModel.uiState.value.selectedDate)
        assertEquals(
            CalendarEffect.NavigateToInsightWithDate(InsightFilterSeed(dates = setOf(LocalDate.parse("2026-03-23")))),
            effectDeferred.await(),
        )
    }

    @Test
    fun `calendar bucket navigation emits matching insight seed`() = runTest {
        val repository = TestRepository().apply {
            calendarSummary = InsightTestFixtures.calendarBucketsSpanningYears()
        }
        val viewModel = CalendarViewModel(
            observeCalendarData = ObserveCalendarData(repository),
        )
        advanceUntilIdle()

        val yearEffect = async { viewModel.effect.first() }
        viewModel.onAction(CalendarAction.OnModeChange(CalendarMode.YEAR))
        viewModel.onAction(CalendarAction.OnDateClick(InsightTestFixtures.bucket("2026", 46)))
        viewModel.onAction(CalendarAction.OnViewDayStatsClick)
        assertEquals(
            CalendarEffect.NavigateToInsightWithDate(InsightFilterSeed(years = setOf(2026))),
            yearEffect.await(),
        )

        val monthEffect = async { viewModel.effect.first() }
        viewModel.onAction(CalendarAction.OnModeChange(CalendarMode.MONTH))
        viewModel.onAction(CalendarAction.OnDateClick(InsightTestFixtures.bucket("2026-03", 40)))
        viewModel.onAction(CalendarAction.OnViewDayStatsClick)
        assertEquals(
            CalendarEffect.NavigateToInsightWithDate(InsightFilterSeed(yearMonths = setOf("2026-03"))),
            monthEffect.await(),
        )

        val dayEffect = async { viewModel.effect.first() }
        viewModel.onAction(CalendarAction.OnModeChange(CalendarMode.DAY))
        viewModel.onAction(CalendarAction.OnDateClick(InsightTestFixtures.bucket("2026-03-21", 12)))
        viewModel.onAction(CalendarAction.OnViewDayStatsClick)
        assertEquals(
            CalendarEffect.NavigateToInsightWithDate(
                InsightFilterSeed(dates = setOf(LocalDate.parse("2026-03-21"))),
            ),
            dayEffect.await(),
        )
    }
}
