package com.oplus.groupimaging.ui.insight

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oplus.groupimaging.ui.theme.OplusInsightTheme
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class FilterBottomSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `filter bottom sheet renders and applies month chips`() {
        var applied: InsightFiltersUi? = null
        composeRule.setContent {
            OplusInsightTheme {
                var draft by remember { mutableStateOf(InsightFiltersUi()) }
                FilterSheetContent(
                    state = FilterSheetUiState(
                        availableMonths = listOf(YearMonth.of(2026, 3)),
                    ),
                    draft = draft,
                    onDraftChange = { draft = it },
                    onDismiss = {},
                    onApply = { applied = it },
                )
            }
        }

        composeRule.onNodeWithText("2026-03").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("应用筛选").performSemanticsAction(SemanticsActions.OnClick)

        composeRule.runOnIdle {
            assertEquals(setOf(YearMonth.of(2026, 3)), applied?.months)
        }
    }
}
