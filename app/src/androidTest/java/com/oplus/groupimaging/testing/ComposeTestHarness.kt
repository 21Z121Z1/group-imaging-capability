package com.oplus.groupimaging.testing

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule

typealias ComposeHarnessRule =
    AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>

fun composeHarnessRule(): ComposeHarnessRule =
    createAndroidComposeRule<ComponentActivity>().apply {
        enableHarnessAccessibilityChecks()
    }
