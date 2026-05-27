package com.oplus.groupimaging.testing

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks

fun ComposeTestRule.enableHarnessAccessibilityChecks() {
    enableAccessibilityChecks()
}
