package com.example.memly.ui.timeline

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.local.entity.Mood
import com.example.memly.ui.theme.color
import kotlinx.coroutines.delay

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun TimelineScreen(
    onMemoryClick: (Long) -> Unit,
    onCaptureClick: () -> Unit = {},
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val isEmpty = state.allMemories.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        ProfileHeader()

        Spacer(Modifier.height(20.dp))

        SearchBarWithFilter(
            query = state.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            onClear = viewModel::clearSearch,
            moodFilters = state.moodFilters,
            dateFilter = state.dateFilter,
            onToggleMood = viewModel::toggleMoodFilter,
            onClearMoods = viewModel::clearMoodFilters,
            onDateSelected = viewModel::setDateFilter,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        // Active filter chips
        if (state.moodFilters.isNotEmpty() || state.dateFilter != null) {
            Spacer(Modifier.height(10.dp))
            ActiveFilterChips(
                moodFilters = state.moodFilters,
                dateFilter = state.dateFilter,
                onRemoveMood = viewModel::toggleMoodFilter,
                onClearDate = { viewModel.setDateFilter(null) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Memories section
        if (isEmpty) {
            EmptyTimelineState(onCaptureClick = onCaptureClick)
        } else if (state.displayMemories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No memories found",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Try a different search or filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = if (state.isFiltering) "${state.displayMemories.size} result${if (state.displayMemories.size != 1) "s" else ""}"
                else "Your Memories",
                style = if (state.isFiltering) MaterialTheme.typography.labelMedium
                else MaterialTheme.typography.titleLarge,
                fontWeight = if (state.isFiltering) FontWeight.Normal else FontWeight.Bold,
                color = if (state.isFiltering) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(16.dp))
            MemoryPager(
                memories = state.displayMemories,
                onMemoryClick = onMemoryClick
            )
        }
    }
}

@Composable
private fun ProfileHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(10.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hello, Catt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Welcome to your memories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = { /* no-op */ }) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarWithFilter(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    moodFilters: Set<Mood>,
    dateFilter: Long?,
    onToggleMood: (Mood) -> Unit,
    onClearMoods: () -> Unit,
    onDateSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    var showMoodSubmenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val hasActiveFilter = moodFilters.isNotEmpty() || dateFilter != null

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search field
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search memories...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Filter button
        Box {
            Surface(
                onClick = { showFilterMenu = true },
                shape = RoundedCornerShape(16.dp),
                color = if (hasActiveFilter) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = "Filter",
                        tint = if (hasActiveFilter) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Main filter menu
            DropdownMenu(
                expanded = showFilterMenu && !showMoodSubmenu,
                onDismissRequest = { showFilterMenu = false }
            ) {
                // Date filter
                Text(
                    text = "Filter by Date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (dateFilter != null) {
                                    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                    fmt.format(Date(dateFilter))
                                } else "Pick a date..."
                            )
                        }
                    },
                    onClick = {
                        showFilterMenu = false
                        showDatePicker = true
                    }
                )
                if (dateFilter != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Clear date filter",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            onDateSelected(null)
                            showFilterMenu = false
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Mood filter
                Text(
                    text = "Filter by Mood",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                if (moodFilters.isNotEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Clear all moods",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            onClearMoods()
                            showFilterMenu = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (moodFilters.isEmpty()) "Select moods..."
                                else "${moodFilters.size} selected"
                            )
                        }
                    },
                    onClick = { showMoodSubmenu = true }
                )
            }

            // Mood submenu — multi-select
            DropdownMenu(
                expanded = showMoodSubmenu,
                onDismissRequest = {
                    showMoodSubmenu = false
                    showFilterMenu = false
                }
            ) {
                Mood.entries.forEach { mood ->
                    val isSelected = mood in moodFilters
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(mood.color())
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = mood.label,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        onClick = { onToggleMood(mood) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Done button to close
                DropdownMenuItem(
                    text = {
                        Text(
                            "Done",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        showMoodSubmenu = false
                        showFilterMenu = false
                    }
                )
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateFilter
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ActiveFilterChips(
    moodFilters: Set<Mood>,
    dateFilter: Long?,
    onRemoveMood: (Mood) -> Unit,
    onClearDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date chip
        if (dateFilter != null) {
            val dateText = remember(dateFilter) {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(dateFilter))
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(
                        onClick = onClearDate,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove date filter",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Mood chips
        moodFilters.forEach { mood ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = mood.color().copy(alpha = 0.15f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(mood.color())
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = mood.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = mood.color()
                    )
                    IconButton(
                        onClick = { onRemoveMood(mood) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove ${mood.label} filter",
                            modifier = Modifier.size(14.dp),
                            tint = mood.color()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryPager(
    memories: List<MemoryWithDetails>,
    onMemoryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { memories.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 40.dp),
        pageSpacing = 12.dp,
        beyondViewportPageCount = 2,
        modifier = modifier.fillMaxWidth()
    ) { page ->
        val pageOffset = (
            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        )

        val isActive = page == pagerState.currentPage &&
            pagerState.currentPageOffsetFraction.absoluteValue < 0.3f

        MemoryPagerCard(
            memoryWithDetails = memories[page],
            isActive = isActive,
            onClick = { onMemoryClick(memories[page].memory.id) },
            modifier = Modifier
                .zIndex((10f - pageOffset.absoluteValue))
                .graphicsLayer {
                    val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)

                    val scale = lerp(0.85f, 1f, 1f - absOffset)
                    scaleX = scale
                    scaleY = scale

                    cameraDistance = 12f * density
                    rotationY = lerp(0f, -15f, pageOffset.coerceIn(-1f, 1f))

                    translationX = lerp(0f, 30f, pageOffset.coerceIn(-1f, 1f))

                    alpha = lerp(0.5f, 1f, 1f - absOffset)
                    shadowElevation = lerp(0f, 16f, 1f - absOffset)
                    shape = RoundedCornerShape(28.dp)
                    clip = true
                }
        )
    }
}

private const val SLIDESHOW_INTERVAL_MS = 4000L
private const val SLIDE_DURATION_MS = 600

@Composable
private fun MemoryPagerCard(
    memoryWithDetails: MemoryWithDetails,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memory = memoryWithDetails.memory
    val visualMediaFiles = remember(memoryWithDetails.mediaFiles) {
        memoryWithDetails.mediaFiles
            .filter { it.mediaType != com.example.memly.data.local.entity.MediaType.AUDIO }
    }
    val imageUris = remember(visualMediaFiles) { visualMediaFiles.map { it.mediaStoreUri } }
    val hasAudio = remember(memoryWithDetails.mediaFiles) {
        memoryWithDetails.mediaFiles.any { it.mediaType == com.example.memly.data.local.entity.MediaType.AUDIO }
    }
    val hasImages = imageUris.isNotEmpty()

    val slideshowState = rememberPagerState(pageCount = { imageUris.size.coerceAtLeast(1) })

    LaunchedEffect(isActive) {
        if (isActive && slideshowState.currentPage != 0) {
            slideshowState.scrollToPage(0)
        }
    }

    LaunchedEffect(isActive, imageUris.size) {
        if (isActive && imageUris.size > 1) {
            while (true) {
                delay(SLIDESHOW_INTERVAL_MS)
                val next = (slideshowState.currentPage + 1) % imageUris.size
                slideshowState.animateScrollToPage(
                    page = next,
                    animationSpec = tween(SLIDE_DURATION_MS)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(480.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
    ) {
        if (hasImages) {
            HorizontalPager(
                state = slideshowState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                val uri = imageUris.getOrElse(index) { imageUris.first() }
                val isVideo = visualMediaFiles.getOrNull(index)?.mediaType == com.example.memly.data.local.entity.MediaType.VIDEO
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = Uri.parse(uri),
                        contentDescription = memory.title ?: "Memory",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (isVideo) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = "Video",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        memory.mood?.let { mood ->
                            Brush.verticalGradient(
                                colors = listOf(
                                    mood.color().copy(alpha = 0.3f),
                                    mood.color().copy(alpha = 0.6f)
                                )
                            )
                        } ?: Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        if (hasImages && imageUris.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                imageUris.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == slideshowState.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == slideshowState.currentPage) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }

        // Audio indicator — below slideshow dots (top start area)
        if (hasAudio) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier
                    .padding(start = 16.dp, top = if (hasImages && imageUris.size > 1) 40.dp else 16.dp)
                    .align(Alignment.TopStart)
                    .size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Has voice memo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        memory.mood?.let { mood ->
            Surface(
                color = mood.color(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                Text(
                    text = mood.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = memory.title ?: "Untitled Memory",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                lineHeight = MaterialTheme.typography.headlineLarge.lineHeight,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = memory.placeLabel?.let { "@$it" } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(Modifier.width(12.dp))

                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "See more",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTimelineState(
    onCaptureClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
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
