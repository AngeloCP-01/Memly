package com.example.memly.ui.timeline

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.memly.ui.theme.color
import kotlinx.coroutines.delay
import java.io.File
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
        // -- Profile Header --
        ProfileHeader()

        Spacer(Modifier.height(20.dp))

        // -- Search Bar --
        SearchBar(
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(16.dp))

        // -- Filter Chips --
        FilterChips(
            modifier = Modifier.padding(start = 20.dp)
        )

        Spacer(Modifier.height(24.dp))

        // -- Memories Section --
        if (isEmpty) {
            EmptyTimelineState(onCaptureClick = onCaptureClick)
        } else {
            Text(
                text = "Your Memories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(16.dp))
            MemoryPager(
                memories = state.allMemories,
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
        // Avatar
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

        // Greeting text
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

        // Notification bell
        IconButton(onClick = { /* no-op */ }) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SearchBar(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
            .fillMaxWidth()
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
            Text(
                text = "Search",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilterChips(modifier: Modifier = Modifier) {
    val filters = listOf("All", "Date", "Mood")

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 20.dp)
    ) {
        items(filters) { label ->
            val isSelected = label == "All"
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                else Color.Transparent,
                modifier = Modifier
                    .then(
                        if (!isSelected) Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(24.dp)
                        ) else Modifier
                    )
                    .clickable { /* no-op for now */ }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
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

    // Coverflow-style pager: centered card, side cards rotated and peeking
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

                    // Scale down side cards
                    val scale = lerp(0.85f, 1f, 1f - absOffset)
                    scaleX = scale
                    scaleY = scale

                    // Rotate side cards on Y-axis (3D perspective tilt)
                    cameraDistance = 12f * density
                    rotationY = lerp(0f, -15f, pageOffset.coerceIn(-1f, 1f))

                    // Push side cards further out
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
    val imagePaths = remember(memoryWithDetails.mediaFiles) {
        memoryWithDetails.mediaFiles.mapNotNull { it.thumbnailPath }
    }
    val hasImages = imagePaths.isNotEmpty()

    // Inner pager state for sliding between images
    val slideshowState = rememberPagerState(pageCount = { imagePaths.size.coerceAtLeast(1) })

    // Reset to first image when this card becomes active
    LaunchedEffect(isActive) {
        if (isActive && slideshowState.currentPage != 0) {
            slideshowState.scrollToPage(0)
        }
    }

    // Auto-advance timer — only runs when active and has multiple images
    LaunchedEffect(isActive, imagePaths.size) {
        if (isActive && imagePaths.size > 1) {
            while (true) {
                delay(SLIDESHOW_INTERVAL_MS)
                val next = (slideshowState.currentPage + 1) % imagePaths.size
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
        // Background image slideshow or fallback
        if (hasImages) {
            HorizontalPager(
                state = slideshowState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                val path = imagePaths.getOrElse(index) { imagePaths.first() }
                AsyncImage(
                    model = File(path),
                    contentDescription = memory.title ?: "Memory",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
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

        // Gradient overlay at bottom — taller and softer
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

        // Slideshow indicator dots — top left
        if (hasImages && imagePaths.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                imagePaths.indices.forEach { index ->
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

        // Mood chip — top right
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

        // Bottom content: large title, then @location + "See more +" row
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            // Large display title
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

            // @location (left) + "See more +" button (right) — same row
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

                // "See more +" pill button
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
