package com.b2.ultraprocessed.ui

import com.b2.ultraprocessed.classify.ClassificationResult
import com.b2.ultraprocessed.classify.IngredientAssessment

object ClassificationUiMapper {
    fun toScanResultUi(
        classification: ClassificationResult,
        normalizedIngredientLine: String,
        productNameOverride: String? = null,
        sourceLabel: String = "OCR",
        warnings: List<String> = emptyList(),
        labelImagePath: String? = null,
        scannedBarcode: String? = null,
        brandOwner: String? = null,
        allergens: List<String> = emptyList(),
        rawIngredientText: String = normalizedIngredientLine,
        usageEstimate: UsageEstimateUi? = null,
    ): ScanResultUi {
        val productName = productNameOverride?.takeIf { it.isNotBlank() }
            ?: deriveProductTitle(normalizedIngredientLine)
        val problemIngredients = classification.highlightTerms.map { marker ->
            val isPackagingCue = marker.startsWith("packaging:")
            val displayName = if (isPackagingCue) {
                titleCasePhrase(marker.removePrefix("packaging:").trim())
            } else {
                titleCasePhrase(marker)
            }
            ProblemIngredient(
                name = if (isPackagingCue) "Packaging: $displayName" else displayName,
                reason = if (isPackagingCue) {
                    "Matched a front-of-box or reheating line OCR often reads instead of the ingredient panel."
                } else {
                    "Flagged by the API analysis as a stronger NOVA signal."
                },
            )
        }

        val allIngredients = splitIngredientList(normalizedIngredientLine)
        val ingredientAssessments = classification.ingredientAssessments.map { it.toUiAssessment() }
        val engineLabel = classification.engine

        return ScanResultUi(
            productName = productName,
            novaGroup = classification.novaGroup,
            summary = classification.explanation,
            problemIngredients = problemIngredients,
            allIngredients = allIngredients,
            engineLabel = engineLabel,
            confidence = classification.confidence,
            sourceLabel = sourceLabel,
            warnings = warnings,
            allergens = allergens,
            ingredientAssessments = ingredientAssessments,
            rawIngredientText = rawIngredientText,
            labelImagePath = labelImagePath,
            scannedBarcode = scannedBarcode,
            brandOwner = brandOwner,
            usageEstimate = usageEstimate,
        )
    }

    private fun deriveProductTitle(normalized: String): String {
        val snippet = normalized.trim().take(48)
        return if (snippet.isEmpty()) {
            "Scanned label"
        } else {
            snippet + if (normalized.length > 48) "…" else ""
        }
    }

    private fun splitIngredientList(normalized: String): List<String> =
        normalized.split(',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun IngredientAssessment.toUiAssessment(): IngredientBubbleUi =
        IngredientBubbleUi(
            name = name,
            novaGroup = novaGroup.coerceIn(1, 4),
            reason = reason,
        )

    private fun titleCasePhrase(phrase: String): String =
        phrase.split(' ')
            .joinToString(" ") { word ->
                word.replaceFirstChar { c -> c.titlecaseChar() }
            }
}
