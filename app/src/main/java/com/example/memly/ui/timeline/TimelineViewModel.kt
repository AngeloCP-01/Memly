package com.example.memly.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TimelineGroup(
    val header: String,
    val memories: List<MemoryWithDetails>
)

data class TimelineUiState(
    val groups: List<TimelineGroup> = emptyList(),
    val allMemories: List<MemoryWithDetails> = emptyList(),
    val timeHopMemories: List<MemoryWithDetails> = emptyList(),
    val isRefreshing: Boolean = false
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val todayMillis = System.currentTimeMillis()

    private val allMemoriesFlow = memoryRepository.getAllMemoriesWithDetails()

    private val groupedMemories = allMemoriesFlow
        .map { memories -> groupByDate(memories) }

    private val timeHopMemories = memoryRepository.getTimeHopMemories(todayMillis)

    val uiState: StateFlow<TimelineUiState> = combine(
        allMemoriesFlow,
        groupedMemories,
        timeHopMemories
    ) { all, groups, timeHop ->
        TimelineUiState(
            groups = groups,
            allMemories = all,
            timeHopMemories = timeHop
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimelineUiState()
    )

    private fun groupByDate(memories: List<MemoryWithDetails>): List<TimelineGroup> {
        if (memories.isEmpty()) return emptyList()

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val weekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

        return memories.groupBy { memory ->
            val cal = Calendar.getInstance().apply { timeInMillis = memory.memory.memoryDate }
            when {
                isSameDay(cal, today) -> "Today"
                isSameDay(cal, yesterday) -> "Yesterday"
                cal.after(weekAgo) -> dayFormat.format(Date(memory.memory.memoryDate))
                else -> dateFormat.format(Date(memory.memory.memoryDate))
            }
        }.map { (header, memories) ->
            TimelineGroup(header = header, memories = memories)
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
