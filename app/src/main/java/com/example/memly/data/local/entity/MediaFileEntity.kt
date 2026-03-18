package com.example.memly.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_files",
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("memoryId"), Index("fileHash")]
)
data class MediaFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memoryId: Long,
    val mediaStoreUri: String,
    val thumbnailPath: String? = null,
    val fileHash: String,
    val mediaType: MediaType,
    val source: MediaSource = MediaSource.APP_OWNED,
    val relativePath: String? = null,
    val displayName: String? = null,
    val mimeType: String? = null,
    val size: Long = 0,
    val dateTaken: Long? = null,
    val width: Int? = null,
    val height: Int? = null
)
