package com.oplus.groupimaging.ui.insight

import com.oplus.groupimaging.MainDispatcherRule
import com.oplus.groupimaging.TestRepository
import com.oplus.groupimaging.domain.InsightBucket
import com.oplus.groupimaging.domain.InsightDimension
import com.oplus.groupimaging.domain.LensClass
import com.oplus.groupimaging.domain.usecase.ObserveInsight
import com.oplus.groupimaging.navigation.InsightFilterSeed
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `apply seed updates filters and keeps exact date filter spec`() = runTest {
        val repository = TestRepository().apply {
            insightResults = InsightDimension.entries.associateWith {
                listOf(InsightBucket("sample", 1))
            }
        }
        val viewModel = InsightViewModel(ObserveInsight(repository))
        advanceUntilIdle()
        repository.insightRequests.clear()

        viewModel.applySeed(
            InsightFilterSeed(
                years = setOf(2026),
                dates = setOf(LocalDate.parse("2026-03-23")),
                devices = setOf("Find X8 Ultra"),
                lenses = setOf(LensClass.MAIN),
                isRawOnly = true,
                focalRanges = setOf("23–28mm"),
            ),
        )
        advanceUntilIdle()

        val lastFilterSpec = repository.insightRequests.last().second
        assertEquals(setOf("Find X8 Ultra"), lastFilterSpec.deviceModels)
        assertEquals(setOf(LensClass.MAIN), lastFilterSpec.lensClasses)
        assertEquals(true, lastFilterSpec.rawOnly)
        assertEquals(setOf(2026), lastFilterSpec.years)
        assertEquals(setOf(LocalDate.parse("2026-03-23")), lastFilterSpec.dates)
        assertEquals(emptySet<String>(), lastFilterSpec.yearMonths)
        assertEquals(setOf("23–28mm"), lastFilterSpec.focalEqRangeLabels)
        assertTrue(viewModel.uiState.value.filters.dates.contains(LocalDate.parse("2026-03-23")))
    }

    @Test
    fun `apply seed maps year month filters into filter spec`() = runTest {
        val repository = TestRepository().apply {
            insightResults = InsightDimension.entries.associateWith {
                listOf(InsightBucket("sample", 1))
            }
        }
        val viewModel = InsightViewModel(ObserveInsight(repository))
        advanceUntilIdle()
        repository.insightRequests.clear()

        viewModel.applySeed(InsightFilterSeed(yearMonths = setOf("2026-03")))
        advanceUntilIdle()

        val lastFilterSpec = repository.insightRequests.last().second
        assertEquals(setOf("2026-03"), lastFilterSpec.yearMonths)
        assertEquals(setOf(java.time.YearMonth.parse("2026-03")), viewModel.uiState.value.filters.months)
    }
}
