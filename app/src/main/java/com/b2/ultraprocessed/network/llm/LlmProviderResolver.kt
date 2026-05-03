package com.b2.ultraprocessed.network.llm

data class LlmProviderModelMetadata(
    val modelId: String,
    val modelName: String,
    val provider: String,
    val acceptsImages: Boolean,
)

object LlmProviderResolver {
    fun detectProvider(apiKey: String): String? {
        val key = apiKey.trim().lowercase()
        if (key.isBlank()) return null
        return when {
            key.startsWith("xai-") -> "grok"
            key.startsWith("gsk_") -> "groq"
            key.startsWith("sk-proj-") || key.startsWith("sk-") -> "openai"
            key.startsWith("aiza") -> "gemini"
            else -> null
        }
    }

    fun defaultModelForProvider(provider: String): LlmProviderModelMetadata? =
        when (provider.lowercase()) {
            "gemini" -> LlmProviderModelMetadata(
                modelId = "gemini-2.0-flash",
                modelName = "Gemini 2.0 Flash",
                provider = "Gemini (Google)",
                acceptsImages = true,
            )
            "openai" -> LlmProviderModelMetadata(
                modelId = "gpt-4.1-mini",
                modelName = "GPT-4.1 mini",
                provider = "OpenAI",
                acceptsImages = true,
            )
            "grok" -> LlmProviderModelMetadata(
                modelId = "grok-2-vision-latest",
                modelName = "Grok 2 Vision",
                provider = "Grok (xAI)",
                acceptsImages = true,
            )
            "groq" -> LlmProviderModelMetadata(
                modelId = "llama-3.1-8b-instant",
                modelName = "Llama 3.1 8B Instant",
                provider = "Groq",
                acceptsImages = false,
            )
            else -> null
        }

    fun metadataFromModelId(modelId: String): LlmProviderModelMetadata? {
        val normalized = modelId.trim().lowercase()
        return when {
            normalized.startsWith("gemini-") -> defaultModelForProvider("gemini")
            normalized.startsWith("gpt-") || normalized.startsWith("o1") || normalized.startsWith("o3") ->
                defaultModelForProvider("openai")
            normalized.startsWith("grok-") -> defaultModelForProvider("grok")
            normalized.startsWith("llama-") || normalized.startsWith("mixtral-") ||
                normalized.startsWith("gemma-") -> defaultModelForProvider("groq")
            else -> null
        }
    }
}
