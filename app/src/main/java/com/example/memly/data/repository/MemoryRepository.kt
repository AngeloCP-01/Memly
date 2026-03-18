package com.example.memly.data.repository

import android.net.Uri
import androidx.room.withTransaction
import com.example.memly.data.local.MemlyDatabase
import com.example.memly.data.local.dao.MemoryDao
import com.example.memly.data.local.dao.TagDao
import com.example.memly.data.local.entity.MediaFileEntity
import com.example.memly.data.local.entity.MediaSource
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.MemoryTagCrossRef
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.local.entity.Mood
import com.example.memly.data.local.entity.TagEntity
import com.example.memly.util.MediaStoreManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val database: MemlyDatabase,
    private val memoryDao: MemoryDao,
    private val tagDao: TagDao,
    private val mediaStoreManager: MediaStoreManager
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

    fun searchMemoriesWithDetails(query: String): Flow<List<MemoryWithDetails>> =
        memoryDao.searchMemoriesWithDetails(query)

    fun getGeotaggedMemories(): Flow<List<MemoryEntity>> =
        memoryDao.getGeotaggedMemories()

    fun getGeotaggedMemoriesWithDetails(): Flow<List<MemoryWithDetails>> =
        memoryDao.getGeotaggedMemoriesWithDetails()

    // Media files
    suspend fun addMediaFile(mediaFile: MediaFileEntity): Long =
        memoryDao.insertMediaFile(mediaFile)

    suspend fun removeMediaFile(mediaFile: MediaFileEntity) {
        memoryDao.deleteMediaFile(mediaFile)
        // Delete actual file for owned/imported media
        if (mediaFile.source != MediaSource.EXTERNAL) {
            mediaStoreManager.deleteOwnedMedia(Uri.parse(mediaFile.mediaStoreUri))
        }
    }

    suspend fun updateMediaFile(mediaFile: MediaFileEntity) =
        memoryDao.updateMediaFile(mediaFile)

    suspend fun findMediaByHash(hash: String): MediaFileEntity? =
        memoryDao.findMediaByHash(hash)

    suspend fun findMediaByUri(uri: String): MediaFileEntity? =
        memoryDao.findMediaByUri(uri)

    fun getMediaFilesForMemory(memoryId: Long): Flow<List<MediaFileEntity>> =
        memoryDao.getMediaFilesForMemory(memoryId)

    fun getMediaFilesBySource(source: MediaSource): Flow<List<MediaFileEntity>> =
        memoryDao.getMediaFilesBySource(source)

    /**
     * Delete a memory and clean up its owned/imported media files from public storage.
     */
    suspend fun deleteMemoryWithFiles(memory: MemoryEntity, mediaFiles: List<MediaFileEntity>) {
        // Delete owned/imported files from MediaStore
        for (file in mediaFiles) {
            if (file.source != MediaSource.EXTERNAL) {
                mediaStoreManager.deleteOwnedMedia(Uri.parse(file.mediaStoreUri))
            }
        }
        // Room CASCADE will delete media_files rows
        memoryDao.deleteMemory(memory)
    }

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

    fun getTimeHopMemories(todayMillis: Long): Flow<List<MemoryWithDetails>> =
        memoryDao.getTimeHopMemories(todayMillis)

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
