package com.example.memly.data.repository

import androidx.room.withTransaction
import com.example.memly.data.local.MemlyDatabase
import com.example.memly.data.local.dao.MemoryDao
import com.example.memly.data.local.dao.TagDao
import com.example.memly.data.local.entity.MediaFileEntity
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.MemoryTagCrossRef
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.local.entity.Mood
import com.example.memly.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val database: MemlyDatabase,
    private val memoryDao: MemoryDao,
    private val tagDao: TagDao
) {
    fun getAllMemoriesWithDetails(): Flow<List<MemoryWithDetails>> =
        memoryDao.getAllMemoriesWithDetails()

    suspend fun getMemoryWithDetails(memoryId: Long): MemoryWithDetails? =
        memoryDao.getMemoryWithDetails(memoryId)

    suspend fun createMemory(memory: MemoryEntity): Long =
        memoryDao.insertMemory(memory)

    suspend fun updateMemory(memory: MemoryEntity) =
        memoryDao.updateMemory(memory)

    suspend fun deleteMemory(memory: MemoryEntity) =
        memoryDao.deleteMemory(memory)

    fun getMemoriesByMood(mood: Mood): Flow<List<MemoryEntity>> =
        memoryDao.getMemoriesByMood(mood)

    fun getMemoriesByDateRange(startDate: Long, endDate: Long): Flow<List<MemoryEntity>> =
        memoryDao.getMemoriesByDateRange(startDate, endDate)

    fun searchMemories(query: String): Flow<List<MemoryEntity>> =
        memoryDao.searchMemories(query)

    fun getGeotaggedMemories(): Flow<List<MemoryEntity>> =
        memoryDao.getGeotaggedMemories()

    // Media files
    suspend fun addMediaFile(mediaFile: MediaFileEntity): Long =
        memoryDao.insertMediaFile(mediaFile)

    suspend fun removeMediaFile(mediaFile: MediaFileEntity) =
        memoryDao.deleteMediaFile(mediaFile)

    suspend fun findMediaByHash(hash: String): MediaFileEntity? =
        memoryDao.findMediaByHash(hash)

    fun getMediaFilesForMemory(memoryId: Long): Flow<List<MediaFileEntity>> =
        memoryDao.getMediaFilesForMemory(memoryId)

    // Tags
    suspend fun addTagToMemory(memoryId: Long, tagName: String) {
        val existingTag = tagDao.getTagByName(tagName)
        val tagId = existingTag?.id ?: tagDao.insertTag(TagEntity(name = tagName))
        memoryDao.insertMemoryTagCrossRef(MemoryTagCrossRef(memoryId, tagId))
    }

    suspend fun removeTagFromMemory(memoryId: Long, tagId: Long) {
        memoryDao.deleteMemoryTagCrossRef(MemoryTagCrossRef(memoryId, tagId))
    }

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun createMemoryWithDetails(
        memory: MemoryEntity,
        mediaFiles: List<MediaFileEntity>,
        tagNames: List<String>
    ): Long = database.withTransaction {
        val memoryId = memoryDao.insertMemory(memory)

        for (mediaFile in mediaFiles) {
            memoryDao.insertMediaFile(mediaFile.copy(memoryId = memoryId))
        }

        for (tagName in tagNames) {
            val existingTag = tagDao.getTagByName(tagName)
            val tagId = existingTag?.id ?: tagDao.insertTag(TagEntity(name = tagName))
            memoryDao.insertMemoryTagCrossRef(MemoryTagCrossRef(memoryId, tagId))
        }

        memoryId
    }
}
