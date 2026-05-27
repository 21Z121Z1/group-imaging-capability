package com.oplus.groupimaging.testing.integration

import android.Manifest
import com.oplus.groupimaging.MainActivity
import com.oplus.groupimaging.testing.AppRobot
import com.oplus.groupimaging.testing.HarnessScenario
import com.oplus.groupimaging.testing.TestTags
import com.oplus.groupimaging.testing.enableHarnessAccessibilityChecks
import com.oplus.groupimaging.testing.hilt.TestRepositoryController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import androidx.test.rule.GrantPermissionRule

@HiltAndroidTest
class EmptyLibraryAppIntegrationTest {
    @Suppress("unused")
    private val scenario = HarnessScenario.emptyLibrary {
        withExtraDirectories("DCIM/WeChat/")
        refreshCompletesWithIndexedLibrary()
    }.also(TestRepositoryController::setScenario)

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val mediaPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_IMAGES)

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.enableHarnessAccessibilityChecks()
    }

    @Test
    fun permission_dialog_is_not_shown_before_scan_action() {
        AppRobot(composeRule)
            .assertTagVisible(TestTags.Screen.ALBUM_GROUPS)

        assertTrue(
            composeRule.onAllNodesWithTag(TestTags.PermissionDialog.ROOT, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }
}
