package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String, // UUID string for unique offline generation
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val isDeletedLocally: Boolean = false // Boolean flag to support offline soft deletes until next sync
)
