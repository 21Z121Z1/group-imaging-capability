package com.oplus.groupimaging.testing.contract

import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.disableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.oplus.groupimaging.testing.composeHarnessRule
import com.oplus.groupimaging.ui.insight.FilterBottomSheet
import com.oplus.groupimaging.ui.insight.FilterSheetUiState
import com.oplus.groupimaging.ui.theme.OplusInsightTheme
import org.junit.Rule
import org.junit.Test

class FilterBottomSheetRestorationContractTest {
    @get:Rule
    val composeRule = composeHarnessRule()

    @Test
    fun selected_filter_state_survives_saved_instance_restore() {
        composeRule.disableAccessibilityChecks()
        val restorationTester = StateRestorationTester(composeRule)

        restorationTester.setContent {
            OplusInsightTheme {
                Surface {
                    FilterBottomSheet(
                        state = FilterSheetUiState(availableDevices = listOf("Find X8 Ultra")),
                        onDismiss = {},
                        onApply = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Live").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText("仅 Live").assertExists()
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("仅 Live").assertExists()
    }
}
