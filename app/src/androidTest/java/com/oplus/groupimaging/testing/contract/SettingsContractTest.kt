package com.oplus.groupimaging.testing.contract

import androidx.compose.foundation.layout.PaddingValues
import com.oplus.groupimaging.testing.AppRobot
import com.oplus.groupimaging.testing.TestTags
import com.oplus.groupimaging.testing.UiStateFixtures
import com.oplus.groupimaging.testing.composeHarnessRule
import com.oplus.groupimaging.testing.setGroupImagingContent
import com.oplus.groupimaging.ui.settings.SettingsAction
import com.oplus.groupimaging.ui.settings.SettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsContractTest {
    @get:Rule
    val composeRule = composeHarnessRule()

    @Test
    fun settings_cards_emit_expected_actions() {
        val emittedActions = mutableListOf<SettingsAction>()

        composeRule.setGroupImagingContent {
            SettingsScreen(
                state = UiStateFixtures.settings(),
                contentPadding = PaddingValues(),
                onAction = { emittedActions += it },
            )
        }

        AppRobot(composeRule)
            .assertTagVisible(TestTags.Screen.SETTINGS)
            .tapTag(TestTags.Settings.MANAGE_DIRECTORIES)
            .tapTag(TestTags.Settings.PARSER_VERSION)
            .tapTag(TestTags.Settings.DEVICE_PROFILE_COUNT)
            .tapTag(TestTags.Settings.SCOPE_EXPLANATION)

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    SettingsAction.OnManageDirectoriesClick,
                    SettingsAction.OnOpenFailedItems,
                    SettingsAction.OnOpenDeviceProfiles,
                    SettingsAction.OnOpenScopeExplanation,
                ),
                emittedActions,
            )
        }
    }
}
