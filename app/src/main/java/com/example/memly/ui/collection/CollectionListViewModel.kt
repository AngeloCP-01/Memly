package com.example.memly.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memly.data.local.entity.CollectionEntity
import com.example.memly.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionWithCount(
    val collection: CollectionEntity,
    val memoryCount: Int
)

data class CollectionListUiState(
    val collections: List<CollectionWithCount> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val showDeleteDialog: CollectionEntity? = null,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CollectionListViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val _dialogState = MutableStateFlow(DialogState())

    private data class DialogState(
        val showCreateDialog: Boolean = false,
        val showDeleteDialog: CollectionEntity? = null,
        val error: String? = null
    )

    val uiState: StateFlow<CollectionListUiState> = combine(
        collectionRepository.getAllCollections().flatMapLatest { collections ->
            if (collections.isEmpty()) {
                flowOf(emptyList())
            } else {
                val countFlows = collections.map { collection ->
                    collectionRepository.getMemoryCountInCollection(collection.id)
                }
                combine(countFlows) { counts ->
                    collections.mapIndexed { index, collection ->
                        CollectionWithCount(collection, counts[index])
                    }
                }
            }
        },
        _dialogState
    ) { collectionsWithCounts, dialogState ->
        CollectionListUiState(
            collections = collectionsWithCounts,
            isLoading = false,
            showCreateDialog = dialogState.showCreateDialog,
            showDeleteDialog = dialogState.showDeleteDialog,
            error = dialogState.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionListUiState())

    fun showCreateDialog() {
        _dialogState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _dialogState.update { it.copy(showCreateDialog = false) }
    }

    fun createCollection(name: String, description: String?) {
        viewModelScope.launch {
            try {
                collectionRepository.createCollection(
                    CollectionEntity(
                        name = name,
                        description = description?.ifBlank { null }
                    )
                )
                _dialogState.update { it.copy(showCreateDialog = false) }
            } catch (e: Exception) {
                _dialogState.update { it.copy(error = "Failed to create: ${e.message}") }
            }
        }
    }

    fun showDeleteDialog(collection: CollectionEntity) {
        _dialogState.update { it.copy(showDeleteDialog = collection) }
    }

    fun hideDeleteDialog() {
        _dialogState.update { it.copy(showDeleteDialog = null) }
    }

    fun deleteCollection(collection: CollectionEntity) {
        viewModelScope.launch {
            try {
                collectionRepository.deleteCollection(collection)
                _dialogState.update { it.copy(showDeleteDialog = null) }
            } catch (e: Exception) {
                _dialogState.update { it.copy(showDeleteDialog = null, error = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _dialogState.update { it.copy(error = null) }
    }
}
