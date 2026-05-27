package com.oplus.groupimaging.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oplus.groupimaging.testing.InsightTestFixtures
import com.oplus.groupimaging.ui.theme.OplusInsightTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AssetPreviewItemTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `asset preview item exposes thumbnail fallback semantics when image cannot load`() {
        composeRule.setContent {
            OplusInsightTheme {
                AssetPreviewItem(
                    item = InsightTestFixtures.assetPreview(
                        assetId = "asset-missing",
                        fileName = "IMG20260323200232.jpg",
                    ).copy(assetUri = "content://missing/asset"),
                    onClick = {},
                )
            }
        }

        composeRule
            .onNode(hasContentDescription("IMG20260323200232.jpg 缩略图不可用"))
            .assertIsDisplayed()
    }
}
