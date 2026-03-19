package com.example.memly.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.net.URL
import java.net.URLEncoder

data class NominatimResult(
    val displayName: String,
    val lat: Double,
    val lon: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacePickerDialog(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onPlaceSelected: (lat: Double, lng: Double, placeName: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<NominatimResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    var selectedLat by remember { mutableStateOf(initialLatitude) }
    var selectedLng by remember { mutableStateOf(initialLongitude) }
    var selectedPlaceName by remember { mutableStateOf<String?>(null) }

    // Track the map view for programmatic updates
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    // Configure osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
        }
    }

    fun searchPlaces(query: String) {
        if (query.isBlank()) return
        scope.launch {
            isSearching = true
            showResults = true
            searchResults = withContext(Dispatchers.IO) {
                try {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5&addressdetails=0"
                    val connection = URL(url).openConnection()
                    connection.setRequestProperty("User-Agent", context.packageName)
                    val response = connection.getInputStream().bufferedReader().readText()
                    val array = JSONArray(response)
                    (0 until array.length()).map { i ->
                        val obj = array.getJSONObject(i)
                        NominatimResult(
                            displayName = obj.getString("display_name"),
                            lat = obj.getDouble("lat"),
                            lon = obj.getDouble("lon")
                        )
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            isSearching = false
        }
    }

    fun reverseGeocode(lat: Double, lon: Double) {
        scope.launch {
            selectedPlaceName = withContext(Dispatchers.IO) {
                try {
                    val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&zoom=18"
                    val connection = URL(url).openConnection()
                    connection.setRequestProperty("User-Agent", context.packageName)
                    val response = connection.getInputStream().bufferedReader().readText()
                    val obj = org.json.JSONObject(response)
                    obj.optString("display_name", null as String?)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    fun updateMapMarker(lat: Double, lon: Double) {
        mapViewRef?.let { mapView ->
            mapView.overlays.removeAll { it is Marker }
            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Selected location"
                // Create a simple pin drawable
                val size = 48
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
                canvas.drawCircle(size / 2f, size / 3f, size / 4f, paint)
                val path = android.graphics.Path().apply {
                    moveTo(size / 2f - size / 6f, size / 3f + size / 8f)
                    lineTo(size / 2f, size.toFloat() - 2f)
                    lineTo(size / 2f + size / 6f, size / 3f + size / 8f)
                    close()
                }
                canvas.drawPath(path, paint)
                val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                }
                canvas.drawCircle(size / 2f, size / 3f, size / 8f, whitePaint)
                icon = BitmapDrawable(mapView.resources, bmp)
            }
            mapView.overlays.add(marker)
            mapView.controller.animateTo(GeoPoint(lat, lon))
            mapView.invalidate()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pick a Location") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (selectedLat != null && selectedLng != null) {
                            IconButton(onClick = {
                                onPlaceSelected(selectedLat!!, selectedLng!!, selectedPlaceName)
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Confirm")
                            }
                        }
                    },
                    windowInsets = WindowInsets(0)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search for a place...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchPlaces(searchQuery) }),
                    shape = RoundedCornerShape(12.dp)
                )

                // Search results
                if (showResults && searchResults.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .heightIn(max = 200.dp),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp
                    ) {
                        LazyColumn {
                            items(searchResults) { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLat = result.lat
                                            selectedLng = result.lon
                                            selectedPlaceName = result.displayName
                                            showResults = false
                                            searchQuery = result.displayName.split(",").firstOrNull()?.trim() ?: ""
                                            updateMapMarker(result.lat, result.lon)
                                            mapViewRef?.controller?.setZoom(16.0)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        result.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (showResults && searchResults.isEmpty() && !isSearching) {
                    Text(
                        "No results found",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Selected location info
                if (selectedLat != null && selectedLng != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                selectedPlaceName
                                    ?: String.format(
                                        java.util.Locale.ROOT,
                                        "%.5f, %.5f",
                                        selectedLat,
                                        selectedLng
                                    ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Map
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val defaultLat = initialLatitude ?: 14.5995
                    val defaultLng = initialLongitude ?: 120.9842
                    val defaultZoom = if (initialLatitude != null) 15.0 else 5.0

                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(defaultZoom)
                                controller.setCenter(GeoPoint(defaultLat, defaultLng))

                                // Tap to select location
                                val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        selectedLat = p.latitude
                                        selectedLng = p.longitude
                                        selectedPlaceName = null
                                        reverseGeocode(p.latitude, p.longitude)
                                        updateMapMarker(p.latitude, p.longitude)
                                        return true
                                    }

                                    override fun longPressHelper(p: GeoPoint): Boolean = false
                                })
                                overlays.add(eventsOverlay)

                                // Show initial marker if location provided
                                if (initialLatitude != null && initialLongitude != null) {
                                    mapViewRef = this
                                    // Marker will be added after mapViewRef is set
                                }

                                mapViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Hint overlay
                    if (selectedLat == null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                "Tap on the map or search to select a location",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Confirm button
                Button(
                    onClick = {
                        if (selectedLat != null && selectedLng != null) {
                            onPlaceSelected(selectedLat!!, selectedLng!!, selectedPlaceName)
                        }
                    },
                    enabled = selectedLat != null && selectedLng != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm Location")
                }
            }
        }
    }

    // Add initial marker after map is ready
    LaunchedEffect(mapViewRef, initialLatitude, initialLongitude) {
        if (mapViewRef != null && initialLatitude != null && initialLongitude != null) {
            updateMapMarker(initialLatitude, initialLongitude)
        }
    }
}
