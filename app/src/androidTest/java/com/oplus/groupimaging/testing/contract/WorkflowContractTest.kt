package com.oplus.groupimaging.testing.contract

import androidx.compose.foundation.layout.PaddingValues
import com.oplus.groupimaging.testing.AppRobot
import com.oplus.groupimaging.testing.TestTags
import com.oplus.groupimaging.testing.UiStateFixtures
import com.oplus.groupimaging.testing.composeHarnessRule
import com.oplus.groupimaging.testing.setGroupImagingContent
import com.oplus.groupimaging.ui.album.preview.MoveConfirmDialog
import com.oplus.groupimaging.ui.album.preview.MoveProgressScreen
import com.oplus.groupimaging.ui.album.preview.RulePreviewAction
import com.oplus.groupimaging.ui.album.preview.RulePreviewScreen
import com.oplus.groupimaging.ui.home.HomeAction
import com.oplus.groupimaging.ui.home.HomeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WorkflowContractTest {
    @get:Rule
    val composeRule = composeHarnessRule()

    @Test
    fun home_empty_state_start_scan_emits_action() {
        var lastAction: HomeAction? = null

        composeRule.setGroupImagingContent {
            HomeScreen(
                state = UiStateFixtures.homeEmpty(),
                contentPadding = PaddingValues(),
                onAction = { lastAction = it },
            )
        }

        AppRobot(composeRule)
            .assertTagVisible(TestTags.Screen.HOME)
            .tapTag(TestTags.Home.START_SCAN)

        composeRule.runOnIdle {
            assertEquals(HomeAction.OnStartScanClick, lastAction)
        }
    }

    @Test
    fun rule_preview_move_all_emits_action() {
        var lastAction: RulePreviewAction? = null

        composeRule.setGroupImagingContent {
            RulePreviewScreen(
                state = UiStateFixtures.rulePreview(),
                contentPadding = PaddingValues(),
                onBack = {},
                onAction = { lastAction = it },
            )
        }

        AppRobot(composeRule)
            .assertTagVisible(TestTags.Screen.RULE_PREVIEW)
            .tapTag(TestTags.RulePreview.MOVE_ALL)

        composeRule.runOnIdle {
            assertEquals(RulePreviewAction.OnMoveClick, lastAction)
        }
    }

    @Test
    fun move_confirm_dialog_renders_key_information() {
        composeRule.setGroupImagingContent {
            MoveConfirmDialog(
                state = UiStateFixtures.moveConfirm(),
                onDismiss = {},
                onConfirm = {},
            )
        }

        AppRobot(composeRule)
            .assertTagVisible(TestTags.Dialogs.MOVE_CONFIRM)
            .assertVisible("确认移动原文件")
            .assertVisible("/storage/emulated/0/DCIM/myalbums/主摄/", substring = true)
            .assertTagVisible(TestTags.Dialogs.MOVE_CONFIRM_CONTINUE)
    }

    @Test
    fun move_progress_dialog_renders_completion_state() {
        composeRule.setGroupImagingContent {
            MoveProgressScreen(
                state = UiStateFixtures.moveProgress(),
                onDismiss = {},
            )
        }

        AppRobot(composeRule)
            .assertTagVisible(TestTags.Dialogs.MOVE_PROGRESS)
            .assertVisible("移动进度")
            .assertVisible("已移动 3384/3384", substring = true)
            .assertTagVisible(TestTags.Dialogs.MOVE_PROGRESS_CONFIRM)
    }
}
