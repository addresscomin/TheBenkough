package com.thebenkough.app.feature.script

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thebenkough.app.data.local.ScriptDraftDao
import com.thebenkough.app.data.local.ScriptDraftEntity
import com.thebenkough.app.data.local.WorkspaceRecordDao
import com.thebenkough.app.data.local.WorkspaceRecordEntity
import com.thebenkough.app.data.remote.WorkspaceCloudBackup
import com.thebenkough.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScriptDraftUiState(
    val title: String = "Untitled script",
    val body: String = "",
    val saveStatus: String = "LOADING DRAFT"
)

class ScriptDraftViewModel(
    private val dao: ScriptDraftDao,
    private val workspaceDao: WorkspaceRecordDao
) : ViewModel() {
    private val _state = MutableStateFlow(ScriptDraftUiState())
    val state: StateFlow<ScriptDraftUiState> = _state.asStateFlow()
    private var loaded = false
    private var signedInUserId: String? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            if (userId == null) {
                _state.update { it.copy(saveStatus = "SIGN IN TO SAVE YOUR SCRIPT") }
                return@launch
            }
            signedInUserId = userId
            val saved = dao.getDraft()
            if (saved != null) {
                _state.update { it.copy(title = saved.title, body = saved.body, saveStatus = "SAVED ON THIS DEVICE") }
            } else {
                _state.update { it.copy(saveStatus = "NEW LOCAL DRAFT") }
            }
            loaded = true
        }
    }

    fun updateTitle(value: String) {
        _state.update { it.copy(title = value) }
        queueSave()
    }

    fun updateBody(value: String) {
        _state.update { it.copy(body = value) }
        queueSave()
    }

    private fun queueSave() {
        if (!loaded) return
        val userId = signedInUserId ?: return
        saveJob?.cancel()
        _state.update { it.copy(saveStatus = "SAVING…") }
        saveJob = viewModelScope.launch {
            delay(650)
            val current = _state.value
            val timestamp = System.currentTimeMillis()
            try {
                dao.saveDraft(ScriptDraftEntity(title = current.title, body = current.body, updatedAt = timestamp))
                val existing = workspaceDao.findByCloudKey(SCRIPT_CLOUD_KEY, userId)
                val cloudRecord = WorkspaceRecordEntity(
                    id = existing?.id ?: 0,
                    module = "CREATOR_SCRIPT",
                    title = current.title,
                    details = current.body,
                    ownerUserId = userId,
                    cloudKey = SCRIPT_CLOUD_KEY,
                    updatedAt = timestamp
                )
                workspaceDao.upsert(cloudRecord)
                _state.update { it.copy(saveStatus = "SAVED ON DEVICE · SYNCING CLOUD…") }
                try {
                    WorkspaceCloudBackup.sync(workspaceDao)
                    _state.update { it.copy(saveStatus = "SAVED + SYNCED TO CLOUD") }
                } catch (_: Exception) {
                    _state.update { it.copy(saveStatus = "SAVED ON DEVICE · CLOUD SYNC WHEN ONLINE") }
                }
            } catch (_: Exception) {
                _state.update { it.copy(saveStatus = "SAVE FAILED — TRY AGAIN") }
            }
        }
    }

    companion object {
        private const val SCRIPT_CLOUD_KEY = "benkough-creator-script-main"
    }
}

class ScriptDraftViewModelFactory(
    private val dao: ScriptDraftDao,
    private val workspaceDao: WorkspaceRecordDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScriptDraftViewModel::class.java)) {
            return ScriptDraftViewModel(dao, workspaceDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
