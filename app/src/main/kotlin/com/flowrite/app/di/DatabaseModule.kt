package com.flowrite.app.di

import android.content.Context
import androidx.room.Room
import com.flowrite.history.data.TranscriptionDao
import com.flowrite.history.data.TranscriptionDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTranscriptionDatabase(
        @ApplicationContext context: Context
    ): TranscriptionDatabase {
        return Room.databaseBuilder(
            context,
            TranscriptionDatabase::class.java,
            "flowrite_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTranscriptionDao(database: TranscriptionDatabase): TranscriptionDao {
        return database.transcriptionDao()
    }
}
