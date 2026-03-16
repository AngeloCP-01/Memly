package com.example.memly.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class MemoryWithDetails(
    @Embedded val memory: MemoryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "memoryId"
    )
    val mediaFiles: List<MediaFileEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MemoryTagCrossRef::class,
            parentColumn = "memoryId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
