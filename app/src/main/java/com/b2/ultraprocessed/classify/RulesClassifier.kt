package com.b2.ultraprocessed.classify

class RulesClassifier : Classifier {
    override val id: String = "rules"

    private val nova4Markers = listOf(
        "high fructose corn syrup", "maltodextrin", "artificial flavor",
        "artificial colour", "artificial color", "sodium benzoate",
        "potassium sorbate", "carrageenan", "modified starch",
        "emulsifier", "stabilizer", "xanthan gum", "soy lecithin",
        "mono and diglycerides", "sodium nitrate", "sodium nitrite",
        "bha", "bht", "tbhq", "acesulfame", "aspartame", "sucralose",
        "saccharin", "natural flavor", "color added", "caramel color",
        "partially hydrogenated", "interesterified", "hydrolyzed",
        "autolyzed yeast", "disodium", "monosodium glutamate", "msg"
    )

    private val nova3Markers = listOf(
        "vinegar", "butter", "cream", "cheese", "yeast",
        "baking powder", "corn syrup", "preservative",
        "citric acid", "lactic acid", "cane sugar"
    )

    private val nova1Markers = listOf(
        "water", "milk", "egg", "flour", "oat", "oats",
        "fruit", "vegetable", "meat", "fish", "nuts",
        "seeds", "honey", "olive oil", "coconut oil",
        "almonds", "dates", "banana", "apple", "tomato"
    )

    override suspend fun classify(
        input: IngredientInput,
        context: ClassificationContext,
    ): ClassificationResult {
        val normalized = input.normalizedText.lowercase()

        val detectedNova4 = nova4Markers.filter { normalized.contains(it) }
        val detectedNova3 = nova3Markers.filter { normalized.contains(it) }
        val detectedNova1 = nova1Markers.filter { normalized.contains(it) }

        val novaGroup: Int
        val confidence: Float
        val explanation: String

        when {
            detectedNova4.size >= 3 -> {
                novaGroup = 4
                confidence = 0.92f
                explanation = "Multiple ultra-processing markers detected: ${detectedNova4.take(3).joinToString(", ")}."
            }
            detectedNova4.size >= 1 -> {
                novaGroup = 4
                confidence = 0.78f
                explanation = "Ultra-processing marker detected: ${detectedNova4.joinToString(", ")}."
            }
            detectedNova3.size >= 2 && detectedNova4.isEmpty() -> {
                novaGroup = 3
                confidence = 0.70f
                explanation = "Processed food markers detected: ${detectedNova3.take(3).joinToString(", ")}."
            }
            detectedNova1.isNotEmpty() && detectedNova4.isEmpty() && detectedNova3.size <= 1 -> {
                novaGroup = 1
                confidence = 0.55f
                explanation = "Mostly unprocessed ingredients: ${detectedNova1.take(3).joinToString(", ")}."
            }
            else -> {
                novaGroup = 2
                confidence = 0.55f
                explanation = "Insufficient markers. Likely minimally processed."
            }
        }

        return ClassificationResult(
            novaGroup = novaGroup,
            confidence = confidence,
            markers = detectedNova4 + detectedNova3,
            explanation = explanation,
            highlightTerms = detectedNova4 + detectedNova3,
            engine = id,
        )
    }
}
