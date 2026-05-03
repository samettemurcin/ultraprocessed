package com.b2.ultraprocessed.network.usda

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class UsdaApiService(
    private val apiKeyProvider: UsdaApiKeyProvider,
    private val client: OkHttpClient = UsdaHttpClientFactory.create(),
) : UsdaApiDataSource {
    override suspend fun searchFoods(
        query: String,
        pageSize: Int,
    ): List<UsdaSearchFood> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isBlank()) return@withContext emptyList()

        // POST is the documented path; `dataType` must be a JSON array (GET array query encoding was unreliable).
        val url = "$BASE_URL/foods/search".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .build()
        val json = JSONObject().apply {
            put("query", query)
            put("pageSize", pageSize.coerceIn(1, 200))
            put("dataType", JSONArray().put("Branded"))
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        val responseBody = executeForBody(request) ?: return@withContext emptyList()
        UsdaJsonParser.parseSearchFoods(responseBody)
    }

    override suspend fun fetchFoodDetail(fdcId: Long): UsdaFoodDetail? = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isBlank()) return@withContext null

        val url = "$BASE_URL/food/$fdcId".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .build()
        val request = Request.Builder().url(url).get().build()
        val body = executeForBody(request) ?: return@withContext null
        UsdaJsonParser.parseFoodDetail(body)
    }

    private suspend fun executeForBody(request: Request): String? {
        var lastFailure: IOException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return response.body?.string()
                    }
                    if (!response.code.isRetryableStatus() || attempt == MAX_ATTEMPTS - 1) {
                        return null
                    }
                }
            } catch (e: IOException) {
                lastFailure = e
                if (attempt == MAX_ATTEMPTS - 1) return null
            }
            delay(RETRY_BACKOFF_MILLIS * (attempt + 1))
        }
        return lastFailure?.let { null }
    }

    companion object {
        const val BASE_URL: String = "https://api.nal.usda.gov/fdc/v1"
        private const val MAX_ATTEMPTS = 2
        private const val RETRY_BACKOFF_MILLIS = 250L
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

object UsdaHttpClientFactory {
    fun create(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private const val CONNECT_TIMEOUT_SECONDS = 3L
    private const val READ_TIMEOUT_SECONDS = 5L
    private const val WRITE_TIMEOUT_SECONDS = 5L
    private const val CALL_TIMEOUT_SECONDS = 8L
}

private fun Int.isRetryableStatus(): Boolean =
    this == 408 || this == 429 || this in 500..599
