package com.flowrite.history.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for FloWrite.
 * Stores transcription history.
 */
@Database(
    entities = [TranscriptionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class TranscriptionDatabase : RoomDatabase() {
    abstract fun transcriptionDao(): TranscriptionDao
}
