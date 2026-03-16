package com.example.memly.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.ui.components.MemoryCard
import com.example.memly.ui.components.MemoryCarouselCard
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onMemoryClick: (Long) -> Unit,
    onCaptureClick: () -> Unit = {},
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val isEmpty = state.groups.isEmpty() && state.timeHopMemories.isEmpty()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { /* Room Flow auto-updates; no-op for now */ },
        modifier = Modifier.fillMaxSize()
    ) {
        if (isEmpty) {
            EmptyTimelineState(onCaptureClick = onCaptureClick)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Time Hop section
                if (state.timeHopMemories.isNotEmpty()) {
                    item(key = "time_hop_header") {
                        TimeHopSection(
                            memories = state.timeHopMemories,
                            onMemoryClick = onMemoryClick
                        )
                    }
                }

                // Grouped memories with sticky headers
                state.groups.forEach { group ->
                    stickyHeader(key = "header_${group.header}") {
                        SectionHeader(title = group.header)
                    }

                    items(
                        items = group.memories,
                        key = { it.memory.id }
                    ) { memoryWithDetails ->
                        MemoryCard(
                            memoryWithDetails = memoryWithDetails,
                            onClick = { onMemoryClick(memoryWithDetails.memory.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TimeHopSection(
    memories: List<MemoryWithDetails>,
    onMemoryClick: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = "On This Day",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(memories, key = { "timehop_${it.memory.id}" }) { memory ->
                val yearsAgo = remember(memory.memory.memoryDate) {
                    val memoryCal = Calendar.getInstance().apply { timeInMillis = memory.memory.memoryDate }
                    val nowCal = Calendar.getInstance()
                    nowCal.get(Calendar.YEAR) - memoryCal.get(Calendar.YEAR)
                }
                MemoryCarouselCard(
                    memoryWithDetails = memory,
                    yearsAgo = yearsAgo,
                    onClick = { onMemoryClick(memory.memory.id) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyTimelineState(
    onCaptureClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "No memories yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Capture your first memory to start building your timeline",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onCaptureClick) {
                Text("Create your first memory")
            }
        }
    }
}
