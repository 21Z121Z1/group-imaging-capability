package com.oplus.groupimaging.testing

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.oplus.groupimaging.ui.theme.OplusInsightTheme

fun ComposeContentTestRule.setGroupImagingContent(content: @Composable () -> Unit) {
    setContent {
        OplusInsightTheme {
            Surface {
                content()
            }
        }
    }
}

fun ComposeContentTestRule.waitForVisibleTag(tag: String) {
    waitUntil(timeoutMillis = 5_000) {
        scrollToTagIfPossible(tag)
        onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
}

fun ComposeContentTestRule.waitForVisibleText(
    text: String,
    substring: Boolean = false,
) {
    waitUntil(timeoutMillis = 5_000) {
        scrollToTextIfPossible(text, substring)
        onAllNodesWithText(text, substring = substring, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }
}

private fun ComposeContentTestRule.scrollToTagIfPossible(tag: String) {
    val scrollables = onAllNodes(hasScrollToNodeAction(), useUnmergedTree = true)
    val scrollableCount = scrollables.fetchSemanticsNodes().size
    repeat(scrollableCount) { index ->
        runCatching { scrollables[index].performScrollToNode(hasTestTag(tag)) }
    }
    runCatching { onNodeWithTag(tag, useUnmergedTree = true).performScrollTo() }
}

private fun ComposeContentTestRule.scrollToTextIfPossible(
    text: String,
    substring: Boolean,
) {
    val scrollables = onAllNodes(hasScrollToNodeAction(), useUnmergedTree = true)
    val scrollableCount = scrollables.fetchSemanticsNodes().size
    repeat(scrollableCount) { index ->
        runCatching { scrollables[index].performScrollToNode(hasText(text, substring = substring)) }
    }
    runCatching { onNodeWithText(text, substring = substring, useUnmergedTree = true).performScrollTo() }
}

fun ComposeContentTestRule.visibleTag(tag: String): SemanticsNodeInteraction =
    onNodeWithTag(tag, useUnmergedTree = true)

fun ComposeContentTestRule.visibleText(
    text: String,
    substring: Boolean = false,
): SemanticsNodeInteraction = onNodeWithText(
    text = text,
    substring = substring,
    useUnmergedTree = true,
)

class AppRobot(
    private val rule: ComposeContentTestRule,
) {
    fun assertTagVisible(tag: String): AppRobot = apply {
        rule.waitForVisibleTag(tag)
        rule.onAllNodesWithTag(tag, useUnmergedTree = true).onFirst().assertIsDisplayed()
    }

    fun tapTag(tag: String): AppRobot = apply {
        rule.waitForVisibleTag(tag)
        rule.onAllNodesWithTag(tag, useUnmergedTree = true).onFirst().performClick()
    }

    fun assertVisible(text: String, substring: Boolean = false): AppRobot = apply {
        rule.waitForVisibleText(text, substring)
        rule.onAllNodesWithText(text, substring = substring, useUnmergedTree = true).onFirst().assertIsDisplayed()
    }

    fun tap(text: String, substring: Boolean = false): AppRobot = apply {
        rule.waitForVisibleText(text, substring)
        rule.onAllNodesWithText(text, substring = substring, useUnmergedTree = true).onFirst().performClick()
    }

    fun dismissPermissionDialogIfPresent(): AppRobot = apply {
        if (rule.onAllNodesWithTag(TestTags.PermissionDialog.ROOT, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
            tapTag(TestTags.PermissionDialog.DISMISS)
        }
    }
}
