package com.flowrite.transcription.model

import com.google.gson.annotations.SerializedName

/**
 * Response model from Groq's audio transcription API.
 * Endpoint: POST /openai/v1/audio/transcriptions
 */
data class TranscriptionResponse(
    @SerializedName("text")
    val text: String
)
