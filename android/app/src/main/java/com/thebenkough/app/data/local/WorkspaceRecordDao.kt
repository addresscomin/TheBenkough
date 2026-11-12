package com.thebenkough.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceRecordDao {
    @Query("SELECT * FROM workspace_records WHERE module = :module ORDER BY updatedAt DESC")
    fun observeModule(module: String): Flow<List<WorkspaceRecordEntity>>

    @Insert
    suspend fun insert(record: WorkspaceRecordEntity)

    @Update
    suspend fun update(record: WorkspaceRecordEntity)

    @Query("DELETE FROM workspace_records WHERE id = :id")
    suspend fun delete(id: Long)
}
