package com.b2.ultraprocessed.network.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import org.json.JSONObject

data class LlmApiKeyVerificationResult(
    val valid: Boolean,
    val message: String,
)

class LlmApiKeyVerifier(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun ping(apiKey: String): LlmApiKeyVerificationResult = withContext(Dispatchers.IO) {
        val provider = LlmProviderResolver.detectProvider(apiKey)
            ?: return@withContext LlmApiKeyVerificationResult(
                valid = false,
                message = "Could not detect provider from key prefix.",
            )

        val request = when (provider) {
            "gemini" -> Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash".toHttpUrl())
                .header("x-goog-api-key", apiKey.trim())
                .get()
                .build()

            "openai" -> Request.Builder()
                .url("https://api.openai.com/v1/models".toHttpUrl())
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .get()
                .build()

            "grok" -> Request.Builder()
                .url("https://api.x.ai/v1/models".toHttpUrl())
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .get()
                .build()
            "groq" -> Request.Builder()
                .url("https://api.groq.com/openai/v1/models".toHttpUrl())
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .get()
                .build()

            else -> null
        } ?: return@withContext LlmApiKeyVerificationResult(
            valid = false,
            message = "Unsupported provider.",
        )

        return@withContext runCatching {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    LlmApiKeyVerificationResult(
                        valid = true,
                        message = "API ping successful (${response.code}).",
                    )
                } else {
                    val body = response.body?.string().orEmpty()
                    LlmApiKeyVerificationResult(
                        valid = false,
                        message = buildHttpErrorMessage(
                            provider = provider,
                            statusCode = response.code,
                            body = body,
                        ),
                    )
                }
            }
        }.getOrElse {
            LlmApiKeyVerificationResult(
                valid = false,
                message = "API ping failed: ${it.message.orEmpty()}",
            )
        }
    }

    private fun buildHttpErrorMessage(
        provider: String,
        statusCode: Int,
        body: String,
    ): String {
        val providerLabel = when (provider) {
            "gemini" -> "Gemini"
            "openai" -> "OpenAI"
            "grok" -> "Grok"
            "groq" -> "Groq"
            else -> "LLM"
        }
        val apiMessage = extractApiErrorMessage(body)
        return when (statusCode) {
            401 -> "$providerLabel ping failed (401 Unauthorized). Check API key format/value."
            403 -> "$providerLabel ping failed (403 Forbidden). Key exists but lacks permission, API access, or billing/project enablement.${apiMessage?.let { " Server says: $it" } ?: ""}"
            404 -> "$providerLabel ping failed (404). Endpoint/model access not available for this key/account."
            429 -> "$providerLabel ping failed (429). Rate limit or quota exceeded."
            else -> "$providerLabel ping failed with HTTP $statusCode.${apiMessage?.let { " Server says: $it" } ?: ""}"
        }
    }

    private fun extractApiErrorMessage(body: String): String? {
        if (body.isBlank()) return null
        return runCatching {
            val json = JSONObject(body)
            json.optJSONObject("error")?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
