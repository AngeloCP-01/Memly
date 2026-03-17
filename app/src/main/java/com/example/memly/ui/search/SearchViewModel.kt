package com.example.memly.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.local.entity.Mood
import com.example.memly.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val moodFilter: Mood? = null,
    val results: List<MemoryWithDetails> = emptyList(),
    val resultCount: Int = 0,
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _moodFilter = MutableStateFlow<Mood?>(null)

    private val debouncedQuery = _query.debounce(300)

    private val searchResults = debouncedQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf(emptyList())
        } else {
            memoryRepository.searchMemoriesWithDetails(query)
        }
    }

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _moodFilter,
        searchResults
    ) { query, moodFilter, results ->
        val filtered = if (moodFilter != null) {
            results.filter { it.memory.mood == moodFilter }
        } else {
            results
        }
        SearchUiState(
            query = query,
            moodFilter = moodFilter,
            results = filtered,
            resultCount = filtered.size,
            hasSearched = query.isNotBlank()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun setMoodFilter(mood: Mood?) {
        _moodFilter.value = mood
    }

    fun clearSearch() {
        _query.value = ""
        _moodFilter.value = null
    }
}
