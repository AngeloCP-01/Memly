package com.example.memly.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memly.data.local.entity.MediaFileEntity
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.Mood
import com.example.memly.data.local.entity.TagEntity
import com.example.memly.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val memory: MemoryEntity? = null,
    val mediaFiles: List<MediaFileEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    // Edit fields
    val editTitle: String = "",
    val editNotes: String = "",
    val editMood: Mood? = null,
    val editPlaceLabel: String = "",
    val editTags: List<String> = emptyList(),
    val editTagInput: String = ""
)

@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val memoryId: Long = savedStateHandle["memoryId"] ?: -1L

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadMemory()
    }

    private fun loadMemory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val details = memoryRepository.getMemoryWithDetails(memoryId)
            if (details != null) {
                _uiState.update {
                    it.copy(
                        memory = details.memory,
                        mediaFiles = details.mediaFiles,
                        tags = details.tags,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Memory not found") }
            }
        }
    }

    fun startEditing() {
        val state = _uiState.value
        val memory = state.memory ?: return
        _uiState.update {
            it.copy(
                isEditing = true,
                editTitle = memory.title ?: "",
                editNotes = memory.notes ?: "",
                editMood = memory.mood,
                editPlaceLabel = memory.placeLabel ?: "",
                editTags = state.tags.map { tag -> tag.name },
                editTagInput = ""
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun updateEditTitle(title: String) {
        _uiState.update { it.copy(editTitle = title) }
    }

    fun updateEditNotes(notes: String) {
        _uiState.update { it.copy(editNotes = notes) }
    }

    fun selectEditMood(mood: Mood?) {
        _uiState.update { it.copy(editMood = if (it.editMood == mood) null else mood) }
    }

    fun updateEditPlaceLabel(label: String) {
        _uiState.update { it.copy(editPlaceLabel = label) }
    }

    fun updateEditTagInput(input: String) {
        _uiState.update { it.copy(editTagInput = input) }
    }

    fun addEditTag() {
        val tag = _uiState.value.editTagInput.trim()
        if (tag.isNotEmpty() && tag !in _uiState.value.editTags) {
            _uiState.update { it.copy(editTags = it.editTags + tag, editTagInput = "") }
        }
    }

    fun removeEditTag(tag: String) {
        _uiState.update { it.copy(editTags = it.editTags - tag) }
    }

    fun saveEdits() {
        val state = _uiState.value
        val memory = state.memory ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val updatedMemory = memory.copy(
                    title = state.editTitle.ifBlank { null },
                    notes = state.editNotes.ifBlank { null },
                    mood = state.editMood,
                    placeLabel = state.editPlaceLabel.ifBlank { null }
                )
                memoryRepository.updateMemory(updatedMemory)

                // Update tags: remove old, add new
                val oldTagNames = state.tags.map { it.name }.toSet()
                val newTagNames = state.editTags.toSet()

                for (tag in state.tags) {
                    if (tag.name !in newTagNames) {
                        memoryRepository.removeTagFromMemory(memory.id, tag.id)
                    }
                }
                for (tagName in newTagNames) {
                    if (tagName !in oldTagNames) {
                        memoryRepository.addTagToMemory(memory.id, tagName)
                    }
                }

                // Reload to get fresh data
                val details = memoryRepository.getMemoryWithDetails(memory.id)
                if (details != null) {
                    _uiState.update {
                        it.copy(
                            memory = details.memory,
                            tags = details.tags,
                            isEditing = false,
                            isSaving = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = "Failed to save: ${e.message}")
                }
            }
        }
    }

    fun deleteMemory() {
        val memory = _uiState.value.memory ?: return
        viewModelScope.launch {
            try {
                memoryRepository.deleteMemory(memory)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
