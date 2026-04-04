package com.b2.ultraprocessed.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.b2.ultraprocessed.classify.ClassificationContext
import com.b2.ultraprocessed.classify.IngredientInput
import com.b2.ultraprocessed.classify.RulesClassifier
import com.b2.ultraprocessed.ocr.OcrEngine
import com.b2.ultraprocessed.ui.theme.DarkBg
import kotlinx.coroutines.launch

data class AppTimingConfig(
    val splashDurationMillis: Long = 4200L,
    val analysisDurationMillis: Long = 3500L,
)

@Composable
fun UltraProcessedApp(
    timingConfig: AppTimingConfig = AppTimingConfig(),
    enableLiveCamera: Boolean = true,
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.Splash) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var selectedModelId by rememberSaveable {
        mutableStateOf(StubUiData.modelOptions.first().id)
    }
    var currentResultIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastCapturedPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var lastOcrText by rememberSaveable { mutableStateOf<String?>(null) }
    var lastRealResult by remember { mutableStateOf<ScanResultUi?>(null) }
    val scope = rememberCoroutineScope()
    val classifier = remember { RulesClassifier() }

    val historyItems = remember {
        mutableStateListOf<HistoryItemUi>().apply {
            addAll(StubUiData.initialHistory())
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBg,
    ) {
        AnimatedContent(
            targetState = destination,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "destination-animation",
        ) { screen ->
            when (screen) {
                AppDestination.Splash -> SplashScreen(
                    displayDurationMillis = timingConfig.splashDurationMillis,
                    onComplete = { destination = AppDestination.Scanner },
                )

                AppDestination.Scanner -> ScannerScreen(
                    hasApiKey = apiKey.isNotBlank(),
                    enableLiveCamera = enableLiveCamera,
                    onScan = { path ->
                        lastCapturedPhotoPath = path
                        // Run real OCR on captured image
                        val bitmap = BitmapFactory.decodeFile(path)
                        if (bitmap != null) {
                            OcrEngine.extractText(bitmap) { ocrResult ->
                                lastOcrText = if (ocrResult.success) ocrResult.normalizedText else null
                                // Run RulesClassifier on OCR output
                                if (ocrResult.success && ocrResult.normalizedText.isNotBlank()) {
                                    scope.launch {
                                        val classResult = classifier.classify(
                                            input = IngredientInput(
                                                rawText = ocrResult.rawText,
                                                normalizedText = ocrResult.normalizedText,
                                            ),
                                            context = ClassificationContext(
                                                allowNetwork = false,
                                                apiFallbackEnabled = false,
                                                preferOnDevice = true,
                                            ),
                                        )
                                        lastRealResult = ScanResultUi(
                                            productName = "Scanned Product",
                                            novaGroup = classResult.novaGroup,
                                            summary = classResult.explanation,
                                            problemIngredients = classResult.markers.map {
                                                ProblemIngredient(it, "Detected by rules engine.")
                                            },
                                            allIngredients = ocrResult.normalizedText
                                                .split(",")
                                                .map { it.trim() },
                                            engineLabel = classResult.engine,
                                        )
                                    }
                                }
                            }
                        }
                        destination = AppDestination.Analyzing
                    },
                    onTryDemo = {
                        lastCapturedPhotoPath = null
                        lastOcrText = null
                        lastRealResult = null
                        destination = AppDestination.Analyzing
                    },
                    onSettings = { destination = AppDestination.Settings },
                    onHistory = { destination = AppDestination.History },
                )

                AppDestination.Analyzing -> AnalyzingScreen(
                    modelName = StubUiData.modelOptions
                        .firstOrNull { it.id == selectedModelId }
                        ?.name ?: selectedModelId,
                    displayDurationMillis = timingConfig.analysisDurationMillis,
                    onComplete = {
                        val result = lastRealResult
                            ?: StubUiData.results[currentResultIndex % StubUiData.results.size]

                        historyItems.add(
                            0,
                            HistoryItemUi(
                                id = "scan-${System.currentTimeMillis()}",
                                productName = result.productName,
                                novaGroup = result.novaGroup,
                                scannedAt = "Just now",
                                summary = result.summary,
                                capturedImagePath = lastCapturedPhotoPath,
                            ),
                        )
                        if (lastRealResult == null) {
                            currentResultIndex = (currentResultIndex + 1) % StubUiData.results.size
                        }
                        destination = AppDestination.Results
                    },
                )

                AppDestination.Results -> ResultsScreen(
                    result = lastRealResult
                        ?: StubUiData.results[
                            (currentResultIndex + StubUiData.results.size - 1) %
                                StubUiData.results.size
                        ],
                    onScanAgain = {
                        lastRealResult = null
                        destination = AppDestination.Scanner
                    },
                    onOpenHistory = { destination = AppDestination.History },
                )

                AppDestination.Settings -> SettingsScreen(
                    apiKey = apiKey,
                    selectedModelId = selectedModelId,
                    modelOptions = StubUiData.modelOptions,
                    onBack = { destination = AppDestination.Scanner },
                    onApiKeySaved = { apiKey = it },
                    onModelSelected = { selectedModelId = it },
                )

                AppDestination.History -> HistoryScreen(
                    historyItems = historyItems,
                    onBack = { destination = AppDestination.Scanner },
                    onClearItem = { item -> historyItems.remove(item) },
                )
            }
        }
    }
}
