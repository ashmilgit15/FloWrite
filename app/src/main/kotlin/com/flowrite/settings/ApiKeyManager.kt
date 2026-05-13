package com.flowrite.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages the Groq API key using EncryptedSharedPreferences.
 * The key is encrypted with AES-256-GCM using Android Keystore-backed MasterKey.
 */
class ApiKeyManager(private val context: Context) {

    companion object {
        private const val PREFS_FILE = "flowrite_secure_prefs"
        private const val KEY_API_KEY = "groq_api_key"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Saves the API key securely.
     */
    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    /**
     * Retrieves the API key, or null if not set.
     */
    fun getApiKey(): String? {
        return encryptedPrefs.getString(KEY_API_KEY, null)
    }

    /**
     * Returns true if an API key has been configured.
     */
    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    /**
     * Clears the stored API key.
     */
    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_API_KEY).apply()
    }
}
