package com.example.memly.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.local.entity.Mood
import com.example.memly.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MapUiState(
    val memories: List<MemoryWithDetails> = emptyList(),
    val selectedMemory: MemoryWithDetails? = null,
    val moodFilter: Mood? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _moodFilter = MutableStateFlow<Mood?>(null)
    private val _selectedMemory = MutableStateFlow<MemoryWithDetails?>(null)

    val uiState: StateFlow<MapUiState> = combine(
        memoryRepository.getGeotaggedMemoriesWithDetails(),
        _moodFilter,
        _selectedMemory
    ) { memories, moodFilter, selected ->
        val filtered = if (moodFilter != null) {
            memories.filter { it.memory.mood == moodFilter }
        } else {
            memories
        }
        MapUiState(
            memories = filtered,
            selectedMemory = selected,
            moodFilter = moodFilter,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapUiState())

    fun selectMemory(memory: MemoryWithDetails?) {
        _selectedMemory.value = memory
    }

    fun setMoodFilter(mood: Mood?) {
        _moodFilter.value = mood
        _selectedMemory.value = null
    }
}
