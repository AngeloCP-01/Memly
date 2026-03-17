package com.example.memly.ui.timeline

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Calendar
import javax.inject.Inject

data class TimelineUiState(
    val allMemories: List<MemoryWithDetails> = emptyList(),
    val displayMemories: List<MemoryWithDetails> = emptyList(),
    val timeHopMemories: List<MemoryWithDetails> = emptyList(),
    val searchQuery: String = "",
    val moodFilters: Set<Mood> = emptySet(),
    val dateFilter: Long? = null,
    val isFiltering: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val todayMillis = System.currentTimeMillis()

    private val _searchQuery = MutableStateFlow("")
    private val _moodFilters = MutableStateFlow<Set<Mood>>(emptySet())
    private val _dateFilter = MutableStateFlow<Long?>(null)

    private val allMemoriesFlow = memoryRepository.getAllMemoriesWithDetails()

    private val debouncedQuery = _searchQuery.debounce(300)

    private val searchResults = debouncedQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            allMemoriesFlow
        } else {
            memoryRepository.searchMemoriesWithDetails(query)
        }
    }

    private val timeHopMemories = memoryRepository.getTimeHopMemories(todayMillis)

    val uiState: StateFlow<TimelineUiState> = combine(
        allMemoriesFlow,
        timeHopMemories,
        _searchQuery,
        combine(searchResults, _moodFilters, _dateFilter) { results, moods, date ->
            Triple(results, moods, date)
        }
    ) { all, timeHop, query, (results, moods, date) ->
        var filtered = results

        if (moods.isNotEmpty()) {
            filtered = filtered.filter { it.memory.mood in moods }
        }

        if (date != null) {
            val dayCal = Calendar.getInstance().apply { timeInMillis = date }
            filtered = filtered.filter { memory ->
                val memCal = Calendar.getInstance().apply { timeInMillis = memory.memory.memoryDate }
                isSameDay(memCal, dayCal)
            }
        }

        val isFiltering = query.isNotBlank() || moods.isNotEmpty() || date != null

        TimelineUiState(
            allMemories = all,
            displayMemories = filtered,
            timeHopMemories = timeHop,
            searchQuery = query,
            moodFilters = moods,
            dateFilter = date,
            isFiltering = isFiltering
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimelineUiState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun toggleMoodFilter(mood: Mood) {
        _moodFilters.update { current ->
            if (mood in current) current - mood else current + mood
        }
    }

    fun clearMoodFilters() {
        _moodFilters.value = emptySet()
    }

    fun setDateFilter(millis: Long?) {
        _dateFilter.value = millis
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
