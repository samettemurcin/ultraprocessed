package com.b2.ultraprocessed.analysis

import com.b2.ultraprocessed.barcode.BarcodeResult
import com.b2.ultraprocessed.barcode.BarcodeScanner
import com.b2.ultraprocessed.network.usda.UsdaApiDataSource
import com.b2.ultraprocessed.network.usda.UsdaFoodDetail
import com.b2.ultraprocessed.network.usda.UsdaRepository
import com.b2.ultraprocessed.network.usda.UsdaSearchFood
import com.b2.ultraprocessed.network.llm.AllergenDetection
import com.b2.ultraprocessed.network.llm.FoodLabelLlmWorkflow
import com.b2.ultraprocessed.network.llm.IngredientClassification
import com.b2.ultraprocessed.classify.IngredientAssessment
import com.b2.ultraprocessed.network.llm.IngredientExtraction
import com.b2.ultraprocessed.network.llm.IngredientRiskMarker
import com.b2.ultraprocessed.ocr.OcrPipeline
import com.b2.ultraprocessed.ocr.OcrResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodAnalysisPipelineTest {
    @Test
    fun analyzeFromImage_runsStagedLlmWorkflow_whenAvailable() = runTest {
        val pipeline = buildPipeline(
            ocrPipeline = OcrPipeline { OcrResult.Failure("OCR should not run") },
            barcodeScanner = BarcodeScanner { BarcodeResult.Failure("unused") },
            usdaRepository = emptyUsdaRepository(),
            llmWorkflow = FakeFoodLabelLlmWorkflow(),
        )

        val result = pipeline.analyzeFromImage("/tmp/fake-image.jpg", "gemini-2.0-flash").getOrThrow()

        assertEquals(AnalysisSourceType.Vlm, result.sourceType)
        assertEquals("AI Read Snack", result.productName)
        assertEquals("LLM image workflow", result.scanResult.sourceLabel)
        assertEquals(listOf("Milk", "Wheat"), result.scanResult.allergens)
        assertEquals("Ingredients: sugar, wheat flour, milk, artificial flavor", result.scanResult.rawIngredientText)
    }

    @Test
    fun analyzeFromImage_fallsBackToOcr_whenLlmExtractionFails() = runTest {
        val pipeline = buildPipeline(
            ocrPipeline = OcrPipeline { OcrResult.Success("Ingredients: corn, salt, sunflower oil") },
            barcodeScanner = BarcodeScanner { BarcodeResult.Failure("unused") },
            usdaRepository = emptyUsdaRepository(),
            llmWorkflow = FakeFoodLabelLlmWorkflow(extractionFailure = true),
        )

        val result = pipeline.analyzeFromImage("/tmp/fake-image.jpg", "gemini-2.0-flash").getOrThrow()

        assertEquals(AnalysisSourceType.Ocr, result.sourceType)
        assertEquals("OCR", result.scanResult.sourceLabel)
        assertTrue(result.warnings.any { it.contains("LLM image workflow") })
    }

    @Test
    fun analyzeFromImage_stopsWithInvalidImageError_whenExtractionRejectsImage() = runTest {
        val pipeline = buildPipeline(
            ocrPipeline = OcrPipeline { OcrResult.Success("Ingredients: should not run") },
            barcodeScanner = BarcodeScanner { BarcodeResult.Failure("unused") },
            usdaRepository = emptyUsdaRepository(),
            llmWorkflow = FakeFoodLabelLlmWorkflow(invalidImage = true),
        )

        val result = pipeline.analyzeFromImage("/tmp/fake-image.jpg", "gemini-2.0-flash")

        assertTrue(result.isFailure)
        assertEquals(FoodAnalysisPipeline.INVALID_IMAGE_ERROR, result.exceptionOrNull()?.message)
    }

    @Test
    fun analyzeFromImage_stopsWithInvalidImageError_whenExtractionHasNoUsableIngredients() = runTest {
        val pipeline = buildPipeline(
            ocrPipeline = OcrPipeline { OcrResult.Success("Ingredients: should not run") },
            barcodeScanner = BarcodeScanner { BarcodeResult.Failure("unused") },
            usdaRepository = emptyUsdaRepository(),
            llmWorkflow = FakeFoodLabelLlmWorkflow(emptyExtraction = true),
        )

        val result = pipeline.analyzeFromImage("/tmp/fake-image.jpg", "gemini-2.0-flash")

        assertTrue(result.isFailure)
        assertEquals(FoodAnalysisPipeline.INVALID_IMAGE_ERROR, result.exceptionOrNull()?.message)
    }

    @Test
    fun analyzeFromBarcode_producesReportFromUsdaIngredients() = runTest {
        val pipeline = buildPipeline(
            ocrPipeline = OcrPipeline { OcrResult.Failure("unused") },
            barcodeScanner = BarcodeScanner { BarcodeResult.Success("078742195760") },
            usdaRepository = UsdaRepository(
                object : UsdaApiDataSource {
                    override suspend fun searchFoods(query: String, pageSize: Int): List<UsdaSearchFood> = listOf(
                        UsdaSearchFood(
                            fdcId = 100L,
                            description = "Frozen Cheeseburger",
                            dataType = "Branded",
                            brandOwner = "Great Value",
                            gtinUpc = "078742195760",
                            ingredients = "BEEF, BUN, ARTIFICIAL FLAVOR",
                        ),
                    )

                    override suspend fun fetchFoodDetail(fdcId: Long): UsdaFoodDetail? = UsdaFoodDetail(
                        fdcId = 100L,
                        description = "Frozen Cheeseburger",
                        brandOwner = "Great Value",
                        gtinUpc = "078742195760",
                        ingredients = "BEEF, BUN, ARTIFICIAL FLAVOR",
                    )
                },
            ),
            llmWorkflow = FakeFoodLabelLlmWorkflow(),
        )

        val result = pipeline.analyzeFromBarcodeImage("/tmp/fake-image.jpg").getOrThrow()
        assertEquals(AnalysisSourceType.Barcode, result.sourceType)
        assertEquals("Barcode → USDA", result.scanResult.sourceLabel)
    }

    @Test
    fun analyzeFromBarcode_withRawCode_samePathAsImageBarcodeFlow() = runTest {
        val pipeline = buildPipeline(
            ocrPipeline = OcrPipeline { OcrResult.Failure("unused") },
            barcodeScanner = BarcodeScanner { BarcodeResult.Failure("unused") },
            usdaRepository = UsdaRepository(
                object : UsdaApiDataSource {
                    override suspend fun searchFoods(query: String, pageSize: Int): List<UsdaSearchFood> = listOf(
                        UsdaSearchFood(
                            fdcId = 100L,
                            description = "Frozen Cheeseburger",
                            dataType = "Branded",
                            brandOwner = "Great Value",
                            gtinUpc = "078742195760",
                            ingredients = "BEEF, BUN, ARTIFICIAL FLAVOR",
                        ),
                    )

                    override suspend fun fetchFoodDetail(fdcId: Long): UsdaFoodDetail? = UsdaFoodDetail(
                        fdcId = 100L,
                        description = "Frozen Cheeseburger",
                        brandOwner = "Great Value",
                        gtinUpc = "078742195760",
                        ingredients = "BEEF, BUN, ARTIFICIAL FLAVOR",
                    )
                },
            ),
            llmWorkflow = FakeFoodLabelLlmWorkflow(),
        )

        val result = pipeline.analyzeFromBarcode("078742195760", sourceImagePath = null).getOrThrow()
        assertEquals(AnalysisSourceType.Barcode, result.sourceType)
        assertEquals("Barcode → USDA", result.scanResult.sourceLabel)
        assertEquals("078742195760", result.scanResult.scannedBarcode)
        assertEquals("Great Value", result.scanResult.brandOwner)
        assertFalse(result.scanResult.isBarcodeLookupOnly)
        assertTrue(result.scanResult.allIngredients.isNotEmpty())
    }

    @Test
    fun analyzeFromBarcode_fallsBackToOcr_whenUsdaMisses() = runTest {
        val pipeline = buildPipeline(
            ocrPipeline = OcrPipeline {
                OcrResult.Success("Ingredients: corn, salt, sunflower oil")
            },
            barcodeScanner = BarcodeScanner { BarcodeResult.Success("999999") },
            usdaRepository = UsdaRepository(
                object : UsdaApiDataSource {
                    override suspend fun searchFoods(query: String, pageSize: Int): List<UsdaSearchFood> = emptyList()
                    override suspend fun fetchFoodDetail(fdcId: Long): UsdaFoodDetail? = null
                },
            ),
            llmWorkflow = FakeFoodLabelLlmWorkflow(),
        )

        val result = pipeline.analyzeFromBarcodeImage("/tmp/fake-image.jpg").getOrThrow()
        assertEquals(AnalysisSourceType.UsdaPlusOcr, result.sourceType)
        assertTrue(result.scanResult.warnings.isNotEmpty())
    }

    @Test
    fun analyzeFromImage_returnsFailure_whenNoOcrText() = runTest {
        val pipeline = buildPipeline(
            ocrPipeline = OcrPipeline { OcrResult.Failure("No text detected in image.") },
            barcodeScanner = BarcodeScanner { BarcodeResult.Failure("unused") },
            usdaRepository = UsdaRepository(
                object : UsdaApiDataSource {
                    override suspend fun searchFoods(query: String, pageSize: Int): List<UsdaSearchFood> = emptyList()
                    override suspend fun fetchFoodDetail(fdcId: Long): UsdaFoodDetail? = null
                },
            ),
        )

        val result = pipeline.analyzeFromImage("/tmp/fake-image.jpg")
        assertTrue(result.isFailure)
    }

}

private fun buildPipeline(
    ocrPipeline: OcrPipeline,
    barcodeScanner: BarcodeScanner,
    usdaRepository: UsdaRepository,
    llmWorkflow: FoodLabelLlmWorkflow? = null,
): FoodAnalysisPipeline = FoodAnalysisPipeline(
    ocrPipeline = ocrPipeline,
    barcodeScanner = barcodeScanner,
    usdaRepository = usdaRepository,
    llmWorkflow = llmWorkflow,
)

private fun emptyUsdaRepository(): UsdaRepository =
    UsdaRepository(
        object : UsdaApiDataSource {
            override suspend fun searchFoods(query: String, pageSize: Int): List<UsdaSearchFood> = emptyList()
            override suspend fun fetchFoodDetail(fdcId: Long): UsdaFoodDetail? = null
        },
    )

private class FakeFoodLabelLlmWorkflow(
    private val extractionFailure: Boolean = false,
    private val invalidImage: Boolean = false,
    private val emptyExtraction: Boolean = false,
) : FoodLabelLlmWorkflow {
    override suspend fun extractIngredients(
        imagePath: String,
        modelId: String,
        onStatus: (String) -> Unit,
    ): Result<IngredientExtraction> {
        if (extractionFailure) return Result.failure(Exception("No key"))
        if (invalidImage) {
            return Result.success(
                IngredientExtraction(
                    code = FoodAnalysisPipeline.INVALID_IMAGE_CODE,
                    productName = "Invalid image",
                    rawText = "",
                    ingredients = emptyList(),
                    confidence = 0f,
                    warnings = listOf(FoodAnalysisPipeline.INVALID_IMAGE_ERROR),
                ),
            )
        }
        if (emptyExtraction) {
            return Result.success(
                IngredientExtraction(
                    code = 0,
                    productName = "Empty Label",
                    rawText = "",
                    ingredients = emptyList(),
                    confidence = 0.6f,
                    warnings = emptyList(),
                ),
            )
        }
        return Result.success(
            IngredientExtraction(
                code = 0,
                productName = "AI Read Snack",
                rawText = "Ingredients: sugar, wheat flour, milk, artificial flavor",
                ingredients = listOf("Sugar", "Wheat Flour", "Milk", "Artificial Flavor"),
                confidence = 0.9f,
                warnings = emptyList(),
            ),
        )
    }

    override suspend fun classifyIngredients(
        extraction: IngredientExtraction,
        modelId: String,
        onStatus: (String) -> Unit,
    ): Result<IngredientClassification> =
        Result.success(
            IngredientClassification(
                novaGroup = 4,
                summary = "The staged classifier found ultra-processed ingredient markers.",
                confidence = 0.82f,
                problemIngredients = listOf(
                    IngredientRiskMarker(
                        name = "Artificial Flavor",
                        reason = "Flavor systems are a NOVA 4 processing marker.",
                    ),
                ),
                warnings = emptyList(),
                ingredientAssessments = extraction.ingredients.map {
                    IngredientAssessment(
                        name = it,
                        novaGroup = 4,
                        reason = "Test fixture NOVA 4 ingredient.",
                    )
                },
            ),
        )

    override suspend fun detectAllergens(
        extraction: IngredientExtraction,
        modelId: String,
        onStatus: (String) -> Unit,
    ): Result<AllergenDetection> =
        Result.success(
            AllergenDetection(
                allergens = listOf("Milk", "Wheat"),
                warnings = emptyList(),
                confidence = 0.88f,
            ),
        )
}
