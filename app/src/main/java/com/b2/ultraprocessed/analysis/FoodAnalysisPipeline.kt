package com.b2.ultraprocessed.analysis

import android.content.Context
import com.b2.ultraprocessed.barcode.BarcodeResult
import com.b2.ultraprocessed.barcode.BarcodeScanner
import com.b2.ultraprocessed.barcode.MlKitBarcodeScanner
import com.b2.ultraprocessed.ingredients.IngredientTextNormalizer
import com.b2.ultraprocessed.network.llm.AllergenDetection
import com.b2.ultraprocessed.classify.ClassificationResult
import com.b2.ultraprocessed.analysis.UsageEstimateCalculator
import com.b2.ultraprocessed.network.llm.FoodLabelLlmWorkflow
import com.b2.ultraprocessed.network.llm.GeminiFoodLabelLlmWorkflow
import com.b2.ultraprocessed.network.llm.IngredientClassification
import com.b2.ultraprocessed.network.llm.IngredientExtraction
import com.b2.ultraprocessed.network.llm.MultiProviderFoodLabelLlmWorkflow
import com.b2.ultraprocessed.network.llm.OpenAiCompatibleFoodLabelLlmWorkflow
import com.b2.ultraprocessed.network.llm.SecretLlmApiKeyProvider
import com.b2.ultraprocessed.network.usda.SecretUsdaApiKeyProvider
import com.b2.ultraprocessed.network.usda.UsdaHttpClientFactory
import com.b2.ultraprocessed.network.usda.UsdaApiService
import com.b2.ultraprocessed.network.usda.UsdaRepository
import com.b2.ultraprocessed.ocr.MlKitOcrPipeline
import com.b2.ultraprocessed.ocr.OcrPipeline
import com.b2.ultraprocessed.ocr.OcrResult
import com.b2.ultraprocessed.storage.secrets.SecretKeyManager
import com.b2.ultraprocessed.ui.ClassificationUiMapper
import com.b2.ultraprocessed.ui.ProblemIngredient
import com.b2.ultraprocessed.ui.ScanResultUi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

class FoodAnalysisPipeline(
    private val ocrPipeline: OcrPipeline,
    private val barcodeScanner: BarcodeScanner,
    private val usdaRepository: UsdaRepository,
    private val llmWorkflow: FoodLabelLlmWorkflow? = null,
) {
    suspend fun analyzeFromImage(
        imagePath: String,
        modelId: String = DEFAULT_VLM_MODEL_ID,
        onStage: (AnalysisStage) -> Unit = {},
        onStatus: (String) -> Unit = {},
    ): Result<AnalysisReport> {
        onStage(AnalysisStage.AnalysingImage)
        val workflow = llmWorkflow ?: return Result.failure(
            Exception("API-only image analysis requires a configured LLM workflow."),
        )

        val llmFallbackWarnings = mutableListOf<String>()
        val llmResult = analyzeImageWithLlmWorkflow(
            workflow = workflow,
            imagePath = imagePath,
            modelId = modelId,
            onStage = onStage,
            onStatus = onStatus,
        )
        if (llmResult != null) {
            return llmResult
        }
        llmFallbackWarnings += "LLM image workflow unavailable; using OCR text for API analysis."
        onStatus("Using OCR text for API analysis. OCR may contain mistakes.")

        onStage(AnalysisStage.ExtractingIngredients)
        val ocr = ocrPipeline.recognizeText(imagePath)
        return when (ocr) {
            is OcrResult.Failure -> Result.failure(
                Exception(ocr.message.toFriendlyAnalysisMessage("Could not read enough ingredient text. Please try again.")),
            )
            is OcrResult.Success -> classifyFromIngredientsTextApiOnly(
                rawText = ocr.rawText,
                sourceImagePath = imagePath,
                modelId = modelId,
                sourceLabel = "OCR",
                sourceType = AnalysisSourceType.Ocr,
                productNameOverride = null,
                warnings = llmFallbackWarnings.toList(),
                onStage = onStage,
                onStatus = onStatus,
            )
        }
    }

    suspend fun analyzeFromBarcode(
        barcodeCode: String,
        sourceImagePath: String?,
        modelId: String = DEFAULT_VLM_MODEL_ID,
        onStage: (AnalysisStage) -> Unit = {},
        onStatus: (String) -> Unit = {},
    ): Result<AnalysisReport> {
        onStage(AnalysisStage.AnalysingImage)
        val usda = usdaRepository.lookupByBarcode(barcodeCode)
            ?: return fallbackToImageOrError(
                sourceImagePath = sourceImagePath,
                error = "No USDA match found for barcode $barcodeCode.",
                modelId = modelId,
                onStage = onStage,
                onStatus = onStatus,
            )

        onStage(AnalysisStage.ExtractingIngredients)
        val ingredients = usda.ingredientsText
        if (ingredients.isNullOrBlank()) {
            return fallbackToImageOrError(
                sourceImagePath = sourceImagePath,
                error = "USDA record found but no ingredient text was available.",
                modelId = modelId,
                onStage = onStage,
            )
        }

        return classifyFromIngredientsTextApiOnly(
            rawText = ingredients,
            sourceImagePath = sourceImagePath,
            modelId = modelId,
            sourceLabel = "Barcode → USDA",
            sourceType = AnalysisSourceType.Barcode,
            productNameOverride = usda.productName,
                warnings = emptyList(),
                scannedBarcode = barcodeCode.trim().takeIf { it.isNotEmpty() },
                brandOwner = usda.brandOwner,
                onStage = onStage,
                onStatus = onStatus,
            )
    }

    suspend fun analyzeFromBarcodeImage(
        imagePath: String,
        modelId: String = DEFAULT_VLM_MODEL_ID,
        onStage: (AnalysisStage) -> Unit = {},
        onStatus: (String) -> Unit = {},
    ): Result<AnalysisReport> {
        onStage(AnalysisStage.AnalysingImage)
        return when (val barcode = barcodeScanner.scanFromImagePath(imagePath)) {
            is BarcodeResult.Failure -> fallbackToImageOrError(
                sourceImagePath = imagePath,
                error = barcode.message,
                modelId = modelId,
                onStage = onStage,
                onStatus = onStatus,
            )
            is BarcodeResult.Success -> analyzeFromBarcode(
                barcodeCode = barcode.code,
                sourceImagePath = imagePath,
                modelId = modelId,
                onStage = onStage,
                onStatus = onStatus,
            )
        }
    }

    private suspend fun fallbackToImageOrError(
        sourceImagePath: String?,
        error: String,
        modelId: String = DEFAULT_VLM_MODEL_ID,
        onStage: (AnalysisStage) -> Unit = {},
        onStatus: (String) -> Unit = {},
    ): Result<AnalysisReport> {
        if (sourceImagePath != null) {
            val fallback = analyzeFromImage(sourceImagePath, modelId, onStage, onStatus)
            if (fallback.isSuccess) {
                val report = fallback.getOrNull() ?: return fallback
                return Result.success(
                    report.copy(
                        sourceType = AnalysisSourceType.UsdaPlusOcr,
                        warnings = report.warnings + error + " Falling back to image analysis.",
                        scanResult = report.scanResult.copy(
                            sourceLabel = "USDA+Image",
                            warnings = report.scanResult.warnings + error +
                                " Falling back to image analysis.",
                        ),
                    ),
                )
            }
        }
        return Result.failure(Exception(error.toFriendlyAnalysisMessage("Analysis unavailable.")))
    }

    private suspend fun classifyFromIngredientsTextApiOnly(
        rawText: String,
        sourceImagePath: String?,
        modelId: String,
        sourceLabel: String,
        sourceType: AnalysisSourceType,
        productNameOverride: String?,
        warnings: List<String>,
        scannedBarcode: String? = null,
        brandOwner: String? = null,
        allergens: List<String> = emptyList(),
        rawIngredientText: String = rawText,
        onStage: (AnalysisStage) -> Unit = {},
        onStatus: (String) -> Unit = {},
    ): Result<AnalysisReport> {
        val normalized = IngredientTextNormalizer.normalize(rawText)
        if (normalized.length < MIN_NORMALIZED_LENGTH) {
            return Result.failure(
                Exception("Could not read enough ingredient text. Please try again."),
            )
        }

        val workflow = llmWorkflow ?: return Result.failure(
            Exception("API-only mode requires a configured LLM workflow."),
        )

        val extraction = IngredientExtraction(
            code = 0,
            productName = productNameOverride ?: "Scanned food label",
            rawText = rawText,
            ingredients = normalized
                .split(',', ';')
                .map { it.trim() }
                .filter { it.isNotBlank() },
            confidence = 0.6f,
            warnings = emptyList(),
        )

        onStage(AnalysisStage.AnalysingIngredients)
        val classification = runLlmStage(
            stageName = "llm_classify_from_text",
            timeoutMillis = CLASSIFICATION_TIMEOUT_MILLIS,
            timeoutMessage = "API text classification timed out.",
        ) {
            workflow.classifyIngredients(extraction, modelId, onStatus)
        }.getOrElse { error ->
            return Result.failure(
                Exception(
                    error.toFriendlyAnalysisMessage("API text classification unavailable."),
                ),
            )
        }
        val allergens = runLlmStage(
            stageName = "llm_detect_allergens_from_text",
            timeoutMillis = ALLERGEN_TIMEOUT_MILLIS,
            timeoutMessage = "API allergen detection timed out.",
        ) {
            workflow.detectAllergens(extraction, modelId, onStatus)
        }.getOrElse {
            AllergenDetection(
                allergens = emptyList(),
                warnings = listOf(it.toFriendlyAnalysisMessage("Allergen detection unavailable.")),
                confidence = 0f,
            )
        }
        val scanResult = ClassificationUiMapper.toScanResultUi(
            classification = classification.toClassificationResult(),
            normalizedIngredientLine = normalized,
            productNameOverride = productNameOverride,
            sourceLabel = sourceLabel,
            warnings = warnings + classification.warnings + allergens.warnings,
            labelImagePath = sourceImagePath,
            scannedBarcode = scannedBarcode,
            brandOwner = brandOwner,
            allergens = allergens.allergens,
            rawIngredientText = rawIngredientText,
            usageEstimate = UsageEstimateCalculator.estimateTextWorkflow(
                modelId = modelId,
                ingredientText = rawIngredientText,
                problemIngredientCount = classification.problemIngredients.size,
                allergenCount = allergens.allergens.size,
            ),
        )
        onStage(AnalysisStage.Completed)
        return Result.success(
            AnalysisReport(
                sourceType = sourceType,
                productName = scanResult.productName,
                ingredientsTextUsed = normalized,
                warnings = warnings + classification.warnings + allergens.warnings,
                scanResult = scanResult,
            ),
        )
    }

    companion object {
        const val INVALID_IMAGE_CODE: Int = -1
        const val INVALID_IMAGE_ERROR: String =
            "Invalid image. Please scan a food ingredient box or ingredient list."
        const val MIN_NORMALIZED_LENGTH: Int = 12
        const val DEFAULT_VLM_MODEL_ID: String = "gemini-2.0-flash"
        private const val EXTRACTION_TIMEOUT_MILLIS = 25_000L
        private const val CLASSIFICATION_TIMEOUT_MILLIS = 18_000L
        private const val ALLERGEN_TIMEOUT_MILLIS = 12_000L

        fun create(context: Context): FoodAnalysisPipeline {
            val appContext = context.applicationContext
            return FoodAnalysisPipeline(
                ocrPipeline = MlKitOcrPipeline(appContext),
                barcodeScanner = MlKitBarcodeScanner(appContext),
                usdaRepository = UsdaRepository(
                    dataSource = UsdaApiService(
                        apiKeyProvider = SecretUsdaApiKeyProvider(
                            SecretKeyManager(appContext),
                        ),
                        client = UsdaHttpClientFactory.create(),
                    ),
                ),
                llmWorkflow = MultiProviderFoodLabelLlmWorkflow(
                    geminiWorkflow = GeminiFoodLabelLlmWorkflow(
                        context = appContext,
                        apiKeyProvider = SecretLlmApiKeyProvider(
                            SecretKeyManager(appContext),
                        ),
                    ),
                    openAiWorkflow = OpenAiCompatibleFoodLabelLlmWorkflow(
                        context = appContext,
                        apiKeyProvider = SecretLlmApiKeyProvider(
                            SecretKeyManager(appContext),
                        ),
                        baseUrl = "https://api.openai.com/v1",
                        providerTag = "openai",
                    ),
                    grokWorkflow = OpenAiCompatibleFoodLabelLlmWorkflow(
                        context = appContext,
                        apiKeyProvider = SecretLlmApiKeyProvider(
                            SecretKeyManager(appContext),
                        ),
                        baseUrl = "https://api.x.ai/v1",
                        providerTag = "grok",
                    ),
                    groqWorkflow = OpenAiCompatibleFoodLabelLlmWorkflow(
                        context = appContext,
                        apiKeyProvider = SecretLlmApiKeyProvider(
                            SecretKeyManager(appContext),
                        ),
                        baseUrl = "https://api.groq.com/openai/v1",
                        providerTag = "groq",
                    ),
                ),
            )
        }
    }

    private suspend fun analyzeImageWithLlmWorkflow(
        workflow: FoodLabelLlmWorkflow,
        imagePath: String,
        modelId: String,
        onStage: (AnalysisStage) -> Unit,
        onStatus: (String) -> Unit,
    ): Result<AnalysisReport>? {
        onStage(AnalysisStage.ExtractingIngredients)
        val extraction = runLlmStage(
            stageName = "llm_extract_ingredients",
            timeoutMillis = EXTRACTION_TIMEOUT_MILLIS,
            timeoutMessage = "Ingredient extraction timed out. Please try again with a clearer label image.",
        ) {
            workflow.extractIngredients(imagePath, modelId, onStatus)
        }.getOrElse { error ->
            if (error is LlmStageTimeoutException) {
                return Result.failure(Exception(error.message.toFriendlyAnalysisMessage(INVALID_IMAGE_ERROR)))
            }
            AnalysisTelemetry.event("llm_extraction_unavailable")
            AnalysisTelemetry.event("llm_extraction_error=${error.message.orEmpty()}")
            onStatus("The AI response could not be parsed after several retries. Falling back to OCR.")
            return null
        }
        if (extraction.code == INVALID_IMAGE_CODE) {
            AnalysisTelemetry.event("invalid_image")
            return Result.failure(Exception(INVALID_IMAGE_ERROR))
        }
        val ingredientsText = extraction.ingredients.joinToString(", ")
            .ifBlank { extraction.rawText }
        if (IngredientTextNormalizer.normalize(ingredientsText).length < MIN_NORMALIZED_LENGTH) {
            return Result.failure(Exception(INVALID_IMAGE_ERROR))
        }

        onStage(AnalysisStage.AnalysingIngredients)
        val (classificationResult, allergenResult) = coroutineScope {
            val classification = async {
                runLlmStage(
                    stageName = "llm_classify_ingredients",
                    timeoutMillis = CLASSIFICATION_TIMEOUT_MILLIS,
                    timeoutMessage = "Ingredient analysis timed out.",
                ) {
                    workflow.classifyIngredients(extraction, modelId, onStatus)
                }
            }
            val allergens = async {
                runLlmStage(
                    stageName = "llm_detect_allergens",
                    timeoutMillis = ALLERGEN_TIMEOUT_MILLIS,
                    timeoutMessage = "Allergen detection timed out for this scan.",
                ) {
                    workflow.detectAllergens(extraction, modelId, onStatus)
                }
            }
            classification.await() to allergens.await()
        }
        val classification = classificationResult.getOrNull()
        val allergens = allergenResult.getOrElse { error ->
            AllergenDetection(
                allergens = emptyList(),
                warnings = listOf(error.toFriendlyAnalysisMessage("Allergen detection was unavailable for this scan.")),
                confidence = 0f,
            )
        }

        return if (classification != null) {
            onStage(AnalysisStage.Completed)
            Result.success(
                buildLlmAnalysisReport(
                    imagePath = imagePath,
                    modelId = modelId,
                    extraction = extraction,
                    classification = classification,
                    allergens = allergens,
                ),
            )
        } else {
            val classificationWarning = classificationResult.exceptionOrNull()?.message
                ?.toFriendlyAnalysisMessage("LLM classification was unavailable.")
                ?: "LLM classification was unavailable."
            classifyFromIngredientsTextApiOnly(
                rawText = ingredientsText,
                sourceImagePath = imagePath,
                modelId = modelId,
                sourceLabel = "LLM extraction + API text",
                sourceType = AnalysisSourceType.Vlm,
                productNameOverride = extraction.productName,
                warnings = extraction.warnings +
                    allergens.warnings +
                    classificationWarning,
                allergens = allergens.allergens,
                rawIngredientText = extraction.rawText.ifBlank { ingredientsText },
                onStage = onStage,
                onStatus = onStatus,
            )
        }
    }

    private fun buildLlmAnalysisReport(
        imagePath: String,
        modelId: String,
        extraction: IngredientExtraction,
        classification: IngredientClassification,
        allergens: AllergenDetection,
    ): AnalysisReport {
        val ingredientsText = extraction.ingredients.joinToString(", ")
            .ifBlank { extraction.rawText }
        val warnings = extraction.warnings + classification.warnings + allergens.warnings
        val scanResult = ScanResultUi(
            productName = extraction.productName,
            novaGroup = classification.novaGroup,
            summary = classification.summary,
            problemIngredients = classification.problemIngredients.map {
                ProblemIngredient(name = it.name, reason = it.reason)
            },
            allIngredients = extraction.ingredients.ifEmpty {
                IngredientTextNormalizer.normalize(ingredientsText)
                    .split(',', ';')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            },
            engineLabel = "Gemini staged LLM ($modelId)",
            confidence = minOf(extraction.confidence, classification.confidence),
            sourceLabel = "LLM image workflow",
            warnings = warnings,
            labelImagePath = imagePath,
            allergens = allergens.allergens,
            rawIngredientText = extraction.rawText.ifBlank { ingredientsText },
            usageEstimate = UsageEstimateCalculator.estimateImageWorkflow(
                modelId = modelId,
                ingredientText = ingredientsText,
                problemIngredientCount = classification.problemIngredients.size,
                allergenCount = allergens.allergens.size,
            ),
        )
        return AnalysisReport(
            sourceType = AnalysisSourceType.Vlm,
            productName = scanResult.productName,
            ingredientsTextUsed = ingredientsText,
            warnings = warnings,
            scanResult = scanResult,
        )
    }

}

private class LlmStageTimeoutException(message: String) : Exception(message)

private suspend fun <T> runLlmStage(
    stageName: String,
    timeoutMillis: Long,
    timeoutMessage: String,
    block: suspend () -> Result<T>,
): Result<T> {
    val startedAt = AnalysisTelemetry.markStart()
    return try {
        val result = withTimeout(timeoutMillis) { block() }
        result
            .onSuccess { AnalysisTelemetry.stageSucceeded(stageName, startedAt) }
            .onFailure {
                AnalysisTelemetry.stageFailed(
                    stageName,
                    startedAt,
                    it::class.simpleName ?: "unknown",
                )
            }
    } catch (e: TimeoutCancellationException) {
        AnalysisTelemetry.stageFailed(stageName, startedAt, "timeout")
        Result.failure(LlmStageTimeoutException(timeoutMessage))
    }
}

private fun IngredientClassification.toClassificationResult(): ClassificationResult =
    ClassificationResult(
        novaGroup = novaGroup,
        confidence = confidence,
        markers = problemIngredients.map { it.name },
        explanation = summary,
        highlightTerms = problemIngredients.map { it.name },
        engine = "Gemini staged LLM",
        ingredientAssessments = ingredientAssessments,
    )

private fun Throwable.toFriendlyAnalysisMessage(defaultMessage: String): String {
    val message = message.orEmpty().trim()
    val lower = message.lowercase()
    return when {
        this is LlmStageTimeoutException -> message.ifBlank { defaultMessage }
        lower.contains("could not be validated after") ||
            lower.contains("failed after contract retries") ||
            lower.contains("llm response") ||
            lower.contains("invalid json") ||
            lower.contains("missing required field") ||
            lower.contains("incomplete") ||
            lower.contains("unsupported code") ||
            lower.contains("no usable ingredient list") ->
            "The AI returned an unreadable response after several retries. Please try again."
        lower.contains("429") ||
            lower.contains("rate limit") ||
            lower.contains("quota exceeded") ->
            "The AI service is temporarily busy. Please wait a moment and try again."
        message.isNotBlank() -> message
        else -> defaultMessage
    }
}

private fun String?.toFriendlyAnalysisMessage(defaultMessage: String): String =
    this.orEmpty().ifBlank { defaultMessage }
