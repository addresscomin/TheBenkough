package com.thebenkough.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_records")
data class WorkspaceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val module: String,
    val title: String,
    val details: String,
    val updatedAt: Long = System.currentTimeMillis()
)
