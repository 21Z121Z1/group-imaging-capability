package com.oplus.groupimaging.testing.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.oplus.groupimaging.testing.ScreenshotTest
import com.oplus.groupimaging.testing.UiStateFixtures
import com.oplus.groupimaging.ui.album.preview.MoveConfirmDialog
import com.oplus.groupimaging.ui.album.preview.MoveProgressScreen
import com.oplus.groupimaging.ui.album.preview.RulePreviewScreen
import com.oplus.groupimaging.ui.home.HomeScreen
import com.oplus.groupimaging.ui.settings.SettingsScreen
import com.oplus.groupimaging.ui.theme.OplusInsightTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
@Category(ScreenshotTest::class)
class AppScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture_home_screen_empty_light() {
        capture(
            fileName = "home/home_empty_light.png",
            darkTheme = false,
        ) {
            HomeScreen(
                state = UiStateFixtures.homeEmpty(),
                contentPadding = PaddingValues(),
                onAction = {},
            )
        }

    }

    @Test
    fun capture_home_screen_indexed_dark() {
        capture(
            fileName = "home/home_indexed_dark.png",
            darkTheme = true,
        ) {
            HomeScreen(
                state = UiStateFixtures.homeIndexed(),
                contentPadding = PaddingValues(),
                onAction = {},
            )
        }
    }

    @Test
    fun capture_settings_screen_large_font_error_state() {
        capture(
            fileName = "settings/settings_error_font2_dark.png",
            darkTheme = true,
            fontScale = 2f,
        ) {
            SettingsScreen(
                state = UiStateFixtures.settings().copy(error = "设置摘要加载失败"),
                contentPadding = PaddingValues(),
                onAction = {},
            )
        }
    }

    @Test
    fun capture_rule_preview_screen_light() {
        capture(
            fileName = "rules/rule_preview_light.png",
            darkTheme = false,
        ) {
            RulePreviewScreen(
                state = UiStateFixtures.rulePreview(),
                contentPadding = PaddingValues(),
                onBack = {},
                onAction = {},
            )
        }
    }

    @Test
    fun capture_move_confirm_dialog_dark() {
        captureDialog(
            fileName = "dialogs/move_confirm_dark.png",
            darkTheme = true,
        ) {
            MoveConfirmDialog(
                state = UiStateFixtures.moveConfirm(),
                onDismiss = {},
                onConfirm = {},
            )
        }
    }

    @Test
    fun capture_move_progress_completed_state() {
        captureDialog(
            fileName = "dialogs/move_progress_completed_light.png",
            darkTheme = false,
        ) {
            MoveProgressScreen(
                state = UiStateFixtures.moveProgress(),
                onDismiss = {},
            )
        }
    }

    private fun capture(
        fileName: String,
        darkTheme: Boolean,
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            ScreenshotFrame(
                darkTheme = darkTheme,
                fontScale = fontScale,
            ) {
                content()
            }
        }

        composeRule.onRoot(useUnmergedTree = true).captureRoboImage(file = screenshotFile(fileName))
    }

    private fun captureDialog(
        fileName: String,
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            ScreenshotFrame(darkTheme = darkTheme) {
                content()
            }
        }
        composeRule.onRoot(useUnmergedTree = true).captureRoboImage(file = screenshotFile(fileName))
    }

    private fun screenshotFile(fileName: String): File =
        File("src/test/screenshots/$fileName").apply {
            parentFile?.mkdirs()
        }
}

@Composable
private fun ScreenshotFrame(
    darkTheme: Boolean,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = fontScale,
        ),
    ) {
        OplusInsightTheme(darkTheme = darkTheme) {
            Box(
                modifier = Modifier
                    .width(411.dp)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
            ) {
                content()
            }
        }
    }
}
