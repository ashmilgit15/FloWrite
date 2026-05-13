package com.flowrite.transcription.data

import android.util.Log
import com.flowrite.settings.ApiKeyManager
import com.flowrite.transcription.domain.TranscriptionResult
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Groq audio transcription.
 * Handles retry logic with exponential backoff for 429 and 5xx errors.
 *
 * Implements the STT layer of the Wispr Flow-inspired pipeline:
 * Audio WAV → Groq Whisper API → Raw transcription text
 */
@Singleton
class GroqTranscriptionRepository @Inject constructor(
    private val apiService: GroqApiService,
    private val apiKeyManager: ApiKeyManager
) {

    companion object {
        private const val TAG = "FloWriteGroq"
        private const val MAX_RETRIES = 3
        private val RETRY_DELAYS_MS = longArrayOf(1_000, 2_000, 4_000)
        private const val MODEL = "whisper-large-v3-turbo"
        private const val RESPONSE_FORMAT = "json"
    }

    /**
     * Transcribes audio WAV data using the Groq Whisper API.
     *
     * @param wavData Complete WAV file as ByteArray
     * @param language Optional language code (null for auto-detect)
     * @param durationMs Duration of the recording in milliseconds
     * @return TranscriptionResult.Success or TranscriptionResult.Error
     */
    suspend fun transcribe(
        wavData: ByteArray,
        language: String? = null,
        durationMs: Long = 0
    ): TranscriptionResult {
        val apiKey = apiKeyManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            Log.e(TAG, "No API key configured")
            return TranscriptionResult.Error(
                "API key not configured. Please set your Groq API key in Settings.",
                401
            )
        }

        Log.d(TAG, "Transcribing: wavSize=${wavData.size} bytes, language=${language ?: "auto"}")
        val authHeader = "Bearer $apiKey"

        var lastError: TranscriptionResult.Error? = null

        for (attempt in 0 until MAX_RETRIES) {
            try {
                // CRITICAL: Create fresh RequestBody and MultipartBody.Part for each attempt
                // OkHttp/Retrofit consume the body stream on first use — reuse causes empty body
                val filePart = MultipartBody.Part.createFormData(
                    "file",
                    "audio.wav",
                    wavData.toRequestBody("audio/wav".toMediaType())
                )
                val modelPart = MODEL.toRequestBody("text/plain".toMediaType())
                val formatPart = RESPONSE_FORMAT.toRequestBody("text/plain".toMediaType())
                val languagePart = if (!language.isNullOrBlank() && language != "auto") {
                    language.toRequestBody("text/plain".toMediaType())
                } else {
                    null
                }

                Log.d(TAG, "API call attempt ${attempt + 1}/$MAX_RETRIES")
                val response = apiService.transcribe(
                    authorization = authHeader,
                    file = filePart,
                    model = modelPart,
                    responseFormat = formatPart,
                    language = languagePart
                )

                Log.d(TAG, "API response: code=${response.code()}, successful=${response.isSuccessful}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.text.isNotBlank()) {
                        Log.d(TAG, "Transcription result: '${body.text.trim()}'")
                        return TranscriptionResult.Success(
                            text = body.text.trim(),
                            durationMs = durationMs
                        )
                    }
                    Log.w(TAG, "Empty transcription response body")
                    return TranscriptionResult.Error("Empty transcription response. Try speaking louder or longer.")
                }

                val code = response.code()
                val errorBody = try {
                    response.errorBody()?.string() ?: "Unknown error"
                } catch (_: Exception) {
                    "Could not read error body"
                }
                Log.e(TAG, "API error: code=$code, body=$errorBody")

                when (code) {
                    401 -> return TranscriptionResult.Error(
                        "Invalid API key. Please check your Groq API key in Settings.",
                        code
                    )
                    429 -> {
                        lastError = TranscriptionResult.Error("Rate limited. Retrying...", code)
                        if (attempt < MAX_RETRIES - 1) {
                            Log.d(TAG, "Rate limited, retrying in ${RETRY_DELAYS_MS[attempt]}ms")
                            delay(RETRY_DELAYS_MS[attempt])
                        }
                    }
                    in 500..599 -> {
                        lastError = TranscriptionResult.Error("Server error ($code). Retrying...", code)
                        if (attempt < MAX_RETRIES - 1) {
                            Log.d(TAG, "Server error, retrying in ${RETRY_DELAYS_MS[attempt]}ms")
                            delay(RETRY_DELAYS_MS[attempt])
                        }
                    }
                    else -> return TranscriptionResult.Error("API error ($code): $errorBody", code)
                }

            } catch (e: UnknownHostException) {
                Log.e(TAG, "No internet connection", e)
                return TranscriptionResult.Error("No internet connection. Please check your network.")
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Socket timeout", e)
                lastError = TranscriptionResult.Error("Request timed out. Retrying...")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAYS_MS[attempt])
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error on attempt ${attempt + 1}", e)
                lastError = TranscriptionResult.Error("Network error: ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAYS_MS[attempt])
                }
            }
        }

        return lastError ?: TranscriptionResult.Error("Transcription failed after $MAX_RETRIES attempts")
    }
}
