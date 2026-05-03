package com.b2.ultraprocessed.network.usda

import com.b2.ultraprocessed.storage.secrets.SecretKeyManager

fun interface UsdaApiKeyProvider {
    fun getApiKey(): String
}

class SecretUsdaApiKeyProvider(
    private val secretKeyManager: SecretKeyManager,
) : UsdaApiKeyProvider {
    override fun getApiKey(): String =
        secretKeyManager.getApiKey(SecretKeyManager.USDA_API_KEY).orEmpty().trim()
}
