package com.flowrite.audio

/**
 * Sealed class representing audio capture states.
 */
sealed class AudioCaptureState {
    /** Idle — not recording */
    data object Idle : AudioCaptureState()

    /** Currently recording — provides amplitude for visualization */
    data class Recording(val amplitudeDb: Float) : AudioCaptureState()

    /** Silence detected — will auto-stop soon */
    data object SilenceDetected : AudioCaptureState()

    /** Recording stopped — PCM data available */
    data class Stopped(val pcmData: ByteArray) : AudioCaptureState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Stopped) return false
            return pcmData.contentEquals(other.pcmData)
        }

        override fun hashCode(): Int = pcmData.contentHashCode()
    }

    /** Error during recording */
    data class Error(val message: String) : AudioCaptureState()
}
