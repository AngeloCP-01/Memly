package com.example.memly.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.memly.data.local.entity.MediaFileEntity
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.MemoryTagCrossRef
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.local.entity.Mood
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Insert
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("SELECT * FROM memories ORDER BY memoryDate DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :memoryId")
    suspend fun getMemoryById(memoryId: Long): MemoryEntity?

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getMemoryCount(): Int

    @Query("SELECT COUNT(*) FROM media_files")
    suspend fun getMediaFileCount(): Int

    @Transaction
    @Query("SELECT * FROM memories ORDER BY memoryDate DESC")
    fun getAllMemoriesWithDetails(): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE id = :memoryId")
    suspend fun getMemoryWithDetails(memoryId: Long): MemoryWithDetails?

    @Query("SELECT * FROM memories WHERE mood = :mood ORDER BY memoryDate DESC")
    fun getMemoriesByMood(mood: Mood): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE memoryDate BETWEEN :startDate AND :endDate ORDER BY memoryDate DESC")
    fun getMemoriesByDateRange(startDate: Long, endDate: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR placeLabel LIKE '%' || :query || '%' ORDER BY memoryDate DESC")
    fun searchMemories(query: String): Flow<List<MemoryEntity>>

    @Transaction
    @Query("SELECT * FROM memories WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR placeLabel LIKE '%' || :query || '%' ORDER BY memoryDate DESC")
    fun searchMemoriesWithDetails(query: String): Flow<List<MemoryWithDetails>>

    @Query("SELECT * FROM memories WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY memoryDate DESC")
    fun getGeotaggedMemories(): Flow<List<MemoryEntity>>

    @Transaction
    @Query("SELECT * FROM memories WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY memoryDate DESC")
    fun getGeotaggedMemoriesWithDetails(): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM memories
        WHERE strftime('%m-%d', memoryDate / 1000, 'unixepoch', 'localtime') = strftime('%m-%d', :todayMillis / 1000, 'unixepoch', 'localtime')
        AND strftime('%Y', memoryDate / 1000, 'unixepoch', 'localtime') != strftime('%Y', :todayMillis / 1000, 'unixepoch', 'localtime')
        ORDER BY memoryDate DESC
    """)
    fun getTimeHopMemories(todayMillis: Long): Flow<List<MemoryWithDetails>>

    // Media file operations
    @Insert
    suspend fun insertMediaFile(mediaFile: MediaFileEntity): Long

    @Delete
    suspend fun deleteMediaFile(mediaFile: MediaFileEntity)

    @Query("SELECT * FROM media_files WHERE memoryId = :memoryId")
    fun getMediaFilesForMemory(memoryId: Long): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE fileHash = :hash LIMIT 1")
    suspend fun findMediaByHash(hash: String): MediaFileEntity?

    // Tag association
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMemoryTagCrossRef(crossRef: MemoryTagCrossRef)

    @Delete
    suspend fun deleteMemoryTagCrossRef(crossRef: MemoryTagCrossRef)
}
