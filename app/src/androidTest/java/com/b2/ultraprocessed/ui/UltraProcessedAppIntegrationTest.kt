package com.b2.ultraprocessed.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.b2.ultraprocessed.ui.theme.UltraProcessedTheme
import org.junit.Rule
import org.junit.Test

class UltraProcessedAppIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun appStartsAtScanner_withProductionEntryPoints() {
        composeRule.setContent {
            UltraProcessedTheme {
                UltraProcessedApp(
                    timingConfig = AppTimingConfig(
                        splashDurationMillis = 0L,
                        analysisMinimumDisplayMillis = 0L,
                    ),
                    enableLiveCamera = false,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Zest").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Zest").assertIsDisplayed()
        composeRule.onNodeWithTag(AppTestTags.SCANNER_CAPTURE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(AppTestTags.SCANNER_UPLOAD_BUTTON).assertIsDisplayed()
    }
}
