package com.flowrite.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val apiKeyManager: ApiKeyManager
) : ViewModel() {

    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_VAD_SILENCE_MS = intPreferencesKey("vad_silence_ms")
        val KEY_VAD_THRESHOLD = intPreferencesKey("vad_threshold")
        val KEY_AUTO_PUNCTUATE = booleanPreferencesKey("auto_punctuate")
        val KEY_DARK_THEME = stringPreferencesKey("theme") // "dark", "light", "system"
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    // API Key state
    private val _hasApiKey = MutableStateFlow(apiKeyManager.hasApiKey())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    private val _apiKeyInput = MutableStateFlow("")
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    // Settings from DataStore
    val language: StateFlow<String> = dataStore.data
        .map { it[KEY_LANGUAGE] ?: "auto" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "auto")

    val vadSilenceMs: StateFlow<Int> = dataStore.data
        .map { it[KEY_VAD_SILENCE_MS] ?: 1500 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1500)

    val vadThreshold: StateFlow<Int> = dataStore.data
        .map { it[KEY_VAD_THRESHOLD] ?: 500 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 500)

    val autoPunctuate: StateFlow<Boolean> = dataStore.data
        .map { it[KEY_AUTO_PUNCTUATE] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val theme: StateFlow<String> = dataStore.data
        .map { it[KEY_DARK_THEME] ?: "dark" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark")

    val onboardingComplete: StateFlow<Boolean> = dataStore.data
        .map { it[KEY_ONBOARDING_COMPLETE] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updateApiKeyInput(input: String) {
        _apiKeyInput.value = input
    }

    fun saveApiKey(key: String) {
        apiKeyManager.saveApiKey(key)
        _hasApiKey.value = true
        _apiKeyInput.value = ""
    }

    fun clearApiKey() {
        apiKeyManager.clearApiKey()
        _hasApiKey.value = false
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_LANGUAGE] = language }
        }
    }

    fun setVadSilenceMs(ms: Int) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_VAD_SILENCE_MS] = ms }
        }
    }

    fun setVadThreshold(threshold: Int) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_VAD_THRESHOLD] = threshold }
        }
    }

    fun setAutoPunctuate(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_AUTO_PUNCTUATE] = enabled }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_DARK_THEME] = theme }
        }
    }

    fun setOnboardingComplete() {
        viewModelScope.launch {
            dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = true }
        }
    }
}
