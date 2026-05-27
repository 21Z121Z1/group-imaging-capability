package com.oplus.groupimaging.ui.album.groups

import com.oplus.groupimaging.MainDispatcherRule
import com.oplus.groupimaging.TestRepository
import com.oplus.groupimaging.domain.usecase.ObserveRuleGroups
import com.oplus.groupimaging.testing.InsightTestFixtures
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
class AlbumGroupsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads rule groups`() = runTest {
        val repository = TestRepository().apply {
            ruleGroups = InsightTestFixtures.ruleGroups()
        }

        val viewModel = AlbumGroupsViewModel(
            observeRuleGroups = ObserveRuleGroups(repository),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(6, viewModel.uiState.value.rules.size)
        assertEquals("主摄", viewModel.uiState.value.rules.first().label)
    }

    @Test
    fun `load failure surfaces error state`() = runTest {
        val repository = TestRepository().apply {
            ruleGroupsFailure = IllegalStateException("rule cache unavailable")
        }

        val viewModel = AlbumGroupsViewModel(
            observeRuleGroups = ObserveRuleGroups(repository),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("rule cache unavailable", viewModel.uiState.value.error)
    }

    @Test
    fun `tab change updates current tab and rule click emits navigation`() = runTest {
        val repository = TestRepository()
        val viewModel = AlbumGroupsViewModel(
            observeRuleGroups = ObserveRuleGroups(repository),
        )
        advanceUntilIdle()

        val effectDeferred = async { viewModel.effect.first() }
        viewModel.onAction(AlbumGroupsAction.OnTabChange(RuleTab.CUSTOM))
        viewModel.onAction(AlbumGroupsAction.OnRuleClick("RAW"))

        assertEquals(RuleTab.CUSTOM, viewModel.uiState.value.selectedTab)
        assertEquals(
            AlbumGroupsEffect.NavigateToRulePreview("RAW"),
            effectDeferred.await(),
        )
    }
}
