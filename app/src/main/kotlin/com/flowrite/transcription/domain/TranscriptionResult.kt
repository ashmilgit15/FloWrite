package com.flowrite.transcription.domain

/**
 * Sealed class representing the result of a transcription operation.
 */
sealed class TranscriptionResult {
    /**
     * Successful transcription.
     * @param text The transcribed text
     * @param durationMs Duration of the audio in milliseconds
     */
    data class Success(
        val text: String,
        val durationMs: Long
    ) : TranscriptionResult()

    /**
     * Transcription failed.
     * @param message Human-readable error message
     * @param code HTTP status code, if applicable
     */
    data class Error(
        val message: String,
        val code: Int? = null
    ) : TranscriptionResult()
}
