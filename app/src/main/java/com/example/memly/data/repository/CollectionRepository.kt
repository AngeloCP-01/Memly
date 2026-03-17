package com.example.memly.data.repository

import com.example.memly.data.local.dao.CollectionDao
import com.example.memly.data.local.entity.CollectionEntity
import com.example.memly.data.local.entity.MemoryCollectionCrossRef
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.MemoryWithDetails
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepository @Inject constructor(
    private val collectionDao: CollectionDao
) {
    fun getAllCollections(): Flow<List<CollectionEntity>> =
        collectionDao.getAllCollections()

    fun searchCollections(query: String): Flow<List<CollectionEntity>> =
        collectionDao.searchCollections(query)

    suspend fun getCollectionById(collectionId: Long): CollectionEntity? =
        collectionDao.getCollectionById(collectionId)

    suspend fun createCollection(collection: CollectionEntity): Long =
        collectionDao.insertCollection(collection)

    suspend fun updateCollection(collection: CollectionEntity) =
        collectionDao.updateCollection(collection)

    suspend fun deleteCollection(collection: CollectionEntity) =
        collectionDao.deleteCollection(collection)

    suspend fun addMemoryToCollection(memoryId: Long, collectionId: Long) {
        collectionDao.insertMemoryCollectionCrossRef(
            MemoryCollectionCrossRef(memoryId, collectionId)
        )
    }

    suspend fun removeMemoryFromCollection(memoryId: Long, collectionId: Long) {
        collectionDao.deleteMemoryCollectionCrossRef(
            MemoryCollectionCrossRef(memoryId, collectionId)
        )
    }

    fun getMemoriesInCollection(collectionId: Long): Flow<List<MemoryEntity>> =
        collectionDao.getMemoriesInCollection(collectionId)

    fun getMemoriesInCollectionWithDetails(collectionId: Long): Flow<List<MemoryWithDetails>> =
        collectionDao.getMemoriesInCollectionWithDetails(collectionId)

    fun getMemoryCountInCollection(collectionId: Long): Flow<Int> =
        collectionDao.getMemoryCountInCollection(collectionId)

    fun getCollectionIdsForMemory(memoryId: Long): Flow<List<Long>> =
        collectionDao.getCollectionIdsForMemory(memoryId)
}
