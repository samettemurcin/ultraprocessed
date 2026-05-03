package com.b2.ultraprocessed.network.llm

import com.b2.ultraprocessed.classify.IngredientAssessment

interface FoodLabelLlmWorkflow {
    suspend fun extractIngredients(
        imagePath: String,
        modelId: String,
        onStatus: (String) -> Unit = {},
    ): Result<IngredientExtraction>

    suspend fun classifyIngredients(
        extraction: IngredientExtraction,
        modelId: String,
        onStatus: (String) -> Unit = {},
    ): Result<IngredientClassification>

    suspend fun detectAllergens(
        extraction: IngredientExtraction,
        modelId: String,
        onStatus: (String) -> Unit = {},
    ): Result<AllergenDetection>
}

data class IngredientExtraction(
    val code: Int,
    val productName: String,
    val rawText: String,
    val ingredients: List<String>,
    val confidence: Float,
    val warnings: List<String>,
)

data class IngredientClassification(
    val novaGroup: Int,
    val summary: String,
    val confidence: Float,
    val problemIngredients: List<IngredientRiskMarker>,
    val warnings: List<String>,
    val ingredientAssessments: List<IngredientAssessment> = emptyList(),
)

data class IngredientRiskMarker(
    val name: String,
    val reason: String,
)

data class AllergenDetection(
    val allergens: List<String>,
    val warnings: List<String>,
    val confidence: Float,
)
