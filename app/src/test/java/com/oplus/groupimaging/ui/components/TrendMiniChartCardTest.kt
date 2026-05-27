package com.oplus.groupimaging.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oplus.groupimaging.domain.InsightBucket
import com.oplus.groupimaging.ui.theme.OplusInsightTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TrendMiniChartCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `trend card shows visible labels and counts`() {
        composeRule.setContent {
            OplusInsightTheme {
                TrendMiniChartCard(
                    title = "月度趋势",
                    buckets = listOf(
                        InsightBucket("2026-03", 5),
                        InsightBucket("2026-02", 3),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("月度趋势").assertIsDisplayed()
        composeRule.onNodeWithText("2026-03").assertIsDisplayed()
        composeRule.onNodeWithText("5 张").assertIsDisplayed()
        composeRule.onNodeWithText("2026-02").assertIsDisplayed()
        composeRule.onNodeWithText("3 张").assertIsDisplayed()
    }

    @Test
    fun `trend card shows empty state text`() {
        composeRule.setContent {
            OplusInsightTheme {
                TrendMiniChartCard(
                    title = "年度趋势",
                    buckets = emptyList(),
                )
            }
        }

        composeRule.onNodeWithText("年度趋势").assertIsDisplayed()
        composeRule.onNodeWithText("暂无趋势数据").assertIsDisplayed()
    }
}
