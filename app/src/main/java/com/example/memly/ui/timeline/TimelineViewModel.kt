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
    val isFiltering: Boolean = false,
    val hasMoreMemories: Boolean = false
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
    private val _visibleCount = MutableStateFlow(PAGE_SIZE)

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
        },
        _visibleCount
    ) { all, timeHop, query, (results, moods, date), visibleCount ->
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
        val paged = filtered.take(visibleCount)

        TimelineUiState(
            allMemories = all,
            displayMemories = paged,
            timeHopMemories = timeHop,
            searchQuery = query,
            moodFilters = moods,
            dateFilter = date,
            isFiltering = isFiltering,
            hasMoreMemories = filtered.size > visibleCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimelineUiState()
    )

    fun loadMore() {
        _visibleCount.update { it + PAGE_SIZE }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _visibleCount.value = PAGE_SIZE
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _visibleCount.value = PAGE_SIZE
    }

    fun toggleMoodFilter(mood: Mood) {
        _moodFilters.update { current ->
            if (mood in current) current - mood else current + mood
        }
        _visibleCount.value = PAGE_SIZE
    }

    fun clearMoodFilters() {
        _moodFilters.value = emptySet()
        _visibleCount.value = PAGE_SIZE
    }

    fun setDateFilter(millis: Long?) {
        _dateFilter.value = millis
        _visibleCount.value = PAGE_SIZE
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    companion object {
        private const val PAGE_SIZE = 10
    }
}
