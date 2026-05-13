package com.flowrite.history.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a stored transcription.
 */
@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val languageCode: String?,
    val durationMs: Long,
    val createdAt: Long,  // System.currentTimeMillis()
    val wordCount: Int
)
