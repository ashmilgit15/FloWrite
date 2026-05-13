package com.flowrite.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility object for encoding raw PCM data into WAV format.
 * Produces a valid WAV file suitable for Groq Whisper API consumption.
 */
object WavEncoder {

    /**
     * Encodes raw PCM 16-bit mono audio data into WAV format.
     *
     * @param pcm Raw PCM audio data (16-bit signed, little-endian, mono)
     * @param sampleRate Sample rate in Hz (default: 16000 for Whisper)
     * @param channels Number of audio channels (default: 1 for mono)
     * @param bitsPerSample Bits per sample (default: 16)
     * @return Complete WAV file as ByteArray including 44-byte header
     */
    fun encode(
        pcm: ByteArray,
        sampleRate: Int = 16_000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val totalDataLen = pcm.size + 36 // 44 - 8 (RIFF header)

        return ByteArrayOutputStream(pcm.size + 44).apply {
            // RIFF header
            write("RIFF".toByteArray(Charsets.US_ASCII))
            write(intToLittleEndianBytes(totalDataLen))
            write("WAVE".toByteArray(Charsets.US_ASCII))

            // fmt sub-chunk
            write("fmt ".toByteArray(Charsets.US_ASCII))
            write(intToLittleEndianBytes(16))               // Sub-chunk size (PCM = 16)
            write(shortToLittleEndianBytes(1))               // Audio format (PCM = 1)
            write(shortToLittleEndianBytes(channels.toShort()))
            write(intToLittleEndianBytes(sampleRate))
            write(intToLittleEndianBytes(byteRate))
            write(shortToLittleEndianBytes(blockAlign.toShort()))
            write(shortToLittleEndianBytes(bitsPerSample.toShort()))

            // data sub-chunk
            write("data".toByteArray(Charsets.US_ASCII))
            write(intToLittleEndianBytes(pcm.size))
            write(pcm)
        }.toByteArray()
    }

    private fun intToLittleEndianBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToLittleEndianBytes(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
}
