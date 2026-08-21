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
        rule.onNodeWithText("校准").assertIsDisplayed()
        rule.onNodeWithText("选择图片").assertIsDisplayed()
    }

    @Test
    fun calibrationIsSecondaryAndSettingsRemainsPrimary() {
        rule.onNodeWithText("校准").performClick()
        rule.onNodeWithText("SDR 六灰阶").assertIsDisplayed()

        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitUntil(timeoutMillis = 4_000) {
            runCatching {
                rule.onNodeWithText("截图去水印").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        rule.onNodeWithText("设置").performClick()
        rule.onNodeWithText("输出位置").assertIsDisplayed()
    }

    @Test
    fun backFromSettingsHandsOffToProcess() {
        rule.onNodeWithText("设置").performClick()
        rule.onNodeWithText("输出位置").assertIsDisplayed()

        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitUntil(timeoutMillis = 4_000) {
            runCatching {
                rule.onNodeWithText("截图去水印").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        rule.onNodeWithText("截图去水印").assertIsDisplayed()
    }

    @Test
    fun backFromCalibrationHomeHandsOffToProcess() {
        rule.onNodeWithText("校准").performClick()
        rule.onNodeWithText("SDR 六灰阶").assertIsDisplayed()

        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitUntil(timeoutMillis = 4_000) {
            runCatching {
                rule.onNodeWithText("截图去水印").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        rule.onNodeWithText("截图去水印").assertIsDisplayed()
    }

    @Test
    fun backFromFullscreenCalibrationHandsOffToCalibrationHome() {
        rule.onNodeWithText("校准").performClick()
        rule.onNodeWithText("开始 SDR 校准").performClick()
        rule.waitForIdle()

        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitUntil(timeoutMillis = 4_000) {
            runCatching {
                rule.onNodeWithText("SDR 六灰阶").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        rule.onNodeWithText("SDR 六灰阶").assertIsDisplayed()
    }
}
