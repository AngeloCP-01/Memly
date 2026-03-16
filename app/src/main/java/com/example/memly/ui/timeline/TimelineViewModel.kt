package com.example.memly.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    val memories: StateFlow<List<MemoryWithDetails>> =
        memoryRepository.getAllMemoriesWithDetails()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}
