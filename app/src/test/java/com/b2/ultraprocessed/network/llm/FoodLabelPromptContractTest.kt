package com.b2.ultraprocessed.network.llm

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodLabelPromptContractTest {
    @Test
    fun extractionPrompt_requiresIngredientPanelAndInvalidImageCode() {
        val prompt = promptText("food_label_ingredient_extraction_prompt.md")

        assertTrue(prompt.contains("ingredient box or ingredient list", ignoreCase = true))
        assertTrue(prompt.contains("code = -1", ignoreCase = true))
        assertTrue(prompt.contains("Do not infer ingredients from product name", ignoreCase = true))
        assertTrue(prompt.contains("front-of-pack photos", ignoreCase = true))
        assertTrue(prompt.contains("generic food photos", ignoreCase = true))
        assertTrue(prompt.contains("\"code\": 0"))
    }

    @Test
    fun classificationPrompt_usesOnlyExtractedIngredients() {
        val prompt = promptText("food_label_classification_prompt.md")

        assertTrue(prompt.contains("Use only `rawIngredientText` and `ingredients`", ignoreCase = true))
        assertTrue(prompt.contains("OCR / Noisy Input Note", ignoreCase = true))
        assertTrue(prompt.contains("do not invent missing ingredients", ignoreCase = true))
        assertTrue(prompt.contains("Choose the lowest NOVA group", ignoreCase = true))
        assertTrue(prompt.contains("ingredientAssessments", ignoreCase = true))
        assertTrue(prompt.contains("novaGroup", ignoreCase = true))
        assertTrue(prompt.contains("Allergen detection is a separate API call", ignoreCase = true))
        assertTrue(prompt.contains("Do not use allergen logic", ignoreCase = true))
    }

    @Test
    fun allergenPrompt_usesOnlyExtractedIngredients() {
        val prompt = promptText("food_label_allergen_prompt.md")

        assertTrue(prompt.contains("Use only `rawIngredientText` and `ingredients`", ignoreCase = true))
        assertTrue(prompt.contains("OCR / Noisy Input Note", ignoreCase = true))
        assertTrue(prompt.contains("do not guess from product name", ignoreCase = true))
        assertTrue(prompt.contains("Do not infer from shared-facility claims", ignoreCase = true))
    }

    @Test
    fun resultChatPrompt_refusesOffTopicAndInjection() {
        val prompt = promptText("food_label_result_chat_prompt.md")

        assertTrue(prompt.contains("answer only questions about this one scan result", ignoreCase = true))
        assertTrue(prompt.contains("prompt injection", ignoreCase = true))
        assertTrue(prompt.contains("Do not answer questions about other products", ignoreCase = true))
        assertTrue(prompt.contains("\"allowed\": true", ignoreCase = true))
    }

    private fun promptText(fileName: String): String =
        File("src/main/assets/prompts/$fileName").readText()
}
