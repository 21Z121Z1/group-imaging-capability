package com.oplus.groupimaging.ui.album.preview

import android.content.IntentSender
import com.oplus.groupimaging.MainDispatcherRule
import com.oplus.groupimaging.TestRepository
import com.oplus.groupimaging.domain.FilterSpec
import com.oplus.groupimaging.domain.MoveExecutionOutcome
import com.oplus.groupimaging.domain.MoveCandidate
import com.oplus.groupimaging.domain.MovePreview
import com.oplus.groupimaging.domain.MoveResult
import com.oplus.groupimaging.domain.RuleGroupSummary
import com.oplus.groupimaging.domain.usecase.ExecuteMovePlan
import com.oplus.groupimaging.domain.usecase.ObserveRuleGroups
import com.oplus.groupimaging.domain.usecase.PreviewMovePlan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RulePreviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `move flow uses active rule filters and executes preview token`() = runTest {
        val repository = TestRepository().apply {
            ruleGroups = listOf(
                RuleGroupSummary(
                    label = "RAW",
                    count = 2,
                    filters = FilterSpec(rawOnly = true),
                ),
            )
            movePreview = MovePreview(
                token = "preview-token",
                ruleName = "RAW",
                targetFolder = "/storage/emulated/0/DCIM/myalbums/RAW/",
                count = 2,
                conflicts = 1,
                candidates = listOf(
                    MoveCandidate(
                        assetId = "asset-1",
                        sourcePath = "/tmp/a.jpg",
                        sourceUri = "content://asset-1",
                        fileName = "a.jpg",
                        destinationFileName = "a__1.jpg",
                        destinationRelativePath = "DCIM/myalbums/RAW/",
                        destinationPath = "/storage/emulated/0/DCIM/myalbums/RAW/a__1.jpg",
                        conflict = false,
                    ),
                ),
            )
            moveResult = MoveResult(
                token = "preview-token",
                movedCount = 1,
                failureCount = 0,
                failures = emptyList(),
            )
        }
        val viewModel = RulePreviewViewModel(
            observeRuleGroups = ObserveRuleGroups(repository),
            previewMovePlan = PreviewMovePlan(repository),
            executeMovePlan = ExecuteMovePlan(repository),
        )

        viewModel.load("RAW")
        advanceUntilIdle()

        assertTrue(repository.previewRequests.first().third.rawOnly == true)
        assertEquals("/storage/emulated/0/DCIM/myalbums/RAW/", repository.previewRequests.first().second)
        assertEquals(2, viewModel.uiState.value.matchedCount)

        val effectsDeferred = async { viewModel.effect.take(2).toList() }
        viewModel.onAction(RulePreviewAction.OnMoveClick)
        advanceUntilIdle()
        viewModel.onAction(RulePreviewAction.OnConfirmMove)
        advanceUntilIdle()

        val effects = effectsDeferred.await()
        assertTrue(effects.first() is RulePreviewEffect.ShowMoveConfirmDialog)
        val confirm = effects.first() as RulePreviewEffect.ShowMoveConfirmDialog
        assertEquals(
            "移动会修改原文件在系统媒体库中的位置。继续前请确认目标目录、RAW 伴随项和冲突改名数量。",
            confirm.plan.warningText,
        )
        assertTrue((effects[1] as RulePreviewEffect.ShowMoveProgress).progress.isCompleted)
        assertEquals(listOf("preview-token" to false), repository.executeRequests)
    }

    @Test
    fun `move flow requests write access and retries after authorization`() = runTest {
        val intentSender = testIntentSender()
        val repository = TestRepository().apply {
            movePreview = MovePreview(
                token = "preview-token",
                ruleName = "RAW",
                targetFolder = "/storage/emulated/0/DCIM/myalbums/RAW/",
                count = 1,
                conflicts = 0,
                candidates = listOf(
                    MoveCandidate(
                        assetId = "asset-1",
                        sourcePath = "/tmp/a.jpg",
                        sourceUri = "content://asset-1",
                        fileName = "a.jpg",
                        destinationFileName = "a.jpg",
                        destinationRelativePath = "DCIM/myalbums/RAW/",
                        destinationPath = "/storage/emulated/0/DCIM/myalbums/RAW/a.jpg",
                        conflict = false,
                    ),
                ),
            )
            moveExecutionOutcome = MoveExecutionOutcome.RequiresWriteAccess(intentSender)
            moveResult = MoveResult(
                token = "preview-token",
                movedCount = 1,
                failureCount = 0,
                failures = emptyList(),
            )
        }
        val viewModel = RulePreviewViewModel(
            observeRuleGroups = ObserveRuleGroups(repository),
            previewMovePlan = PreviewMovePlan(repository),
            executeMovePlan = ExecuteMovePlan(repository),
        )

        viewModel.load("RAW")
        advanceUntilIdle()

        val effectsDeferred = async { viewModel.effect.take(3).toList() }
        viewModel.onAction(RulePreviewAction.OnConfirmMove)
        advanceUntilIdle()
        repository.moveExecutionOutcome = MoveExecutionOutcome.Completed(repository.moveResult)
        viewModel.onAction(RulePreviewAction.OnMoveAuthorizationResult(granted = true))
        advanceUntilIdle()

        val effects = effectsDeferred.await()
        assertTrue(effects.first() is RulePreviewEffect.RequestMoveWriteAccess)
        assertTrue(effects[1] is RulePreviewEffect.ShowMoveProgress)
        assertTrue((effects[2] as RulePreviewEffect.ShowMoveProgress).progress.isCompleted)
        assertEquals(
            listOf("preview-token" to false, "preview-token" to true),
            repository.executeRequests,
        )
    }

    @Test
    fun `empty preview does not show move confirmation`() = runTest {
        val repository = TestRepository().apply {
            movePreview = MovePreview(
                token = "empty-token",
                ruleName = "RAW",
                targetFolder = "/storage/emulated/0/DCIM/myalbums/RAW/",
                count = 0,
                conflicts = 0,
                candidates = emptyList(),
            )
        }
        val viewModel = RulePreviewViewModel(
            observeRuleGroups = ObserveRuleGroups(repository),
            previewMovePlan = PreviewMovePlan(repository),
            executeMovePlan = ExecuteMovePlan(repository),
        )

        viewModel.load("RAW")
        advanceUntilIdle()
        viewModel.onAction(RulePreviewAction.OnMoveClick)
        advanceUntilIdle()

        assertEquals("没有可移动的文件", viewModel.uiState.value.error)
        assertTrue(repository.executeRequests.isEmpty())
    }

    @Test
    fun `rule names are converted to safe album subfolders`() = runTest {
        val repository = TestRepository().apply {
            ruleGroups = listOf(
                RuleGroupSummary(
                    label = "主摄/长焦",
                    count = 1,
                    filters = FilterSpec(lensClasses = emptySet()),
                ),
            )
        }
        val viewModel = RulePreviewViewModel(
            observeRuleGroups = ObserveRuleGroups(repository),
            previewMovePlan = PreviewMovePlan(repository),
            executeMovePlan = ExecuteMovePlan(repository),
        )

        viewModel.load("主摄/长焦")
        advanceUntilIdle()

        assertEquals("/storage/emulated/0/DCIM/myalbums/主摄-长焦/", repository.previewRequests.first().second)
    }
}

private fun testIntentSender(): IntentSender {
    val ctor = IntentSender::class.java.declaredConstructors.single()
    ctor.isAccessible = true
    return ctor.newInstance(*arrayOfNulls<Any>(ctor.parameterCount)) as IntentSender
}
