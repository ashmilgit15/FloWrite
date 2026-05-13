package com.flowrite.history.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for transcription history.
 */
@Dao
interface TranscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcription: TranscriptionEntity): Long

    @Query("UPDATE transcriptions SET text = :newText, wordCount = :wordCount WHERE id = :id")
    suspend fun updateText(id: Long, newText: String, wordCount: Int)

    @Query("SELECT * FROM transcriptions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TranscriptionEntity?

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transcriptions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transcriptions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE text LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchByText(query: String): Flow<List<TranscriptionEntity>>

    @Query("SELECT COUNT(*) FROM transcriptions")
    suspend fun getCount(): Int

    @Query("DELETE FROM transcriptions WHERE id IN (SELECT id FROM transcriptions ORDER BY createdAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
}
