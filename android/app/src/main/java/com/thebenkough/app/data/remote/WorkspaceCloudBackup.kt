package com.thebenkough.app.data.remote

import com.thebenkough.app.data.local.WorkspaceRecordEntity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
private data class CloudWorkspaceRecord(
    @SerialName("user_id") val userId: String,
    @SerialName("local_id") val localId: Long,
    val module: String,
    val title: String,
    val details: String,
    @SerialName("local_updated_at") val localUpdatedAt: Long
)

/**
 * One-way backup of Room records to the signed-in user's private Supabase rows.
 * The publishable key is used; RLS is the authorization boundary.
 */
object WorkspaceCloudBackup {
    suspend fun backup(records: List<WorkspaceRecordEntity>): Int = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: error("No signed-in account. Sign in before cloud backup.")

        if (records.isEmpty()) return@withContext 0

        val payload = records.map { record ->
            CloudWorkspaceRecord(
                userId = userId,
                localId = record.id,
                module = record.module,
                title = record.title,
                details = record.details,
                localUpdatedAt = record.updatedAt
            )
        }

        SupabaseProvider.client.postgrest["workspace_records"].insert(
            payload,
            upsert = true,
            onConflict = "user_id,module,local_id"
        )
        payload.size
    }
}
