package com.b2.ultraprocessed.network.llm

import com.b2.ultraprocessed.storage.secrets.SecretKeyManager

fun interface LlmApiKeyProvider {
    fun getApiKey(): String
}

class SecretLlmApiKeyProvider(
    private val secretKeyManager: SecretKeyManager,
) : LlmApiKeyProvider {
    override fun getApiKey(): String =
        secretKeyManager.getApiKey(SecretKeyManager.LLM_API_KEY).orEmpty().trim()
}

