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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.File
import java.net.URL
import java.net.URLEncoder

data class NominatimResult(
    val displayName: String,
    val lat: Double,
    val lon: Double
)

data class CountryFilter(
    val flag: String,
    val name: String,
    val code: String, // ISO country code for Photon filtering
    val centerLat: Double,
    val centerLon: Double,
    val bbox: String // lon1,lat1,lon2,lat2 for Photon bbox param
)

val COUNTRY_FILTERS = listOf(
    CountryFilter("\uD83C\uDDF5\uD83C\uDDED", "Philippines", "PH", 12.8797, 121.7740, "116.95,4.59,126.60,21.12"),
    CountryFilter("\uD83C\uDDFA\uD83C\uDDF8", "United States", "US", 39.8283, -98.5795, "-125.0,24.0,-66.0,50.0"),
    CountryFilter("\uD83C\uDDEC\uD83C\uDDE7", "United Kingdom", "GB", 54.3781, -2.36, "-8.65,49.86,1.77,60.86"),
    CountryFilter("\uD83C\uDDEF\uD83C\uDDF5", "Japan", "JP", 36.2048, 138.2529, "122.93,24.04,153.99,45.55"),
    CountryFilter("\uD83C\uDDF0\uD83C\uDDF7", "South Korea", "KR", 35.9078, 127.7669, "124.60,33.10,131.87,38.63"),
    CountryFilter("\uD83C\uDDE8\uD83C\uDDE6", "Canada", "CA", 56.1304, -106.3468, "-141.0,41.68,-52.0,83.11"),
    CountryFilter("\uD83C\uDDE6\uD83C\uDDFA", "Australia", "AU", -25.2744, 133.7751, "113.34,-43.63,153.57,-10.67"),
    CountryFilter("\uD83C\uDDF8\uD83C\uDDEC", "Singapore", "SG", 1.3521, 103.8198, "103.60,1.15,104.09,1.47"),
    CountryFilter("\uD83C\uDDE9\uD83C\uDDEA", "Germany", "DE", 51.1657, 10.4515, "5.87,47.27,15.04,55.06"),
    CountryFilter("\uD83C\uDDEE\uD83C\uDDF3", "India", "IN", 20.5937, 78.9629, "68.18,6.75,97.40,35.50"),
    CountryFilter("\uD83C\uDDF2\uD83C\uDDFE", "Malaysia", "MY", 4.2105, 101.9758, "99.64,0.85,119.27,7.36"),
    CountryFilter("\uD83C\uDDE6\uD83C\uDDEA", "UAE", "AE", 23.4241, 53.8478, "51.58,22.63,56.38,26.08"),
    CountryFilter("\uD83C\uDDF3\uD83C\uDDFF", "New Zealand", "NZ", -40.9006, 174.886, "166.51,-47.29,178.52,-34.39"),
    CountryFilter("\uD83C\uDF0D", "Worldwide", "", 0.0, 0.0, "")
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

    var isDetectingLocation by remember { mutableStateOf(false) }
    var userLat by remember { mutableStateOf(initialLatitude) }
    var userLng by remember { mutableStateOf(initialLongitude) }
    var autocompleteJob by remember { mutableStateOf<Job?>(null) }
    var selectedCountry by remember { mutableStateOf(COUNTRY_FILTERS.first()) } // Philippines default
    var showCountryDropdown by remember { mutableStateOf(false) }

    // Configure osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
        }
    }

    // Auto-detect current location on open (only if no initial location provided)
    @Suppress("MissingPermission")
    LaunchedEffect(Unit) {
        if (initialLatitude != null && initialLongitude != null) return@LaunchedEffect

        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return@LaunchedEffect

        isDetectingLocation = true
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLat = location.latitude
                userLng = location.longitude
                mapViewRef?.controller?.animateTo(GeoPoint(location.latitude, location.longitude))
                mapViewRef?.controller?.setZoom(15.0)
                isDetectingLocation = false
            } else {
                val token = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
                    .addOnSuccessListener { fresh ->
                        if (fresh != null) {
                            userLat = fresh.latitude
                            userLng = fresh.longitude
                            mapViewRef?.controller?.animateTo(GeoPoint(fresh.latitude, fresh.longitude))
                            mapViewRef?.controller?.setZoom(15.0)
                        }
                        isDetectingLocation = false
                    }
                    .addOnFailureListener { isDetectingLocation = false }
            }
        }.addOnFailureListener { isDetectingLocation = false }
    }

    fun searchPlaces(query: String) {
        if (query.isBlank()) return
        scope.launch {
            isSearching = true
            showResults = true
            searchResults = withContext(Dispatchers.IO) {
                try {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val country = selectedCountry
                    val biasLat = userLat ?: country.centerLat
                    val biasLon = userLng ?: country.centerLon
                    val bboxParam = if (country.bbox.isNotEmpty()) "&bbox=${country.bbox}" else ""
                    val url = "https://photon.komoot.io/api/?q=$encoded&lat=$biasLat&lon=$biasLon&limit=15&lang=en$bboxParam"
                    val connection = URL(url).openConnection().apply {
                        setRequestProperty("User-Agent", context.packageName)
                        connectTimeout = 10_000
                        readTimeout = 10_000
                    }
                    val response = connection.getInputStream().use { it.bufferedReader().readText() }
                    val root = org.json.JSONObject(response)
                    val features = root.getJSONArray("features")
                    val countryCode = country.code
                    (0 until features.length()).mapNotNull { i ->
                        val feature = features.getJSONObject(i)
                        val props = feature.getJSONObject("properties")
                        // Filter by country code if a specific country is selected
                        if (countryCode.isNotEmpty()) {
                            val resultCountryCode = props.optString("countrycode", "")
                            if (!resultCountryCode.equals(countryCode, ignoreCase = true)) return@mapNotNull null
                        }
                        val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
                        val nameParts = mutableListOf<String>()
                        props.optString("name", "").takeIf { it.isNotEmpty() }?.let { nameParts.add(it) }
                        props.optString("street", "").takeIf { it.isNotEmpty() }?.let { nameParts.add(it) }
                        props.optString("city", "").takeIf { it.isNotEmpty() }?.let { nameParts.add(it) }
                        props.optString("state", "").takeIf { it.isNotEmpty() }?.let { nameParts.add(it) }
                        NominatimResult(
                            displayName = nameParts.joinToString(", ").ifEmpty { "Unknown location" },
                            lat = coords.getDouble(1), // GeoJSON: [lon, lat]
                            lon = coords.getDouble(0)
                        )
                    }.take(7)
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
                    val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&zoom=18&accept-language=en"
                    val connection = URL(url).openConnection().apply {
                        setRequestProperty("User-Agent", context.packageName)
                        connectTimeout = 10_000
                        readTimeout = 10_000
                    }
                    val response = connection.getInputStream().use { it.bufferedReader().readText() }
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
                            val lat = selectedLat ?: return@TopAppBar
                            val lng = selectedLng ?: return@TopAppBar
                            IconButton(onClick = {
                                onPlaceSelected(lat, lng, selectedPlaceName)
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Confirm")
                            }
                        }
                    },
                    windowInsets = WindowInsets(0)
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val defaultLat = initialLatitude ?: 12.8797
                val defaultLng = initialLongitude ?: 121.7740
                val defaultZoom = if (initialLatitude != null) 15.0 else 6.0

                // Map fills the entire area
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                            controller.setZoom(defaultZoom)
                            controller.setCenter(GeoPoint(defaultLat, defaultLng))

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

                            onResume()
                            mapViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Clean up MapView lifecycle
                DisposableEffect(Unit) {
                    onDispose {
                        mapViewRef?.onPause()
                        mapViewRef?.onDetach()
                        mapViewRef = null
                    }
                }

                // Search bar + results overlay at top
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { newValue ->
                                searchQuery = newValue
                                autocompleteJob?.cancel()
                                if (newValue.length >= 2) {
                                    autocompleteJob = scope.launch {
                                        delay(300)
                                        searchPlaces(newValue)
                                    }
                                } else {
                                    showResults = false
                                    searchResults = emptyList()
                                }
                            },
                            placeholder = { Text("Search for a place...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchPlaces(searchQuery) }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { searchPlaces(searchQuery) }),
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(Modifier.width(6.dp))

                        // Country flag dropdown
                        Box {
                            Surface(
                                onClick = { showCountryDropdown = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = selectedCountry.flag,
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showCountryDropdown,
                                onDismissRequest = { showCountryDropdown = false }
                            ) {
                                COUNTRY_FILTERS.forEach { country ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = country.flag,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = country.name,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedCountry = country
                                            showCountryDropdown = false
                                            // Re-trigger search with new country filter
                                            if (searchQuery.length >= 2) {
                                                autocompleteJob?.cancel()
                                                autocompleteJob = scope.launch {
                                                    searchPlaces(searchQuery)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Search results dropdown
                    if (showResults && searchResults.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .heightIn(max = 250.dp),
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 4.dp,
                            shadowElevation = 8.dp
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
                        Surface(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                "No results found",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Zoom + My Location buttons (stacked bottom-end)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 130.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Zoom in
                    androidx.compose.material3.SmallFloatingActionButton(
                        onClick = { mapViewRef?.controller?.zoomIn() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom in")
                    }

                    // Zoom out
                    androidx.compose.material3.SmallFloatingActionButton(
                        onClick = { mapViewRef?.controller?.zoomOut() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom out")
                    }

                    // My Location
                    @Suppress("MissingPermission")
                    androidx.compose.material3.SmallFloatingActionButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPerm) return@SmallFloatingActionButton
                            isDetectingLocation = true
                            val client = LocationServices.getFusedLocationProviderClient(context)
                            client.lastLocation.addOnSuccessListener { loc ->
                                if (loc != null) {
                                    userLat = loc.latitude
                                    userLng = loc.longitude
                                    mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                                    mapViewRef?.controller?.setZoom(16.0)
                                }
                                isDetectingLocation = false
                            }.addOnFailureListener { isDetectingLocation = false }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "My location")
                        }
                    }
                }

                // Bottom section: selected location + confirm button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    // Hint overlay
                    if (selectedLat == null && !showResults) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 8.dp),
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

                    // Confirm button
                    Button(
                        onClick = {
                            val lat = selectedLat ?: return@Button
                            val lng = selectedLng ?: return@Button
                            onPlaceSelected(lat, lng, selectedPlaceName)
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
    }

    // Add initial marker after map is ready
    LaunchedEffect(mapViewRef, initialLatitude, initialLongitude) {
        if (mapViewRef != null && initialLatitude != null && initialLongitude != null) {
            updateMapMarker(initialLatitude, initialLongitude)
        }
    }
}
