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
    val filePath: String,
    val thumbnailPath: String? = null,
    val fileHash: String,
    val mediaType: MediaType,
    val isReference: Boolean = true
)
