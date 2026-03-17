package com.example.memly.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.memly.data.local.entity.CollectionEntity
import com.example.memly.data.local.entity.MemoryCollectionCrossRef
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.MemoryWithDetails
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Insert
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)

    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :collectionId")
    suspend fun getCollectionById(collectionId: Long): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMemoryCollectionCrossRef(crossRef: MemoryCollectionCrossRef)

    @Delete
    suspend fun deleteMemoryCollectionCrossRef(crossRef: MemoryCollectionCrossRef)

    @Transaction
    @Query("SELECT m.* FROM memories m INNER JOIN memory_collection_cross_ref mc ON m.id = mc.memoryId WHERE mc.collectionId = :collectionId ORDER BY m.memoryDate DESC")
    fun getMemoriesInCollection(collectionId: Long): Flow<List<MemoryEntity>>

    @Query("SELECT COUNT(*) FROM memory_collection_cross_ref WHERE collectionId = :collectionId")
    fun getMemoryCountInCollection(collectionId: Long): Flow<Int>

    @Query("SELECT collectionId FROM memory_collection_cross_ref WHERE memoryId = :memoryId")
    fun getCollectionIdsForMemory(memoryId: Long): Flow<List<Long>>

    @Transaction
    @Query("SELECT m.* FROM memories m INNER JOIN memory_collection_cross_ref mc ON m.id = mc.memoryId WHERE mc.collectionId = :collectionId ORDER BY m.memoryDate DESC")
    fun getMemoriesInCollectionWithDetails(collectionId: Long): Flow<List<MemoryWithDetails>>
}
