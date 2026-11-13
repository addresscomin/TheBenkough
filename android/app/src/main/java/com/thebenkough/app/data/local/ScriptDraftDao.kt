package com.thebenkough.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ScriptDraftDao {
    @Query("SELECT * FROM script_drafts WHERE id = 1 LIMIT 1")
    suspend fun getDraft(): ScriptDraftEntity?

    @Upsert
    suspend fun saveDraft(draft: ScriptDraftEntity)
}
