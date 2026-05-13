package com.flowrite.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Manages audio capture using Android's AudioRecord API.
 * Implements Voice Activity Detection (VAD) using RMS amplitude analysis.
 * Emits audio capture state via Flow for reactive collection.
 *
 * Inspired by Wispr Flow: captures audio, performs VAD-based auto-stop,
 * and delivers clean PCM data for cloud-based transcription.
 */
@Singleton
class AudioCaptureManager @Inject constructor() {

    companion object {
        private const val TAG = "FloWriteAudio"
        const val SAMPLE_RATE = 16_000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_MULTIPLIER = 4
        const val MAX_DURATION_MS = 120_000L
        const val DEFAULT_VAD_SILENCE_MS = 1_500L
        const val DEFAULT_VAD_RMS_THRESHOLD = 500
        const val CHUNK_SIZE_MS = 20 // 20ms chunks for VAD
    }

    var vadSilenceMs: Long = DEFAULT_VAD_SILENCE_MS
    var vadRmsThreshold: Int = DEFAULT_VAD_RMS_THRESHOLD

    /** Set to true to signal recording should stop gracefully */
    @Volatile
    var stopRequested: Boolean = false

    private val minBufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
    )

    /**
     * Starts recording and emits AudioCaptureState via Flow.
     * The flow handles VAD (Voice Activity Detection) and auto-stops on silence.
     * Also supports manual stop via the stopRequested flag.
     * PCM data is accumulated in memory and emitted as Stopped state when done.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(): Flow<AudioCaptureState> = flow {
        stopRequested = false

        val bufferSize = if (minBufferSize > 0) minBufferSize * BUFFER_MULTIPLIER else 8192
        val chunkSizeInBytes = (SAMPLE_RATE * 2 * CHUNK_SIZE_MS) / 1000 // 16-bit = 2 bytes per sample
        val pcmOutputStream = ByteArrayOutputStream()

        Log.d(TAG, "Initializing AudioRecord: sampleRate=$SAMPLE_RATE, bufferSize=$bufferSize, minBuffer=$minBufferSize")

        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AudioRecord", e)
            emit(AudioCaptureState.Error("Failed to create AudioRecord: ${e.message}"))
            return@flow
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized, state=${audioRecord.state}")
            emit(AudioCaptureState.Error("Failed to initialize AudioRecord. Microphone may be in use by another app."))
            audioRecord.release()
            return@flow
        }

        try {
            audioRecord.startRecording()
            Log.d(TAG, "Recording started")
            emit(AudioCaptureState.Recording(0f))

            val readBuffer = ShortArray(chunkSizeInBytes / 2)
            var silenceDurationMs = 0L
            var totalDurationMs = 0L
            var hasDetectedVoice = false

            while (currentCoroutineContext().isActive &&
                totalDurationMs < MAX_DURATION_MS &&
                !stopRequested
            ) {
                val shortsRead = audioRecord.read(readBuffer, 0, readBuffer.size)

                if (shortsRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "AudioRecord ERROR_INVALID_OPERATION")
                    emit(AudioCaptureState.Error("AudioRecord error: invalid operation"))
                    return@flow
                }
                if (shortsRead == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "AudioRecord ERROR_BAD_VALUE")
                    emit(AudioCaptureState.Error("AudioRecord error: bad value"))
                    return@flow
                }
                if (shortsRead == AudioRecord.ERROR) {
                    Log.e(TAG, "AudioRecord ERROR")
                    emit(AudioCaptureState.Error("AudioRecord error"))
                    return@flow
                }

                if (shortsRead > 0) {
                    // Convert shorts to little-endian bytes and append to output
                    val byteBuffer = ByteArray(shortsRead * 2)
                    for (i in 0 until shortsRead) {
                        byteBuffer[i * 2] = (readBuffer[i].toInt() and 0xFF).toByte()
                        byteBuffer[i * 2 + 1] = (readBuffer[i].toInt() shr 8 and 0xFF).toByte()
                    }
                    pcmOutputStream.write(byteBuffer)

                    // Compute RMS for VAD
                    val rms = computeRms(readBuffer, shortsRead)
                    val amplitudeDb = if (rms > 0) (20 * log10(rms)).toFloat() else -60f

                    if (rms > vadRmsThreshold) {
                        hasDetectedVoice = true
                        silenceDurationMs = 0
                        emit(AudioCaptureState.Recording(amplitudeDb))
                    } else {
                        silenceDurationMs += CHUNK_SIZE_MS
                        if (hasDetectedVoice && silenceDurationMs >= vadSilenceMs) {
                            Log.d(TAG, "VAD: silence threshold reached after ${totalDurationMs}ms total")
                            emit(AudioCaptureState.SilenceDetected)
                            break
                        }
                        emit(AudioCaptureState.Recording(amplitudeDb))
                    }

                    totalDurationMs += CHUNK_SIZE_MS
                }
            }

            if (stopRequested) {
                Log.d(TAG, "Recording stopped by user request after ${totalDurationMs}ms")
            }

            val pcmData = pcmOutputStream.toByteArray()
            Log.d(TAG, "Recording complete: ${pcmData.size} bytes, ${totalDurationMs}ms, voiceDetected=$hasDetectedVoice")

            if (pcmData.isNotEmpty()) {
                emit(AudioCaptureState.Stopped(pcmData))
            } else {
                emit(AudioCaptureState.Error("No audio data recorded"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recording exception", e)
            // Try to emit whatever PCM data we have
            val pcmData = pcmOutputStream.toByteArray()
            if (pcmData.isNotEmpty()) {
                emit(AudioCaptureState.Stopped(pcmData))
            } else {
                emit(AudioCaptureState.Error("Recording error: ${e.message}"))
            }
        } finally {
            stopRequested = false
            try {
                if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop()
                }
            } catch (_: IllegalStateException) {
                // Already stopped
            }
            audioRecord.release()
            Log.d(TAG, "AudioRecord released")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Computes Root Mean Square (RMS) of audio samples for VAD.
     */
    private fun computeRms(samples: ShortArray, count: Int): Double {
        if (count == 0) return 0.0
        var sumOfSquares = 0.0
        for (i in 0 until count) {
            val sample = samples[i].toDouble()
            sumOfSquares += sample * sample
        }
        return sqrt(sumOfSquares / count)
    }
}
