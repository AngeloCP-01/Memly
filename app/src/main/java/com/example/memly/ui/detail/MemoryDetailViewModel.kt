package com.example.memly.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.memly.data.local.entity.CollectionEntity
import com.example.memly.data.local.entity.MediaFileEntity
import com.example.memly.data.local.entity.MediaSource
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.Mood
import com.example.memly.data.local.entity.TagEntity
import com.example.memly.data.repository.CollectionRepository
import com.example.memly.data.repository.MemoryRepository
import com.example.memly.util.MediaStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val editTagInput: String = "",
    // Collections
    val showCollectionDialog: Boolean = false,
    val allCollections: List<CollectionEntity> = emptyList(),
    val memberCollectionIds: Set<Long> = emptySet(),
    // Broken references
    val brokenMediaIds: Set<Long> = emptySet()
)

@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val memoryRepository: MemoryRepository,
    private val collectionRepository: CollectionRepository,
    private val mediaStoreManager: MediaStoreManager
) : ViewModel() {
    companion object {
        private const val TAG = "MemlyDetail"
    }

    private val memoryId: Long = savedStateHandle["memoryId"] ?: -1L

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadMemory()
    }

    private fun loadMemory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val details = memoryRepository.getMemoryWithDetails(memoryId)
                if (details != null) {
                    Log.d(TAG, "━━━ loadMemory id=$memoryId ━━━")
                    Log.d(TAG, "  mediaFiles count=${details.mediaFiles.size}")
                    for (mf in details.mediaFiles) {
                        Log.d(TAG, "  ── media id=${mf.id} ──")
                        Log.d(TAG, "    source=${mf.source}")
                        Log.d(TAG, "    mediaStoreUri=${mf.mediaStoreUri}")
                        Log.d(TAG, "    mimeType=${mf.mimeType}")
                        Log.d(TAG, "    thumbnailPath=${mf.thumbnailPath}")
                        // Readability test
                        try {
                            val uri = Uri.parse(mf.mediaStoreUri)
                            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                                Log.d(TAG, "    ✓ readable, available=${stream.available()}")
                            } ?: Log.e(TAG, "    ✗ openInputStream returned null")
                        } catch (e: SecurityException) {
                            Log.e(TAG, "    ✗ SecurityException: ${e.message}")
                        } catch (e: java.io.FileNotFoundException) {
                            Log.e(TAG, "    ✗ FileNotFoundException: ${e.message}")
                        } catch (e: Exception) {
                            Log.e(TAG, "    ✗ Exception: ${e.message}")
                        }
                    }

                    // Check ALL media files for broken/inaccessible URIs (IO-bound)
                    val brokenIds = withContext(Dispatchers.IO) {
                        details.mediaFiles
                            .filter { !mediaStoreManager.isUriAccessible(Uri.parse(it.mediaStoreUri)) }
                            .map { it.id }
                            .toSet()
                    }
                    Log.d(TAG, "  brokenIds=$brokenIds")

                    _uiState.update {
                        it.copy(
                            memory = details.memory,
                            mediaFiles = details.mediaFiles,
                            tags = details.tags,
                            isLoading = false,
                            brokenMediaIds = brokenIds
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Memory not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load: ${e.message}") }
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

    fun importToMemly(mediaFile: MediaFileEntity) {
        if (mediaFile.source != MediaSource.EXTERNAL) return
        viewModelScope.launch {
            try {
                val sourceUri = Uri.parse(mediaFile.mediaStoreUri)
                val mimeType = mediaFile.mimeType ?: "image/jpeg"
                val metadata = mediaStoreManager.insertMedia(sourceUri, mediaFile.mediaType, mimeType)
                    ?: throw Exception("Failed to copy file")

                val updated = mediaFile.copy(
                    mediaStoreUri = metadata.uri.toString(),
                    source = MediaSource.IMPORTED,
                    relativePath = metadata.relativePath,
                    displayName = metadata.displayName,
                    size = metadata.size,
                    dateTaken = metadata.dateTaken,
                    width = metadata.width,
                    height = metadata.height
                )
                memoryRepository.updateMediaFile(updated)

                // Reload
                loadMemory()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to import: ${e.message}") }
            }
        }
    }

    fun removeBrokenReference(mediaFile: MediaFileEntity) {
        viewModelScope.launch {
            try {
                memoryRepository.removeMediaFile(mediaFile)
                loadMemory()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to remove: ${e.message}") }
            }
        }
    }

    fun deleteMemory() {
        val state = _uiState.value
        val memory = state.memory ?: return
        viewModelScope.launch {
            try {
                memoryRepository.deleteMemoryWithFiles(memory, state.mediaFiles)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun showCollectionDialog() {
        viewModelScope.launch {
            try {
                val collections = collectionRepository.getAllCollections().first()
                val memberIds = collectionRepository.getCollectionIdsForMemory(memoryId).first().toSet()
                _uiState.update {
                    it.copy(
                        showCollectionDialog = true,
                        allCollections = collections,
                        memberCollectionIds = memberIds
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load collections: ${e.message}") }
            }
        }
    }

    fun hideCollectionDialog() {
        _uiState.update { it.copy(showCollectionDialog = false) }
    }

    fun toggleCollection(collectionId: Long) {
        viewModelScope.launch {
            try {
                val isMember = collectionId in _uiState.value.memberCollectionIds
                if (isMember) {
                    collectionRepository.removeMemoryFromCollection(memoryId, collectionId)
                    _uiState.update {
                        it.copy(memberCollectionIds = it.memberCollectionIds - collectionId)
                    }
                } else {
                    collectionRepository.addMemoryToCollection(memoryId, collectionId)
                    _uiState.update {
                        it.copy(memberCollectionIds = it.memberCollectionIds + collectionId)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update collection: ${e.message}") }
            }
        }
    }
}
