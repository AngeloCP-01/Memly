package com.example.memly.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.example.memly.data.local.entity.MediaType
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.ui.theme.color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Timeline Memory Card — image-dominant with gradient overlay.
 * 3:4 portrait aspect ratio, mood chip top-right, title/date/place over gradient.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryCard(
    memoryWithDetails: MemoryWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memory = memoryWithDetails.memory
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val visualMedia = memoryWithDetails.mediaFiles.firstOrNull { it.mediaType != MediaType.AUDIO }
    val fullResUri = visualMedia?.mediaStoreUri
    val hasAudio = memoryWithDetails.mediaFiles.any { it.mediaType == MediaType.AUDIO }

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "cardScale"
    )

    val context = LocalContext.current

    // Check if the URI is actually readable (catches dead picker URIs, deleted files, etc.)
    var isUriReadable by remember(fullResUri) { mutableStateOf(true) } // optimistic default
    LaunchedEffect(fullResUri) {
        if (fullResUri != null) {
            isUriReadable = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(fullResUri))
                        ?.use { true } ?: false
                } catch (_: Exception) {
                    false
                }
            }
        }
    }

    Column(modifier = modifier) {
        if (fullResUri != null) {
            // Image-dominant card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(16.dp))
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
            ) {
                if (isUriReadable) {
                    AsyncImage(
                        model = Uri.parse(fullResUri),
                        contentDescription = memory.title ?: "Memory",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Broken / inaccessible image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.BrokenImage,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Image unavailable",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Gradient overlay — bottom third
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

                // Mood chip — top right
                memory.mood?.let { mood ->
                    Surface(
                        color = mood.color(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = mood.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Audio indicator — top left
                if (hasAudio) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(12.dp)
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

                // Text over gradient
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = memory.title ?: "Untitled Memory",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateFormat.format(Date(memory.memoryDate)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        memory.placeLabel?.let { place ->
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = place,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            // No-image fallback card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
            ) {
                Column {
                    // Mood accent strip
                    memory.mood?.let { mood ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(mood.color())
                        )
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        // Audio indicator for no-image card
                        if (hasAudio) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Has voice memo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Voice memo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = memory.title ?: "Untitled Memory",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            memory.mood?.let { mood ->
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = mood.color().copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = mood.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = mood.color(),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dateFormat.format(Date(memory.memoryDate)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            memory.placeLabel?.let { place ->
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = place,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        memory.notes?.let { notes ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Tag chips — below image area
        if (memoryWithDetails.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val visibleTags = memoryWithDetails.tags.take(3)
                val overflow = memoryWithDetails.tags.size - 3
                visibleTags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "#${tag.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (overflow > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "+$overflow",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Carousel variant — compact card for "Time Hop" horizontal sections.
 * 180dp wide, 4:5 aspect ratio, simplified overlay (title + date only).
 */
@Composable
fun MemoryCarouselCard(
    memoryWithDetails: MemoryWithDetails,
    yearsAgo: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memory = memoryWithDetails.memory
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val thumbnail = memoryWithDetails.mediaFiles.firstOrNull { it.mediaType != MediaType.AUDIO }?.thumbnailPath

    Box(
        modifier = modifier
            .width(180.dp)
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        if (thumbnail != null) {
            AsyncImage(
                model = File(thumbnail),
                contentDescription = memory.title ?: "Memory",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
        )

        // "X years ago" label — top left
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                text = if (yearsAgo == 1) "1 year ago" else "$yearsAgo years ago",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Title + date
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = memory.title ?: "Untitled",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateFormat.format(Date(memory.memoryDate)),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Search result variant — horizontal layout with square thumbnail + text.
 * Used in search results and collection screens.
 */
@Composable
fun MemorySearchResultCard(
    memoryWithDetails: MemoryWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memory = memoryWithDetails.memory
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val thumbnail = memoryWithDetails.mediaFiles.firstOrNull { it.mediaType != MediaType.AUDIO }?.thumbnailPath
    val hasAudio = memoryWithDetails.mediaFiles.any { it.mediaType == MediaType.AUDIO }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (thumbnail != null) {
                    AsyncImage(
                        model = File(thumbnail),
                        contentDescription = memory.title ?: "Memory",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (hasAudio) {
                    // Audio-only: show mic icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice memo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    // Mood color accent for no-image
                    memory.mood?.let { mood ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(mood.color().copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mood.label.first().toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = mood.color()
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memory.title ?: "Untitled Memory",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(Date(memory.memoryDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                memory.placeLabel?.let { place ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = place,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                memory.mood?.let { mood ->
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = mood.color().copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = mood.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = mood.color(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
