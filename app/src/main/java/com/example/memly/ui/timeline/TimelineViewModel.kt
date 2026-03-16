package com.example.memly.ui.timeline

import androidx.lifecycle.ViewModel
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    val memories: Flow<List<MemoryWithDetails>> =
        memoryRepository.getAllMemoriesWithDetails()
}
