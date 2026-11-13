package com.thebenkough.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceRecordDao {
    @Query("SELECT * FROM workspace_records WHERE module = :module ORDER BY updatedAt DESC")
    fun observeModule(module: String): Flow<List<WorkspaceRecordEntity>>

    @Query("SELECT * FROM workspace_records ORDER BY updatedAt DESC")
    suspend fun getAll(): List<WorkspaceRecordEntity>

    @Query("SELECT * FROM workspace_records WHERE cloudKey = :cloudKey LIMIT 1")
    suspend fun findByCloudKey(cloudKey: String): WorkspaceRecordEntity?

    @Insert
    suspend fun insert(record: WorkspaceRecordEntity)

    @Upsert
    suspend fun upsert(record: WorkspaceRecordEntity)

    @Query("DELETE FROM workspace_records WHERE id = :id")
    suspend fun delete(id: Long)
}
