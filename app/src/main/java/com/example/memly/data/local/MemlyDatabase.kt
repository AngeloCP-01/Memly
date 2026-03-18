package com.example.memly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.memly.data.local.dao.CollectionDao
import com.example.memly.data.local.dao.MemoryDao
import com.example.memly.data.local.dao.TagDao
import com.example.memly.data.local.entity.CollectionEntity
import com.example.memly.data.local.entity.MediaFileEntity
import com.example.memly.data.local.entity.MemoryCollectionCrossRef
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.MemoryTagCrossRef
import com.example.memly.data.local.entity.TagEntity

@Database(
    entities = [
        MemoryEntity::class,
        MediaFileEntity::class,
        TagEntity::class,
        CollectionEntity::class,
        MemoryTagCrossRef::class,
        MemoryCollectionCrossRef::class
    ],
    version = 3,
    exportSchema = true
)
abstract class MemlyDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun tagDao(): TagDao
    abstract fun collectionDao(): CollectionDao
}
