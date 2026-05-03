package com.b2.ultraprocessed.classify

data class IngredientAssessment(
    val name: String,
    val novaGroup: Int,
    val reason: String,
)

data class ClassificationResult(
    val novaGroup: Int,
    val confidence: Float,
    val markers: List<String>,
    val explanation: String,
    val highlightTerms: List<String>,
    val engine: String,
    val ingredientAssessments: List<IngredientAssessment> = emptyList(),
)
