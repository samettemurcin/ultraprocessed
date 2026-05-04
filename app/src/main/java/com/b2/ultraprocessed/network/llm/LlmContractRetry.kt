package com.b2.ultraprocessed.network.llm

import java.io.IOException
import org.json.JSONObject
import kotlinx.coroutines.delay

internal const val LLM_CONTRACT_RETRY_ATTEMPTS: Int = 3

internal suspend fun <T> retryContractParse(
    operationLabel: String,
    onStatus: (String) -> Unit,
    buildPrompt: (attempt: Int, previousError: String?) -> String,
    request: suspend (String) -> JSONObject,
    parse: (JSONObject) -> T,
): T {
    var previousError: String? = null
    repeat(LLM_CONTRACT_RETRY_ATTEMPTS) { index ->
        val attempt = index + 1
        if (attempt > 1) {
            val waitMillis = retryBackoffMillis(attempt)
            onStatus(
                "$operationLabel format retry $attempt/$LLM_CONTRACT_RETRY_ATTEMPTS · waiting ${formatRetryDelay(waitMillis)}",
            )
            delay(waitMillis)
        }
        val response = request(buildPrompt(attempt, previousError))
        try {
            return parse(response)
        } catch (error: IOException) {
            if (!error.isContractViolation() || attempt == LLM_CONTRACT_RETRY_ATTEMPTS) {
                if (error.isContractViolation()) {
                    throw IOException(
                        "$operationLabel could not be validated after $LLM_CONTRACT_RETRY_ATTEMPTS attempts.",
                        error,
                    )
                }
                throw error
            }
            previousError = error.message.orEmpty()
            val nextAttempt = attempt + 1
            if (nextAttempt <= LLM_CONTRACT_RETRY_ATTEMPTS) {
                onStatus(
                    "$operationLabel repair attempt $attempt/$LLM_CONTRACT_RETRY_ATTEMPTS · retrying in ${formatRetryDelay(retryBackoffMillis(nextAttempt))}",
                )
            }
        }
    }
    throw IOException("$operationLabel failed after contract retries.")
}

internal fun buildContractRepairSuffix(
    operationLabel: String,
    attempt: Int,
    previousError: String?,
): String {
    if (attempt <= 1) return ""
    val reason = previousError
        ?.takeIf { it.isNotBlank() }
        ?: "the previous output did not match the required JSON schema"
    return """

## Contract Repair

Previous output failed validation because: $reason

Retry #$attempt of $LLM_CONTRACT_RETRY_ATTEMPTS for $operationLabel.
Return only a single valid JSON object.
Do not use markdown.
Do not add commentary.
Follow the schema exactly.
If a field is unknown, use the empty array, an empty string, or 0.0 as required by the schema.
""".trimIndent()
}

private fun IOException.isContractViolation(): Boolean {
    val message = message.orEmpty()
    return message.contains("LLM response missing required field") ||
        message.contains("LLM response field") ||
        message.contains("invalid JSON") ||
        message.contains("no usable ingredient list") ||
        message.contains("unsupported code") ||
        message.contains("incomplete") ||
        message.contains("invalid NOVA group")
}

private fun retryBackoffMillis(attempt: Int): Long =
    when (attempt) {
        1 -> 0L
        2 -> 1200L
        3 -> 2400L
        else -> 3600L
    }

private fun formatRetryDelay(millis: Long): String =
    if (millis >= 1000L) {
        val seconds = millis / 1000.0
        String.format("%.1fs", seconds)
    } else {
        "${millis}ms"
    }
