package com.example.memly.ui.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memly.data.local.entity.CollectionEntity
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.repository.CollectionRepository
import com.example.memly.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionDetailUiState(
    val collection: CollectionEntity? = null,
    val memories: List<MemoryWithDetails> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // Add memory dialog
    val showAddMemoryDialog: Boolean = false,
    val allMemories: List<MemoryWithDetails> = emptyList(),
    val memberMemoryIds: Set<Long> = emptySet()
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collectionRepository: CollectionRepository,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val collectionId: Long = savedStateHandle["collectionId"] ?: -1L

    private val _collection = MutableStateFlow<CollectionEntity?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _dialogState = MutableStateFlow(
        DialogState(showDialog = false, allMemories = emptyList(), memberIds = emptySet())
    )

    private data class DialogState(
        val showDialog: Boolean,
        val allMemories: List<MemoryWithDetails>,
        val memberIds: Set<Long>
    )

    val uiState: StateFlow<CollectionDetailUiState> = combine(
        _collection,
        collectionRepository.getMemoriesInCollectionWithDetails(collectionId),
        _dialogState,
        _error
    ) { collection, memories, dialog, error ->
        CollectionDetailUiState(
            collection = collection,
            memories = memories,
            isLoading = false,
            error = error,
            showAddMemoryDialog = dialog.showDialog,
            allMemories = dialog.allMemories,
            memberMemoryIds = dialog.memberIds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionDetailUiState())

    init {
        loadCollection()
    }

    private fun loadCollection() {
        viewModelScope.launch {
            try {
                _collection.value = collectionRepository.getCollectionById(collectionId)
            } catch (_: Exception) {
                // Collection not found — UI will show null state
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun removeMemoryFromCollection(memoryId: Long) {
        viewModelScope.launch {
            try {
                collectionRepository.removeMemoryFromCollection(memoryId, collectionId)
                _dialogState.update { it.copy(memberIds = it.memberIds - memoryId) }
            } catch (e: Exception) {
                _error.value = "Failed to remove memory: ${e.message}"
            }
        }
    }

    fun showAddMemoryDialog() {
        viewModelScope.launch {
            try {
                val allMemories = memoryRepository.getAllMemoriesWithDetails().first()
                val memberIds = collectionRepository.getMemoriesInCollectionWithDetails(collectionId)
                    .first()
                    .map { it.memory.id }
                    .toSet()
                _dialogState.value = DialogState(
                    showDialog = true,
                    allMemories = allMemories,
                    memberIds = memberIds
                )
            } catch (e: Exception) {
                _error.value = "Failed to load memories: ${e.message}"
            }
        }
    }

    fun hideAddMemoryDialog() {
        _dialogState.update { it.copy(showDialog = false) }
    }

    fun toggleMemoryInCollection(memoryId: Long) {
        viewModelScope.launch {
            try {
                val isMember = memoryId in _dialogState.value.memberIds
                if (isMember) {
                    collectionRepository.removeMemoryFromCollection(memoryId, collectionId)
                    _dialogState.update { it.copy(memberIds = it.memberIds - memoryId) }
                } else {
                    collectionRepository.addMemoryToCollection(memoryId, collectionId)
                    _dialogState.update { it.copy(memberIds = it.memberIds + memoryId) }
                }
            } catch (e: Exception) {
                _error.value = "Failed to update collection: ${e.message}"
            }
        }
    }
}
