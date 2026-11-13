package com.thebenkough.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "script_drafts")
data class ScriptDraftEntity(
    @PrimaryKey val id: Int = 1,
    val title: String = "Untitled script",
    val body: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
