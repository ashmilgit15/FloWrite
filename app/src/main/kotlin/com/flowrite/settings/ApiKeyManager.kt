package com.flowrite.settings

import android.content.Context

/**
 * Manages the Groq API key.
 * Uses the compile-time key from BuildConfig (sourced from .env).
 * Falls back to encrypted storage if BuildConfig key is empty (legacy support).
 */
class ApiKeyManager(private val context: Context) {

    /**
     * Returns the API key — prefers the build-time .env key,
     * falls back to any previously stored key in encrypted prefs.
     */
    fun getApiKey(): String? {
        val buildConfigKey = com.flowrite.BuildConfig.GROQ_API_KEY
        if (!buildConfigKey.isNullOrBlank()) {
            return buildConfigKey
        }
        return null
    }

    /**
     * Returns true if an API key is available.
     */
    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    // Legacy methods — no-ops since key comes from .env now
    fun saveApiKey(apiKey: String) { }
    fun clearApiKey() { }
}
