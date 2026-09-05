package io.github.z121z1.watermarkcleaner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesProcessHome() {
        rule.onNodeWithText("截图去水印").assertIsDisplayed()
        rule.onNodeWithText("选择截图").assertIsDisplayed()
    }

    @Test
    fun navigatesToCalibrationAndSettings() {
        rule.onNodeWithText("校准").performClick()
        rule.onNodeWithText("SDR").assertIsDisplayed()
        rule.onNodeWithText("设置").performClick()
        rule.onNodeWithText("保存位置").assertIsDisplayed()
    }
}
