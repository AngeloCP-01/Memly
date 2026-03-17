package com.example.memly.ui.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memly.data.local.entity.CollectionEntity
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionDetailUiState(
    val collection: CollectionEntity? = null,
    val memories: List<MemoryWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val collectionId: Long = savedStateHandle["collectionId"] ?: -1L

    private val _collection = MutableStateFlow<CollectionEntity?>(null)

    val uiState: StateFlow<CollectionDetailUiState> = combine(
        _collection,
        collectionRepository.getMemoriesInCollectionWithDetails(collectionId)
    ) { collection, memories ->
        CollectionDetailUiState(
            collection = collection,
            memories = memories,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionDetailUiState())

    init {
        loadCollection()
    }

    private fun loadCollection() {
        viewModelScope.launch {
            _collection.value = collectionRepository.getCollectionById(collectionId)
        }
    }

    fun removeMemoryFromCollection(memoryId: Long) {
        viewModelScope.launch {
            collectionRepository.removeMemoryFromCollection(memoryId, collectionId)
        }
    }
}
