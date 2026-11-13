package com.thebenkough.app.data.remote

import com.thebenkough.app.data.local.WorkspaceRecordDao
import com.thebenkough.app.data.local.WorkspaceRecordEntity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import java.util.UUID

@Serializable
private data class CloudWorkspaceRecord(
    @SerialName("user_id") val userId: String,
    @SerialName("local_id") val localId: Long,
    @SerialName("record_key") val recordKey: String,
    val module: String,
    val title: String,
    val details: String,
    @SerialName("local_updated_at") val localUpdatedAt: Long
)

/** Two-way sync: newer cloud copies are restored, then local changes are upserted. */
object WorkspaceCloudBackup {
    suspend fun sync(dao: WorkspaceRecordDao): Int = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: error("No signed-in account. Sign in before syncing.")

        val remote = SupabaseProvider.client.postgrest["workspace_records"]
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<CloudWorkspaceRecord>()

        var restored = 0
        remote.forEach { cloud ->
            val existing = dao.findByCloudKey(cloud.recordKey)
            if (existing == null || cloud.localUpdatedAt > existing.updatedAt) {
                dao.upsert(
                    WorkspaceRecordEntity(
                        id = existing?.id ?: 0,
                        module = cloud.module,
                        title = cloud.title,
                        details = cloud.details,
                        cloudKey = cloud.recordKey,
                        updatedAt = cloud.localUpdatedAt
                    )
                )
                restored++
            }
        }

        val local = dao.getAll().map { record ->
            val stableKey = record.cloudKey.ifBlank { UUID.randomUUID().toString() }
            if (stableKey != record.cloudKey) {
                dao.upsert(record.copy(cloudKey = stableKey))
            }
            CloudWorkspaceRecord(
                userId = userId,
                localId = record.id,
                recordKey = stableKey,
                module = record.module,
                title = record.title,
                details = record.details,
                localUpdatedAt = record.updatedAt
            )
        }

        if (local.isNotEmpty()) {
            val jsonArray = Json.encodeToJsonElement(
                ListSerializer(CloudWorkspaceRecord.serializer()),
                local
            ).jsonArray
            SupabaseProvider.client.postgrest["workspace_records"].insert(
                jsonArray,
                upsert = true,
                onConflict = "user_id,record_key"
            )
        }
        restored
    }
}
