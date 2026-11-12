package com.thebenkough.app.feature.script

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thebenkough.app.data.local.ScriptDraftDao
import com.thebenkough.app.data.local.ScriptDraftEntity
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

class ScriptDraftViewModel(private val dao: ScriptDraftDao) : ViewModel() {
    private val _state = MutableStateFlow(ScriptDraftUiState())
    val state: StateFlow<ScriptDraftUiState> = _state.asStateFlow()

    private var loaded = false
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = dao.getDraft()
            if (saved != null) {
                _state.update {
                    it.copy(title = saved.title, body = saved.body, saveStatus = "SAVED ON THIS DEVICE")
                }
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
        saveJob?.cancel()
        _state.update { it.copy(saveStatus = "SAVING…") }
        saveJob = viewModelScope.launch {
            delay(450)
            val current = _state.value
            try {
                dao.saveDraft(
                    ScriptDraftEntity(
                        title = current.title,
                        body = current.body,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                _state.update { it.copy(saveStatus = "SAVED ON THIS DEVICE") }
            } catch (_: Exception) {
                _state.update { it.copy(saveStatus = "SAVE FAILED — TRY AGAIN") }
            }
        }
    }
}

class ScriptDraftViewModelFactory(
    private val dao: ScriptDraftDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScriptDraftViewModel::class.java)) {
            return ScriptDraftViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
