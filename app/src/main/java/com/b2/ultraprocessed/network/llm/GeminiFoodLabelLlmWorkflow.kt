package com.b2.ultraprocessed.network.llm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.b2.ultraprocessed.analysis.AnalysisTelemetry
import com.b2.ultraprocessed.classify.IngredientAssessment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GeminiFoodLabelLlmWorkflow(
    context: Context,
    private val apiKeyProvider: LlmApiKeyProvider,
    private val client: OkHttpClient = GeminiHttpClientFactory.create(),
) : FoodLabelLlmWorkflow {
    private val appContext = context.applicationContext

    override suspend fun extractIngredients(
        imagePath: String,
        modelId: String,
        onStatus: (String) -> Unit,
    ): Result<IngredientExtraction> = withContext(Dispatchers.IO) {
        runCatching {
            requireSupportedModel(modelId)
            val apiKey = requireApiKey()

            val prompt = readPrompt(INGREDIENT_EXTRACTION_PROMPT)
            val encodedImage = encodeImageForModel(imagePath)
            retryContractParse(
                operationLabel = "ingredient extraction",
                onStatus = onStatus,
                buildPrompt = { attempt, previousError ->
                    prompt + buildContractRepairSuffix("ingredient extraction", attempt, previousError)
                },
                request = { repairedPrompt ->
                    executeJsonRequest(
                        requestBody = buildVisionRequestBody(repairedPrompt, encodedImage),
                        modelId = modelId,
                        apiKey = apiKey,
                        operation = "extract_ingredients",
                    )
                },
                parse = ::parseIngredientExtraction,
            )
        }
    }

    override suspend fun classifyIngredients(
        extraction: IngredientExtraction,
        modelId: String,
        onStatus: (String) -> Unit,
    ): Result<IngredientClassification> = withContext(Dispatchers.IO) {
        runCatching {
            requireSupportedModel(modelId)
            val apiKey = requireApiKey()

            val prompt = readPrompt(CLASSIFICATION_PROMPT)
            retryContractParse(
                operationLabel = "classification",
                onStatus = onStatus,
                buildPrompt = { attempt, previousError ->
                    prompt + buildContractRepairSuffix("classification", attempt, previousError)
                },
                request = { repairedPrompt ->
                    executeJsonRequest(
                        requestBody = buildTextRequestBody(repairedPrompt, extraction.toPromptJson()),
                        modelId = modelId,
                        apiKey = apiKey,
                        operation = "classify_ingredients",
                    )
                },
                parse = ::parseIngredientClassification,
            )
        }
    }

    override suspend fun detectAllergens(
        extraction: IngredientExtraction,
        modelId: String,
        onStatus: (String) -> Unit,
    ): Result<AllergenDetection> = withContext(Dispatchers.IO) {
        runCatching {
            requireSupportedModel(modelId)
            val apiKey = requireApiKey()

            val prompt = readPrompt(ALLERGEN_PROMPT)
            retryContractParse(
                operationLabel = "allergen detection",
                onStatus = onStatus,
                buildPrompt = { attempt, previousError ->
                    prompt + buildContractRepairSuffix("allergen detection", attempt, previousError)
                },
                request = { repairedPrompt ->
                    executeJsonRequest(
                        requestBody = buildTextRequestBody(repairedPrompt, extraction.toPromptJson()),
                        modelId = modelId,
                        apiKey = apiKey,
                        operation = "detect_allergens",
                    )
                },
                parse = ::parseAllergenDetection,
            )
        }
    }

    private fun requireSupportedModel(modelId: String) {
        require(modelId.startsWith("gemini-")) {
            "Selected model is not supported for direct food-label analysis yet."
        }
    }

    private fun requireApiKey(): String {
        val apiKey = apiKeyProvider.getApiKey()
        require(apiKey.isNotBlank()) {
            "Add an LLM API key in Settings to use image AI analysis."
        }
        return apiKey
    }

    private fun readPrompt(assetPath: String): String =
        appContext.assets.open(assetPath).bufferedReader().use { it.readText() }

    private fun buildVisionRequestBody(
        prompt: String,
        encodedImage: String,
    ): RequestBody {
        val imagePart = JSONObject()
            .put(
                "inline_data",
                JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", encodedImage),
            )
        val textPart = JSONObject().put("text", prompt)
        return buildGenerateContentRequest(JSONArray().put(textPart).put(imagePart))
    }

    private fun buildTextRequestBody(
        prompt: String,
        inputJson: JSONObject,
    ): RequestBody {
        val text = prompt.trim() + "\n\n## Input JSON\n" + inputJson.toString(2)
        return buildGenerateContentRequest(JSONArray().put(JSONObject().put("text", text)))
    }

    private fun buildGenerateContentRequest(parts: JSONArray): RequestBody {
        val root = JSONObject()
            .put(
                "contents",
                JSONArray().put(JSONObject().put("parts", parts)),
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.1)
                    .put("topP", 0.9)
                    .put("maxOutputTokens", 1800)
                    .put("responseMimeType", "application/json"),
            )
        return root.toString().toRequestBody(JSON_MEDIA_TYPE)
    }

    private suspend fun executeJsonRequest(
        requestBody: RequestBody,
        modelId: String,
        apiKey: String,
        operation: String,
    ): JSONObject {
        val url = "$BASE_URL/models/$modelId:generateContent".toHttpUrl().newBuilder()
            .build()
        AnalysisTelemetry.event("gemini_request_start op=$operation model=$modelId")
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", apiKey)
            .post(requestBody)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!continuation.isCancelled) {
                            continuation.resumeWithException(e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            AnalysisTelemetry.event("gemini_response op=$operation http=${it.code}")
                            runCatching {
                                parseGenerateContentResponse(it)
                            }.onSuccess { json ->
                                if (!continuation.isCancelled) {
                                    continuation.resume(json)
                                }
                            }.onFailure { error ->
                                if (!continuation.isCancelled) {
                                    continuation.resumeWithException(error)
                                }
                            }
                        }
                    }
                },
            )
        }
    }

    private fun parseGenerateContentResponse(response: Response): JSONObject {
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            val trimmed = body.replace('\n', ' ').take(220)
            throw IOException(buildHttpFailureMessage(response.code, trimmed))
                .also {
                    AnalysisTelemetry.event(
                        "gemini_http_error code=${response.code} body=$trimmed",
                    )
                }
        }
        val root = JSONObject(body)
        val candidates = root.optJSONArray("candidates")
        val content = candidates
            ?.optJSONObject(0)
            ?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text").orEmpty()
        if (text.isBlank()) {
            throw IOException("LLM food-label workflow returned no text.")
        }
        return parseResponseJson(text)
    }

    private fun encodeImageForModel(imagePath: String): String {
        val file = File(imagePath)
        require(file.isFile && file.canRead()) {
            "Image file not found or unreadable."
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        val bitmapOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, bitmapOptions)
            ?: throw IOException("Could not decode image for LLM analysis.")
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
    }

    private fun buildHttpFailureMessage(statusCode: Int, apiBody: String): String {
        return when (statusCode) {
            429 -> buildString {
                append("LLM food-label workflow failed with HTTP 429 (rate limit or quota exceeded).")
                if (apiBody.isNotBlank()) {
                    append(" Provider says: ")
                    append(apiBody)
                }
            }
            else -> "LLM food-label workflow failed with HTTP $statusCode."
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth / 2 >= MAX_IMAGE_DIMENSION || currentHeight / 2 >= MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private fun parseResponseJson(text: String): JSONObject {
        val trimmed = text.trim()
        return try {
            JSONObject(trimmed)
        } catch (e: JSONException) {
            throw IOException("LLM food-label workflow returned invalid JSON.", e)
        }
    }

    private fun parseIngredientExtraction(json: JSONObject): IngredientExtraction {
        val code = json.requiredInt("code")
        if (code != VALID_EXTRACTION_CODE && code != INVALID_EXTRACTION_CODE) {
            throw IOException("LLM ingredient extraction returned unsupported code $code.")
        }
        val warnings = json.requiredArray("warnings").toStringList()
        if (code == INVALID_EXTRACTION_CODE) {
            return IngredientExtraction(
                code = code,
                productName = json.requiredString("productName").ifBlank { "Invalid image" },
                rawText = "",
                ingredients = emptyList(),
                confidence = 0f,
                warnings = warnings.ifEmpty {
                    listOf("Invalid image. Please scan a food ingredient box or ingredient list.")
                },
            )
        }
        val rawText = json.requiredString("rawIngredientText")
        val ingredients = json.requiredArray("ingredients").toStringList()
        if (rawText.isBlank() || ingredients.isEmpty()) {
            throw IOException("LLM ingredient extraction returned no usable ingredient list.")
        }
        return IngredientExtraction(
            code = code,
            productName = json.requiredString("productName").ifBlank { "Scanned food label" },
            rawText = rawText,
            ingredients = ingredients,
            confidence = json.requiredConfidence("confidence"),
            warnings = warnings,
        )
    }

    private fun parseIngredientClassification(json: JSONObject): IngredientClassification =
        IngredientClassification(
            novaGroup = json.requiredInt("novaGroup").also {
                if (it !in 1..4) throw IOException("LLM classification returned invalid NOVA group $it.")
            },
            summary = json.requiredString("summary"),
            confidence = json.requiredConfidence("confidence"),
            problemIngredients = json.requiredArray("problemIngredients").toRiskMarkers(),
            warnings = json.requiredArray("warnings").toStringList(),
            ingredientAssessments = json.optJSONArray("ingredientAssessments").toIngredientAssessments(),
        )

    private fun parseAllergenDetection(json: JSONObject): AllergenDetection =
        AllergenDetection(
            allergens = json.requiredArray("allergens").toStringList(),
            warnings = json.requiredArray("warnings").toStringList(),
            confidence = json.requiredConfidence("confidence"),
        )

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val INGREDIENT_EXTRACTION_PROMPT = "prompts/food_label_ingredient_extraction_prompt.md"
        private const val CLASSIFICATION_PROMPT = "prompts/food_label_classification_prompt.md"
        private const val ALLERGEN_PROMPT = "prompts/food_label_allergen_prompt.md"
        private const val MAX_IMAGE_DIMENSION = 1600
        private const val JPEG_QUALITY = 86
        private const val VALID_EXTRACTION_CODE = 0
        private const val INVALID_EXTRACTION_CODE = -1
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

object GeminiHttpClientFactory {
    fun create(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
}

private fun IngredientExtraction.toPromptJson(): JSONObject =
    JSONObject()
        .put("code", code)
        .put("productName", productName)
        .put("rawIngredientText", rawText)
        .put("ingredients", JSONArray(ingredients))
        .put("extractionConfidence", confidence)
        .put("extractionWarnings", JSONArray(warnings))

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val value = optString(index).trim()
            if (value.isNotBlank()) add(value)
        }
    }
}

private fun JSONArray?.toRiskMarkers(): List<IngredientRiskMarker> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index)
                ?: throw IOException("LLM response field 'problemIngredients[$index]' must be an object.")
            val name = item.requiredString("name")
            val reason = item.requiredString("reason")
            if (name.isBlank() || reason.isBlank()) {
                throw IOException("LLM response field 'problemIngredients[$index]' is incomplete.")
            }
            add(IngredientRiskMarker(name = name, reason = reason))
        }
    }
}

private fun JSONArray?.toIngredientAssessments(): List<IngredientAssessment> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index)
                ?: throw IOException("LLM response field 'ingredientAssessments[$index]' must be an object.")
            val name = item.requiredString("name")
            val novaGroup = item.requiredInt("novaGroup")
            val reason = item.requiredString("reason")
            if (name.isBlank() || reason.isBlank()) {
                throw IOException("LLM response field 'ingredientAssessments[$index]' is incomplete.")
            }
            if (novaGroup !in 1..4) {
                throw IOException("LLM response field 'ingredientAssessments[$index].novaGroup' is invalid.")
            }
            add(
                IngredientAssessment(
                    name = name,
                    novaGroup = novaGroup,
                    reason = reason,
                ),
            )
        }
    }
}

private fun JSONObject.requiredString(name: String): String {
    if (!has(name)) throw IOException("LLM response missing required field '$name'.")
    return try {
        getString(name).trim()
    } catch (e: Exception) {
        throw IOException("LLM response field '$name' must be a string.", e)
    }
}

private fun JSONObject.requiredInt(name: String): Int {
    if (!has(name)) throw IOException("LLM response missing required field '$name'.")
    return try {
        getInt(name)
    } catch (e: Exception) {
        throw IOException("LLM response field '$name' must be an integer.", e)
    }
}

private fun JSONObject.requiredArray(name: String): JSONArray {
    if (!has(name)) throw IOException("LLM response missing required field '$name'.")
    return try {
        getJSONArray(name)
    } catch (e: Exception) {
        throw IOException("LLM response field '$name' must be an array.", e)
    }
}

private fun JSONObject.requiredConfidence(name: String): Float {
    if (!has(name)) throw IOException("LLM response missing required field '$name'.")
    val confidence = try {
        getDouble(name).toFloat()
    } catch (e: Exception) {
        throw IOException("LLM response field '$name' must be a number.", e)
    }
    if (confidence !in 0f..1f) {
        throw IOException("LLM response field '$name' must be between 0.0 and 1.0.")
    }
    return confidence
}
