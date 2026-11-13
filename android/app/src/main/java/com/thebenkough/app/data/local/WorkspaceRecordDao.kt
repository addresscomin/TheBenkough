package com.thebenkough.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceRecordDao {
    @Query("SELECT * FROM workspace_records WHERE module = :module AND ownerUserId = :userId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeModule(module: String, userId: String): Flow<List<WorkspaceRecordEntity>>

    @Query("SELECT * FROM workspace_records WHERE ownerUserId = :userId ORDER BY updatedAt DESC")
    suspend fun getAll(userId: String): List<WorkspaceRecordEntity>

    @Query("SELECT * FROM workspace_records WHERE cloudKey = :cloudKey AND ownerUserId = :userId LIMIT 1")
    suspend fun findByCloudKey(cloudKey: String, userId: String): WorkspaceRecordEntity?

    @Query("UPDATE workspace_records SET ownerUserId = :userId WHERE ownerUserId = ''")
    suspend fun claimUnowned(userId: String)

    @Insert
    suspend fun insert(record: WorkspaceRecordEntity)

    @Upsert
    suspend fun upsert(record: WorkspaceRecordEntity)

    @Query("UPDATE workspace_records SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id AND ownerUserId = :userId")
    suspend fun softDelete(id: Long, userId: String, updatedAt: Long = System.currentTimeMillis())
}
