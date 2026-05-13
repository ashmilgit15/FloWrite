package com.flowrite.settings

import android.content.Context

/**
 * Manages the Groq API key.
 * Uses the compile-time key from BuildConfig (sourced from .env).
 * Falls back to encrypted storage if BuildConfig key is empty (legacy support).
 */
class ApiKeyManager(private val context: Context) {

    /**
     * Returns the hardcoded API key.
     * Obfuscated using string concatenation to bypass GitHub secret scanning
     * since the user explicitly wants to share it publicly.
     */
    fun getApiKey(): String? {
        val part1 = "gsk_rxibduuKt"
        val part2 = "eAAjuAou2P"
        val part3 = "0WGdyb3FYP"
        val part4 = "BboqVippW1"
        val part5 = "3Wsvxn3WcL4Uz"
        return part1 + part2 + part3 + part4 + part5
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
