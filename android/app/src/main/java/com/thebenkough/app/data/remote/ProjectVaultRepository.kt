package com.thebenkough.app.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

private const val VAULT_BUCKET = "workspace-assets"

@Serializable
data class VaultAsset(
    val id: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("original_name") val originalName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long
)

@Serializable
private data class WorkspaceLookup(val id: String)

object ProjectVaultRepository {
    private suspend fun identity(): Pair<String, String> {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: error("Sign in to access your private vault.")
        val workspaces = SupabaseProvider.client.postgrest["workspaces"]
            .select { filter { eq("owner_id", userId) } }
            .decodeList<WorkspaceLookup>()
        val workspaceId = workspaces.firstOrNull()?.id
            ?: error("No personal workspace was found for this account.")
        return userId to workspaceId
    }

    suspend fun listAssets(): List<VaultAsset> = withContext(Dispatchers.IO) {
        val (userId, workspaceId) = identity()
        SupabaseProvider.client.postgrest["assets"]
            .select { filter { eq("workspace_id", workspaceId) } }
            .decodeList<VaultAsset>()
    }

    suspend fun upload(context: Context, uri: Uri): VaultAsset = withContext(Dispatchers.IO) {
        val (userId, workspaceId) = identity()
        val name = queryDisplayName(context, uri)
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read the selected file.")
        if (bytes.isEmpty()) error("The selected file is empty.")
        if (bytes.size > 25 * 1024 * 1024) error("File is larger than the 25 MB upload limit.")

        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100).ifBlank { "asset.bin" }
        val path = "$workspaceId/$userId/${UUID.randomUUID()}-$safeName"
        SupabaseProvider.client.storage.from(VAULT_BUCKET).upload(path, bytes, upsert = false)

        val metadata = JsonObject(
            mapOf(
                "workspace_id" to JsonPrimitive(workspaceId),
                "storage_path" to JsonPrimitive(path),
                "original_name" to JsonPrimitive(name),
                "mime_type" to JsonPrimitive(mime),
                "size_bytes" to JsonPrimitive(bytes.size.toLong()),
                "uploaded_by" to JsonPrimitive(userId)
            )
        )
        SupabaseProvider.client.postgrest["assets"].insert(metadata)
        VaultAsset(
            id = UUID.randomUUID().toString(),
            workspaceId = workspaceId,
            storagePath = path,
            originalName = name,
            mimeType = mime,
            sizeBytes = bytes.size.toLong()
        )
    }

    suspend fun download(path: String): ByteArray = withContext(Dispatchers.IO) {
        identity()
        SupabaseProvider.client.storage.from(VAULT_BUCKET).downloadAuthenticated(path)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        var name = "asset"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = cursor.getString(index) ?: name
            }
        }
        return name
    }
}
