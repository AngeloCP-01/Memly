package com.example.memly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String? = null,
    val notes: String? = null,
    val mood: Mood? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeLabel: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val memoryDate: Long = System.currentTimeMillis()
)
