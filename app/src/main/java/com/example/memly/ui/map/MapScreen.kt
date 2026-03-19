package com.example.memly.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.memly.data.local.entity.MemoryWithDetails
import com.example.memly.data.local.entity.Mood
import com.example.memly.ui.theme.color
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(
    onMemoryClick: (Long) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Configure osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.memories.isEmpty() && uiState.moodFilter == null) {
            // Empty state — no geotagged memories at all
            MapEmptyState(modifier = Modifier.align(Alignment.Center))
        } else {
            // Map with pins
            OsmdroidMapView(
                memories = uiState.memories,
                selectedMemory = uiState.selectedMemory,
                onMarkerClick = { memory -> viewModel.selectMemory(memory) },
                onMapClick = { viewModel.selectMemory(null) }
            )

            // Filter chips overlay — top
            MoodFilterBar(
                selectedMood = uiState.moodFilter,
                onMoodSelected = { viewModel.setMoodFilter(it) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            )

            // Empty state for filtered results
            if (uiState.memories.isEmpty() && uiState.moodFilter != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                ) {
                    Text(
                        text = "No ${uiState.moodFilter!!.label.lowercase()} memories with locations",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Preview card — top
            AnimatedVisibility(
                visible = uiState.selectedMemory != null,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                uiState.selectedMemory?.let { memory ->
                    MemoryMapPreviewCard(
                        memoryWithDetails = memory,
                        onClick = { onMemoryClick(memory.memory.id) },
                        onDismiss = { viewModel.selectMemory(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OsmdroidMapView(
    memories: List<MemoryWithDetails>,
    selectedMemory: MemoryWithDetails?,
    onMarkerClick: (MemoryWithDetails) -> Unit,
    onMapClick: () -> Unit
) {
    val context = LocalContext.current
    var hasCentered by remember { mutableStateOf(false) }
    var lastMemoryIds by remember { mutableStateOf(emptySet<Long>()) }

    // Cache mood pin drawables by color
    val pinCache = remember { mutableMapOf<Int, BitmapDrawable>() }
    val density = context.resources.displayMetrics.density

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(0.0, 0.0))
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    AndroidView(
        factory = { mapView },
        update = { mv ->
            mv.overlays.clear()

            // Map tap to dismiss preview
            mv.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    onMapClick()
                    return true
                }
                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }))

            memories.forEach { memoryWithDetails ->
                val memory = memoryWithDetails.memory
                val lat = memory.latitude ?: return@forEach
                val lng = memory.longitude ?: return@forEach

                val marker = Marker(mv).apply {
                    position = GeoPoint(lat, lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = memory.title ?: "Memory"
                    snippet = memory.placeLabel

                    // Mood-colored pin (cached)
                    val moodColor = memory.mood?.color()?.toArgb()
                        ?: android.graphics.Color.parseColor("#FF6B6B")
                    icon = pinCache.getOrPut(moodColor) {
                        createMoodPin(moodColor, density, context.resources)
                    }

                    setOnMarkerClickListener { _, _ ->
                        onMarkerClick(memoryWithDetails)
                        true
                    }
                }
                mv.overlays.add(marker)
            }

            // Only re-center when memory set changes, not on every recomposition
            val currentIds = memories.map { it.memory.id }.toSet()
            if (!hasCentered || currentIds != lastMemoryIds) {
                hasCentered = true
                lastMemoryIds = currentIds

                if (memories.isNotEmpty()) {
                    val avgLat = memories.mapNotNull { it.memory.latitude }.average()
                    val avgLng = memories.mapNotNull { it.memory.longitude }.average()
                    mv.controller.setCenter(GeoPoint(avgLat, avgLng))
                    if (memories.size == 1) {
                        mv.controller.setZoom(15.0)
                    } else {
                        val lats = memories.mapNotNull { it.memory.latitude }
                        val lngs = memories.mapNotNull { it.memory.longitude }
                        val latSpan = (lats.max() - lats.min()).coerceAtLeast(0.01)
                        val lngSpan = (lngs.max() - lngs.min()).coerceAtLeast(0.01)
                        val zoom = when {
                            latSpan > 10 || lngSpan > 10 -> 4.0
                            latSpan > 5 || lngSpan > 5 -> 6.0
                            latSpan > 1 || lngSpan > 1 -> 8.0
                            latSpan > 0.1 || lngSpan > 0.1 -> 12.0
                            else -> 14.0
                        }
                        mv.controller.setZoom(zoom)
                    }
                }
            }

            mv.invalidate()
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun createMoodPin(
    color: Int,
    density: Float,
    resources: android.content.res.Resources
): BitmapDrawable {
    val size = (48 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Outer circle (mood color)
    paint.color = color
    paint.style = Paint.Style.FILL
    canvas.drawCircle(size / 2f, size / 2.5f, size / 3f, paint)

    // White inner dot
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2.5f, size / 8f, paint)

    // Triangle pointer at bottom
    paint.color = color
    val path = android.graphics.Path().apply {
        moveTo(size / 2f - size / 6f, size / 2f)
        lineTo(size / 2f + size / 6f, size / 2f)
        lineTo(size / 2f, size.toFloat() - 2f)
        close()
    }
    canvas.drawPath(path, paint)

    return BitmapDrawable(resources, bitmap)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodFilterBar(
    selectedMood: Mood?,
    onMoodSelected: (Mood?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        modifier = modifier.padding(horizontal = 12.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Mood.entries.forEach { mood ->
                FilterChip(
                    selected = selectedMood == mood,
                    onClick = {
                        onMoodSelected(if (selectedMood == mood) null else mood)
                    },
                    label = {
                        Text(
                            text = mood.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = mood.color().copy(alpha = 0.3f),
                        selectedLabelColor = mood.color()
                    )
                )
            }
        }
    }
}

@Composable
private fun MemoryMapPreviewCard(
    memoryWithDetails: MemoryWithDetails,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memory = memoryWithDetails.memory
    val thumbnail = memoryWithDetails.mediaFiles.firstOrNull { it.mediaType != com.example.memly.data.local.entity.MediaType.AUDIO }?.thumbnailPath

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mood accent strip (left edge)
            memory.mood?.let { mood ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(mood.color())
                )
                Spacer(Modifier.width(8.dp))
            }

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (thumbnail != null) {
                    AsyncImage(
                        model = File(thumbnail),
                        contentDescription = memory.title ?: "Memory",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    memory.mood?.let { mood ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(mood.color().copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mood.label.first().toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = mood.color()
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            // Title
            Text(
                text = memory.title ?: "Untitled Memory",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Dismiss
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MapEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No memories on the map yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Memories with location data will appear as pins here. Enable location when capturing a memory!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
