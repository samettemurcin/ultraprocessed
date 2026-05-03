package com.b2.ultraprocessed.network.usda

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

data class UsdaApiKeyVerificationResult(
    val valid: Boolean,
    val message: String,
)

class UsdaApiKeyVerifier {
    suspend fun verify(apiKey: String): UsdaApiKeyVerificationResult = withContext(Dispatchers.IO) {
        val normalizedKey = apiKey.trim()
        if (normalizedKey.isBlank()) {
            return@withContext UsdaApiKeyVerificationResult(
                valid = false,
                message = "USDA API key cannot be empty.",
            )
        }

        val url = "${UsdaApiService.BASE_URL}/food/$KNOWN_FOOD_ID".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", normalizedKey)
            .build()
        val request = Request.Builder().url(url).get().build()

        return@withContext runCatching {
            UsdaHttpClientFactory.create().newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> UsdaApiKeyVerificationResult(
                        valid = true,
                        message = "USDA key verified and saved securely.",
                    )

                    response.code == 401 || response.code == 403 -> UsdaApiKeyVerificationResult(
                        valid = false,
                        message = "USDA key rejected. Check the key and try again.",
                    )

                    else -> UsdaApiKeyVerificationResult(
                        valid = false,
                        message = "USDA verification failed (HTTP ${response.code}).",
                    )
                }
            }
        }.getOrElse {
            UsdaApiKeyVerificationResult(
                valid = false,
                message = "Could not reach USDA for key verification.",
            )
        }
    }

    private companion object {
        const val KNOWN_FOOD_ID = 356425L
    }
}
