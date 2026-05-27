package com.oplus.groupimaging.testing.integration

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.oplus.groupimaging.MainActivity
import com.oplus.groupimaging.testing.AppRobot
import com.oplus.groupimaging.testing.HarnessScenario
import com.oplus.groupimaging.testing.TestTags
import com.oplus.groupimaging.testing.enableHarnessAccessibilityChecks
import com.oplus.groupimaging.testing.hilt.TestRepositoryController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class IndexedLibraryAppIntegrationTest {
    @Suppress("unused")
    private val scenario = HarnessScenario.indexedLibrary().also(TestRepositoryController::setScenario)

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.enableHarnessAccessibilityChecks()
    }

    @Test
    fun home_tele_card_opens_seeded_insight_route() {
        AppRobot(composeRule)
            .dismissPermissionDialogIfPresent()
            .assertTagVisible(TestTags.Screen.HOME)
            .tapTag(TestTags.Home.TELE_LENS)
            .assertTagVisible(TestTags.Screen.INSIGHT)
            .assertVisible("TELE")
    }

    @Test
    fun settings_cards_open_subpages() {
        AppRobot(composeRule)
            .dismissPermissionDialogIfPresent()
            .tapTag(TestTags.Navigation.SETTINGS)
            .assertTagVisible(TestTags.Screen.SETTINGS)
            .tapTag(TestTags.Settings.MANAGE_DIRECTORIES)
            .assertTagVisible(TestTags.Screen.DIRECTORY_MANAGER)
    }

    @Test
    fun rule_preview_flow_shows_global_move_dialogs() {
        AppRobot(composeRule)
            .dismissPermissionDialogIfPresent()
            .tapTag(TestTags.Navigation.ALBUM_GROUPS)
            .assertTagVisible(TestTags.Screen.ALBUM_GROUPS)
            .tapTag(TestTags.AlbumGroups.ruleCard("主摄"))
            .assertTagVisible(TestTags.Screen.RULE_PREVIEW)
            .tapTag(TestTags.RulePreview.MOVE_ALL)
            .assertTagVisible(TestTags.Dialogs.MOVE_CONFIRM)
            .tapTag(TestTags.Dialogs.MOVE_CONFIRM_CONTINUE)
            .assertTagVisible(TestTags.Dialogs.MOVE_PROGRESS)
    }
}
