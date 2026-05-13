package com.flowrite.transcription.domain

import android.util.Log
import com.flowrite.audio.WavEncoder
import com.flowrite.transcription.data.GroqTranscriptionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case that orchestrates the full Wispr Flow-inspired transcription pipeline:
 *
 * 1. PCM audio → WAV encoding
 * 2. WAV → Groq Whisper API → Raw transcription
 * 3. Raw transcription → Semantic post-processing (filler removal, auto-capitalize, punctuation)
 * 4. Final cleaned text → Result
 *
 * The semantic editing layer is inspired by Wispr Flow's approach:
 * - Remove filler words ("um", "uh", "like", "you know")
 * - Handle self-corrections ("I mean", "actually", "sorry", "wait")
 * - Auto-capitalize sentences
 * - Ensure proper punctuation
 * - Clean up repeated words/stutters
 */
@Singleton
class TranscribeUseCase @Inject constructor(
    private val repository: GroqTranscriptionRepository
) {

    companion object {
        private const val TAG = "FloWriteTranscribe"

        // Filler words to remove (Wispr Flow-style semantic cleanup)
        private val FILLER_PATTERNS = listOf(
            // Common English fillers
            "\\b(um|umm|uh|uhh|er|err|ah|ahh|hmm|hm)\\b",
            // Hedge words when used as fillers
            "\\b(you know|i mean|like)\\b(?=\\s*,?\\s*(?:um|uh|er|ah|hmm|so|and|but))",
            // Repeated "so" at start
            "^(so\\s+)+(?=\\w)",
        )

        // Self-correction patterns (Wispr Flow handles "actually, make that...")
        private val CORRECTION_PATTERNS = listOf(
            // "actually" / "I mean" / "sorry" / "wait" followed by correction
            ".*?\\b(?:actually|i mean|sorry|wait|no)\\s*,?\\s*",
        )
    }

    /**
     * Transcribes raw PCM audio data with full post-processing pipeline.
     *
     * @param pcmData Raw PCM audio bytes (16-bit, 16kHz, mono)
     * @param language Optional language code (null for auto-detect)
     * @param autoPunctuate Whether to apply auto-punctuation post-processing
     * @return TranscriptionResult with processed text
     */
    suspend fun execute(
        pcmData: ByteArray,
        language: String? = null,
        autoPunctuate: Boolean = true
    ): TranscriptionResult {
        Log.d(TAG, "Starting transcription pipeline: ${pcmData.size} bytes")

        // Calculate duration from PCM data (16-bit mono at 16kHz = 32000 bytes/sec)
        val durationMs = (pcmData.size.toLong() * 1000L) / (16_000 * 2)
        Log.d(TAG, "Audio duration: ${durationMs}ms")

        // Step 1: Encode PCM to WAV
        val wavData = WavEncoder.encode(pcmData)
        Log.d(TAG, "WAV encoded: ${wavData.size} bytes")

        // Step 2: Send to Groq API
        val result = repository.transcribe(
            wavData = wavData,
            language = language,
            durationMs = durationMs
        )

        // Step 3: Post-process successful transcription (Wispr Flow-style semantic editing)
        return when (result) {
            is TranscriptionResult.Success -> {
                Log.d(TAG, "Raw transcription: '${result.text}'")
                val processedText = semanticEdit(result.text, autoPunctuate)
                Log.d(TAG, "Processed transcription: '$processedText'")

                if (processedText.isBlank()) {
                    TranscriptionResult.Error("No speech detected. Try speaking louder.")
                } else {
                    result.copy(text = processedText)
                }
            }
            is TranscriptionResult.Error -> {
                Log.e(TAG, "Transcription error: ${result.message}")
                result
            }
        }
    }

    /**
     * Wispr Flow-style semantic editing pipeline:
     * 1. Remove filler words
     * 2. Handle self-corrections (keep only the corrected version)
     * 3. Clean up stutters/repeated words
     * 4. Auto-capitalize sentences
     * 5. Ensure proper punctuation
     * 6. Normalize whitespace
     */
    private fun semanticEdit(text: String, autoPunctuate: Boolean): String {
        var processed = text.trim()

        if (processed.isEmpty()) return processed

        // Step 1: Remove filler words
        processed = removeFillerWords(processed)

        // Step 2: Handle self-corrections
        processed = handleSelfCorrections(processed)

        // Step 3: Clean up stutters and repeated words
        processed = cleanStutters(processed)

        // Step 4: Normalize whitespace and punctuation
        processed = normalizeWhitespace(processed)

        // Step 5: Auto-capitalize sentences
        processed = autoCapitalize(processed)

        // Step 6: Ensure trailing punctuation
        if (autoPunctuate) {
            processed = ensureTrailingPunctuation(processed)
        }

        return processed.trim()
    }

    /**
     * Removes common filler words: um, uh, er, hmm, etc.
     */
    private fun removeFillerWords(text: String): String {
        var result = text

        // Remove standalone fillers (case-insensitive)
        val fillerRegex = Regex(
            "\\b(um|umm|uh|uhh|er|err|ah|ahh|hmm|hm|erm)\\b\\s*,?\\s*",
            RegexOption.IGNORE_CASE
        )
        result = fillerRegex.replace(result, " ")

        return result
    }

    /**
     * Handles self-corrections — when someone says "actually X" or "I mean Y",
     * tries to keep only the corrected version (the part after the correction marker).
     */
    private fun handleSelfCorrections(text: String): String {
        // Look for correction markers followed by the actual intended text
        val correctionRegex = Regex(
            "(.+?)\\s*\\b(?:actually|I mean|sorry|wait no|no wait)\\s*,?\\s*(.+)",
            RegexOption.IGNORE_CASE
        )

        val match = correctionRegex.find(text)
        if (match != null) {
            val correction = match.groupValues[2].trim()
            if (correction.length > 3) {
                // The correction is substantive enough — use it
                return correction
            }
        }

        return text
    }

    /**
     * Cleans up stutters and repeated words (e.g., "the the" → "the").
     */
    private fun cleanStutters(text: String): String {
        // Remove immediate word repetitions
        val stutterRegex = Regex("\\b(\\w+)\\s+\\1\\b", RegexOption.IGNORE_CASE)
        return stutterRegex.replace(text, "$1")
    }

    /**
     * Normalizes whitespace: collapses multiple spaces, fixes space around punctuation.
     */
    private fun normalizeWhitespace(text: String): String {
        var result = text

        // Collapse multiple spaces
        result = result.replace(Regex("\\s{2,}"), " ")

        // Remove space before punctuation
        result = result.replace(Regex("\\s+([.,!?;:])"), "$1")

        // Ensure space after punctuation (except at end)
        result = result.replace(Regex("([.,!?;:])(?=\\w)"), "$1 ")

        return result.trim()
    }

    /**
     * Auto-capitalizes the first letter and letters after sentence-ending punctuation.
     */
    private fun autoCapitalize(text: String): String {
        if (text.isEmpty()) return text

        val result = StringBuilder()
        var capitalizeNext = true

        for (char in text) {
            if (capitalizeNext && char.isLetter()) {
                result.append(char.uppercase())
                capitalizeNext = false
            } else {
                result.append(char)
                if (char in listOf('.', '!', '?')) {
                    capitalizeNext = true
                }
            }
        }

        return result.toString()
    }

    /**
     * Ensures the text ends with proper punctuation.
     */
    private fun ensureTrailingPunctuation(text: String): String {
        if (text.isEmpty()) return text

        val trimmed = text.trimEnd()
        val lastChar = trimmed.last()

        // Already has terminal punctuation
        if (lastChar in listOf('.', '!', '?', '…')) return trimmed

        // Ends with comma or semicolon — replace with period
        if (lastChar in listOf(',', ';')) {
            return trimmed.dropLast(1) + "."
        }

        // Ends with letter or digit — add period
        if (lastChar.isLetterOrDigit()) {
            return "$trimmed."
        }

        return trimmed
    }
}
