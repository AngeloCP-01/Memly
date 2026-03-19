package com.example.memly.di

import android.content.Context
import androidx.room.Room
import com.example.memly.data.local.MemlyDatabase
import com.example.memly.data.local.OnboardingPreferences
import com.example.memly.data.local.dao.CollectionDao
import com.example.memly.data.local.dao.MemoryDao
import com.example.memly.data.local.dao.TagDao
import com.example.memly.util.MediaStoreManager
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
    fun provideOnboardingPreferences(@ApplicationContext context: Context): OnboardingPreferences {
        return OnboardingPreferences(context)
    }

    @Provides
    @Singleton
    fun provideMediaStoreManager(@ApplicationContext context: Context): MediaStoreManager {
        return MediaStoreManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MemlyDatabase {
        return Room.databaseBuilder(
            context,
            MemlyDatabase::class.java,
            "memly_database"
        ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideMemoryDao(database: MemlyDatabase): MemoryDao = database.memoryDao()

    @Provides
    fun provideTagDao(database: MemlyDatabase): TagDao = database.tagDao()

    @Provides
    fun provideCollectionDao(database: MemlyDatabase): CollectionDao = database.collectionDao()
}
