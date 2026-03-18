package com.example.memly.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.memly.data.local.entity.MediaType
import com.example.memly.data.local.entity.Mood
import com.example.memly.ui.components.AudioPlaybackBar
import com.example.memly.ui.components.formatDuration
import com.example.memly.ui.theme.color
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaptureScreen(
    onMemorySaved: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Navigate back when saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onMemorySaved()
    }

    // Show error in snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val items = uris.map { uri ->
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val type = if (mimeType.startsWith("video")) MediaType.VIDEO else MediaType.PHOTO
                uri to type
            }
            viewModel.addPickedMedia(items)
        }
    }

    // Camera photo URI
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri != null) {
            viewModel.addCameraMedia(cameraPhotoUri!!)
        }
    }

    // Camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val cacheDir = File(context.cacheDir, "camera")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    // Location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchLocation(context, viewModel)
        } else {
            Toast.makeText(context, "Location permission is required", Toast.LENGTH_SHORT).show()
            viewModel.setLocationLoading(false)
        }
    }

    // Audio recording permission
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required to record", Toast.LENGTH_SHORT).show()
        }
    }

    // Media read permission (needed for "Keep Original" to resolve stable MediaStore URIs)
    val mediaReadPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onImportChoiceMade(saveToMemly = false)
        } else {
            Toast.makeText(context, "Permission needed to reference original files. Copying instead.", Toast.LENGTH_LONG).show()
            viewModel.onImportChoiceMade(saveToMemly = true)
        }
    }

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Memory") },
                navigationIcon = {
                    IconButton(onClick = onMemorySaved) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // === MEDIA SECTION ===
                Text("Photos & Videos", style = MaterialTheme.typography.titleMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val visualItems = state.mediaItems.filter { it.mediaType != MediaType.AUDIO }
                    itemsIndexed(visualItems) { _, item ->
                        val index = state.mediaItems.indexOf(item)
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = "Media",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.removeMedia(index) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    item {
                        // Gallery button
                        MediaActionButton(
                            icon = Icons.Default.PhotoLibrary,
                            label = "Gallery",
                            onClick = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                    )
                                )
                            }
                        )
                    }
                    item {
                        // Camera button
                        MediaActionButton(
                            icon = Icons.Default.CameraAlt,
                            label = "Camera",
                            onClick = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }
                }

                // === VOICE MEMO SECTION ===
                Text("Voice Memo", style = MaterialTheme.typography.titleMedium)
                val audioItems = state.mediaItems.filter { it.mediaType == MediaType.AUDIO }

                if (state.isRecording) {
                    // Recording in progress
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Recording...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    formatDuration(state.recordingDurationMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(onClick = { viewModel.cancelRecording() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            IconButton(onClick = { viewModel.stopRecording() }) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "Stop recording",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else if (audioItems.isEmpty()) {
                    // Record button
                    Button(
                        onClick = {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Record Voice Memo")
                    }
                }

                // Show recorded audio items with playback + remove
                audioItems.forEachIndexed { _, item ->
                    val index = state.mediaItems.indexOf(item)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AudioPlaybackBar(
                            audioUri = item.uri,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.removeMedia(index) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // === TEXT INPUT ===
                Text("Details", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    supportingText = { Text("${state.title.length}/100") }
                )
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::updateNotes,
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    supportingText = { Text("${state.notes.length}/1000") }
                )

                // === MOOD SELECTOR ===
                Text("How are you feeling?", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Mood.entries.forEach { mood ->
                        FilterChip(
                            selected = state.mood == mood,
                            onClick = { viewModel.selectMood(mood) },
                            label = { Text(mood.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = mood.color().copy(alpha = 0.3f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                // === LOCATION ===
                Text("Location", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            viewModel.setLocationLoading(true)
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        label = {
                            if (state.isLocationLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else if (state.latitude != null) {
                                Text("📍 ${String.format(java.util.Locale.ROOT, "%.4f, %.4f", state.latitude, state.longitude)}")
                            } else {
                                Text("Get Location")
                            }
                        },
                        leadingIcon = {
                            if (!state.isLocationLoading && state.latitude == null) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                            }
                        }
                    )
                    if (state.latitude != null) {
                        IconButton(onClick = { viewModel.clearLocation() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear location")
                        }
                    }
                }
                OutlinedTextField(
                    value = state.placeLabel,
                    onValueChange = viewModel::updatePlaceLabel,
                    label = { Text("Place name (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                // === TAGS ===
                Text("Tags", style = MaterialTheme.typography.titleMedium)
                if (state.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.tags.forEach { tag ->
                            AssistChip(
                                onClick = { viewModel.removeTag(tag) },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove tag", modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = state.tagInput,
                    onValueChange = viewModel::updateTagInput,
                    label = { Text("Add a tag") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.addTag() }),
                    trailingIcon = {
                        if (state.tagInput.isNotBlank()) {
                            IconButton(onClick = { viewModel.addTag() }) {
                                Icon(Icons.Default.Add, contentDescription = "Add tag")
                            }
                        }
                    }
                )

                // === DATE PICKER ===
                Text("Date", style = MaterialTheme.typography.titleMedium)
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(dateFormat.format(Date(state.memoryDate))) },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                    }
                )

                // === SAVE BUTTON ===
                Button(
                    onClick = { viewModel.saveMemory() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Text("Save Memory", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.memoryDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateMemoryDate(it) }
                    showDatePicker = false
                }) {
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

    // Import choice dialog
    if (state.showImportChoiceDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportChoice() },
            title = { Text("Save photos to Memly?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose how to store the selected media:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Save to Memly: Copies to Memly's folder. Survives even if you delete the original.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Keep original: References the file in its current location. No extra storage used, but the photo may disappear from Memly if you delete the original.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onImportChoiceMade(saveToMemly = true) }) {
                    Text("Save to Memly")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Need READ_MEDIA_IMAGES (API 33+) or READ_EXTERNAL_STORAGE to resolve
                    // picker URIs to stable MediaStore URIs for "Keep Original"
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, permission
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        viewModel.onImportChoiceMade(saveToMemly = false)
                    } else {
                        // Hide dialog but keep pending URIs for the permission callback
                        viewModel.hideImportChoiceDialog()
                        mediaReadPermissionLauncher.launch(permission)
                    }
                }) {
                    Text("Keep original")
                }
            }
        )
    }
}

@Composable
private fun MediaActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Suppress("MissingPermission")
private fun fetchLocation(context: android.content.Context, viewModel: CaptureViewModel) {
    val client = LocationServices.getFusedLocationProviderClient(context)

    // Try last known location first (fast, works indoors)
    client.lastLocation
        .addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
                viewModel.updateLocation(lastLocation.latitude, lastLocation.longitude)
            } else {
                // No cached location — request a fresh one with balanced priority
                requestFreshLocation(client, context, viewModel)
            }
        }
        .addOnFailureListener {
            // lastLocation failed — try fresh request anyway
            requestFreshLocation(client, context, viewModel)
        }
}

@Suppress("MissingPermission")
private fun requestFreshLocation(
    client: com.google.android.gms.location.FusedLocationProviderClient,
    context: android.content.Context,
    viewModel: CaptureViewModel
) {
    val cancellation = CancellationTokenSource()
    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
        .addOnSuccessListener { location ->
            if (location != null) {
                viewModel.updateLocation(location.latitude, location.longitude)
            } else {
                viewModel.setLocationLoading(false)
                Toast.makeText(
                    context,
                    "Could not get location. Make sure Location is enabled in Settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        .addOnFailureListener {
            viewModel.setLocationLoading(false)
            Toast.makeText(context, "Location error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
}
