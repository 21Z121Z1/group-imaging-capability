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
class RouteRestorationIntegrationTest {
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
    fun insight_filter_state_survives_activity_recreation() {
        val robot = AppRobot(composeRule)
        robot.dismissPermissionDialogIfPresent()
            .tapTag(TestTags.Home.TELE_LENS)
            .assertTagVisible(TestTags.Screen.INSIGHT)
            .assertVisible("TELE")

        composeRule.activityRule.scenario.recreate()

        robot.assertTagVisible(TestTags.Screen.INSIGHT)
            .assertVisible("TELE")
    }

    @Test
    fun settings_subpage_survives_activity_recreation() {
        val robot = AppRobot(composeRule)
        robot.dismissPermissionDialogIfPresent()
            .tapTag(TestTags.Navigation.SETTINGS)
            .tapTag(TestTags.Settings.MANAGE_DIRECTORIES)
            .assertTagVisible(TestTags.Screen.DIRECTORY_MANAGER)

        composeRule.activityRule.scenario.recreate()

        robot.assertTagVisible(TestTags.Screen.DIRECTORY_MANAGER)
    }

    @Test
    fun move_progress_dialog_survives_activity_recreation() {
        val robot = AppRobot(composeRule)
        robot.dismissPermissionDialogIfPresent()
            .tapTag(TestTags.Navigation.ALBUM_GROUPS)
            .tapTag(TestTags.AlbumGroups.ruleCard("主摄"))
            .tapTag(TestTags.RulePreview.MOVE_ALL)
            .tapTag(TestTags.Dialogs.MOVE_CONFIRM_CONTINUE)
            .assertTagVisible(TestTags.Dialogs.MOVE_PROGRESS)

        composeRule.activityRule.scenario.recreate()

        robot.assertTagVisible(TestTags.Dialogs.MOVE_PROGRESS)
    }
}
