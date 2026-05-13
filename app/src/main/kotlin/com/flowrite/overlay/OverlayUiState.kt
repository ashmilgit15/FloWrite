package com.flowrite.overlay

/**
 * Sealed class representing the UI state of the floating overlay.
 */
sealed class OverlayUiState {
    /** Idle state — mic button shown, ready to record */
    data object Idle : OverlayUiState()

    /** Recording state — mic is active, shows pulse animation */
    data class Recording(val amplitudeDb: Float = 0f) : OverlayUiState()

    /** Processing state — audio sent to Groq, waiting for response */
    data object Processing : OverlayUiState()

    /** Result state — transcription received, show result card */
    data class Result(val text: String) : OverlayUiState()

    /** Error state — something went wrong */
    data class Error(val message: String) : OverlayUiState()
}
