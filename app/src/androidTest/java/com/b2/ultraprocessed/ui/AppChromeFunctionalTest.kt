package com.b2.ultraprocessed.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.b2.ultraprocessed.ui.theme.UltraProcessedTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppChromeFunctionalTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun scannerScreen_rendersSharedHeaderAndFooter_andRoutesHeaderActions() {
        var historyClicks = 0
        var settingsClicks = 0
        var barcodeClicks = 0

        composeRule.setContent {
            UltraProcessedTheme {
                ScannerScreen(
                    hasApiKey = false,
                    hasUsdaApiKey = false,
                    enableLiveCamera = false,
                    onScan = {},
                    onBarcodeScanned = { barcodeClicks += 1 },
                    onSettings = { settingsClicks += 1 },
                    onHistory = { historyClicks += 1 },
                )
            }
        }

        composeRule.onNodeWithTag(AppTestTags.HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(AppTestTags.FOOTER).assertIsDisplayed()
        composeRule.onNodeWithText("Zest").assertIsDisplayed()
        composeRule.onNodeWithTag(AppTestTags.HEADER_ACTION_HISTORY).performClick()
        composeRule.onNodeWithTag(AppTestTags.HEADER_ACTION_SETTINGS).performClick()
        composeRule.onNodeWithTag(AppTestTags.SCANNER_BARCODE_BUTTON).performClick()

        composeRule.runOnIdle {
            assertEquals(1, historyClicks)
            assertEquals(1, settingsClicks)
            assertEquals(1, barcodeClicks)
        }
    }

    @Test
    fun resultsScreen_rendersSharedChrome() {
        composeRule.setContent {
            UltraProcessedTheme {
                ResultsScreen(
                    result = sampleScanResult,
                    onScanAgain = {},
                    onOpenHistory = {},
                )
            }
        }

        composeRule.onNodeWithTag(AppTestTags.HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(AppTestTags.FOOTER).assertIsDisplayed()
        composeRule.onNodeWithText("What this means").assertIsDisplayed()
        composeRule.onNodeWithText("Uploaded Label Result").assertIsDisplayed()
    }

    private val sampleScanResult = ScanResultUi(
        productName = "Uploaded Label Result",
        novaGroup = 4,
        summary = "Flagged for multiple industrial additives and syrup-based sweeteners.",
        problemIngredients = listOf(
            ProblemIngredient(
                name = "High Fructose Corn Syrup",
                reason = "Industrial sweetener often seen in ultra-processed products.",
            ),
        ),
        allIngredients = listOf("Sugar", "High Fructose Corn Syrup", "Natural Flavor"),
        engineLabel = "Rules engine (on-device)",
        confidence = 0.88f,
    )
}
