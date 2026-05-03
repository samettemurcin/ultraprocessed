package com.b2.ultraprocessed.storage.secrets

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecretKeyManager(context: Context) {
    private val appContext = context.applicationContext

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "nova_secrets",
        masterKeyAlias,
        appContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveApiKey(keyName: String, apiKey: String): Boolean {
        val normalized = apiKey.trim()
        if (normalized.isBlank()) {
            return deleteApiKey(keyName)
        }
        return encryptedPrefs.edit().putString(keyName, normalized).commit()
    }

    fun getApiKey(keyName: String): String? {
        return encryptedPrefs.getString(keyName, null)
    }

    fun deleteApiKey(keyName: String): Boolean {
        return encryptedPrefs.edit().remove(keyName).commit()
    }

    fun hasApiKey(keyName: String): Boolean {
        return encryptedPrefs.contains(keyName)
    }

    companion object {
        const val LLM_API_KEY: String = "llm_api_key"
        const val USDA_API_KEY: String = "usda_api_key"
    }
}
