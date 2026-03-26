package com.example.memly.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.memly.data.local.entity.CollectionEntity
import com.example.memly.data.local.entity.MediaFileEntity
import com.example.memly.data.local.entity.MediaSource
import com.example.memly.data.local.entity.MediaType
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.Mood
import com.example.memly.data.local.entity.TagEntity
import com.example.memly.ui.components.AudioPlaybackBar
import com.example.memly.ui.components.MemlyToast
import com.example.memly.ui.components.PlacePickerDialog
import com.example.memly.ui.components.VideoPlayer
import com.example.memly.ui.theme.color
import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.platform.LocalContext
import com.example.memly.ui.capture.MediaItem
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var fullScreenMediaIndex by remember { mutableStateOf<Int?>(null) }

    // Photo picker for edit mode
    val editPhotoPickerLauncher = rememberLauncherForActivityResult(
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

    // Camera for edit mode
    var editCameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val editCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && editCameraPhotoUri != null) {
            viewModel.addCameraMedia(editCameraPhotoUri!!)
        }
    }

    // Video camera for edit mode
    var editCameraVideoUri by remember { mutableStateOf<Uri?>(null) }
    val editVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && editCameraVideoUri != null) {
            viewModel.addCameraMedia(editCameraVideoUri!!, MediaType.VIDEO)
        }
    }

    // Camera mode dialog for edit mode
    var showEditCameraModeDialog by remember { mutableStateOf(false) }

    // Place picker and date picker for edit mode
    var showEditPlacePicker by remember { mutableStateOf(false) }
    var showEditDatePicker by remember { mutableStateOf(false) }

    // Location permission for edit mode
    val editLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchEditLocation(context, viewModel)
        } else {
            Toast.makeText(context, "Location permission is required", Toast.LENGTH_SHORT).show()
            viewModel.setLocationLoading(false)
        }
    }

    val editCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showEditCameraModeDialog = true
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    // Media read permission for "Keep Original"
    val editMediaReadPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onImportChoiceMade(saveToMemly = false)
        } else {
            Toast.makeText(context, "Permission needed. Copying instead.", Toast.LENGTH_LONG).show()
            viewModel.onImportChoiceMade(saveToMemly = true)
        }
    }

    // Navigate back on delete
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onNavigateBack()
    }

    // Show errors
    LaunchedEffect(state.error) {
        state.error?.let {
            toastMessage = it
            viewModel.clearError()
        }
    }

    // Derive mood-based colors
    val moodColor = state.memory?.mood?.color()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val cardBgColor = if (moodColor != null) {
        // Blend mood tint over opaque surface
        androidx.compose.ui.graphics.lerp(surfaceColor, moodColor, 0.08f)
    } else {
        surfaceColor
    }
    val fabColor = moodColor ?: MaterialTheme.colorScheme.primary
    val fabContentColor = if (fabColor.luminance() > 0.5f) Color.Black else Color.White

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    // Auto-expand sheet when entering edit mode
    LaunchedEffect(state.isEditing) {
        if (state.isEditing) {
            sheetState.expand()
        }
    }

    when {
        state.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.memory == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Memory not found", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This memory may have been deleted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Go back")
                    }
                }
            }
        }
        else -> {
            val memory = state.memory!!
            val visualMedia = state.mediaFiles.filter { it.mediaType != MediaType.AUDIO }
            val audioMedia = state.mediaFiles.filter { it.mediaType == MediaType.AUDIO }
            val moodBgColor = memory.mood?.color()?.copy(alpha = 0.15f)
                ?: MaterialTheme.colorScheme.surfaceVariant

            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = 160.dp,
                sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                sheetContainerColor = cardBgColor,
                sheetDragHandle = {
                    // Drag handle pill
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 4.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                moodColor?.copy(alpha = 0.3f)
                                    ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                },
                sheetContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Audio playback section
                        if (audioMedia.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                audioMedia.forEach { audio ->
                                    AudioPlaybackBar(
                                        audioUri = Uri.parse(audio.mediaStoreUri),
                                        durationMs = audio.durationMs
                                    )
                                }
                            }
                        }

                        if (state.isEditing) {
                            EditModeContent(
                                state = state,
                                viewModel = viewModel,
                                onPickFromGallery = {
                                    editPhotoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                        )
                                    )
                                },
                                onTakePhoto = {
                                    editCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                onGetLocation = {
                                    viewModel.setLocationLoading(true)
                                    editLocationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onPickOnMap = { showEditPlacePicker = true },
                                onShowDatePicker = { showEditDatePicker = true }
                            )

                            // Save button
                            Button(
                                onClick = { viewModel.saveEdits() },
                                enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = fabColor,
                                    contentColor = fabContentColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = fabContentColor,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Saving...")
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Save Memory")
                                }
                            }
                        } else {
                            ReadModeContent(
                                memory = memory,
                                tags = state.tags,
                                dateFormat = dateFormat,
                                moodColor = moodColor,
                                onAddToCollection = { viewModel.showCollectionDialog() },
                                hasExternalMedia = state.mediaFiles.any { it.source == MediaSource.EXTERNAL },
                                onImportAll = {
                                    state.mediaFiles
                                        .filter { it.source == MediaSource.EXTERNAL }
                                        .forEach { viewModel.importToMemly(it) }
                                }
                            )

                            // Edit button
                            Button(
                                onClick = { viewModel.startEditing() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = fabColor,
                                    contentColor = fabContentColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Edit Memory")
                            }
                        }
                    }
                },
                sheetContentColor = Color.Unspecified
            ) {
                // Full-screen media behind the sheet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(moodBgColor)
                ) {
                    if (visualMedia.isNotEmpty()) {
                        PhotoHeroSection(
                            mediaFiles = visualMedia,
                            mood = memory.mood,
                            onBackClick = {
                                if (state.isEditing) viewModel.cancelEditing()
                                else onNavigateBack()
                            },
                            onDeleteClick = { showDeleteDialog = true },
                            isEditing = state.isEditing,
                            brokenMediaIds = state.brokenMediaIds,
                            onImportToMemly = { viewModel.importToMemly(it) },
                            onRemoveBroken = { viewModel.removeBrokenReference(it) },
                            onMediaClick = { index -> fullScreenMediaIndex = index }
                        )
                    } else {
                        // No media header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    if (state.isEditing) viewModel.cancelEditing()
                                    else onNavigateBack()
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    if (state.isEditing) Icons.Default.Close
                                    else Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (state.isEditing) {
                                IconButton(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // (Edit/Save button moved into bottom sheet)

                    MemlyToast(
                        message = toastMessage,
                        onDismiss = { toastMessage = null }
                    )
                }
            }

            // Full-screen media viewer overlay
            fullScreenMediaIndex?.let { startIndex ->
                FullScreenMediaViewer(
                    mediaFiles = visualMedia,
                    startIndex = startIndex,
                    onDismiss = { fullScreenMediaIndex = null }
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Memory?") },
            text = { Text("This memory and all its media will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteMemory()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add to collection dialog
    if (state.showCollectionDialog) {
        AddToCollectionDialog(
            collections = state.allCollections,
            memberCollectionIds = state.memberCollectionIds,
            onToggle = { viewModel.toggleCollection(it) },
            onDismiss = { viewModel.hideCollectionDialog() }
        )
    }

    // Import choice dialog (edit mode)
    if (state.showImportChoiceDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportChoice() },
            title = { Text("Save photos to Memly?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose how to store the selected media:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Save to Memly: Copies to Memly's folder. Survives even if you delete the original.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Keep original: References the file in its current location. No extra storage used.",
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
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, permission
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        viewModel.onImportChoiceMade(saveToMemly = false)
                    } else {
                        viewModel.hideImportChoiceDialog()
                        editMediaReadPermissionLauncher.launch(permission)
                    }
                }) {
                    Text("Keep original")
                }
            }
        )
    }

    // Camera mode dialog for edit mode (Photo vs Video)
    if (showEditCameraModeDialog) {
        AlertDialog(
            onDismissRequest = { showEditCameraModeDialog = false },
            title = { Text("Camera mode") },
            text = { Text("What would you like to capture?") },
            confirmButton = {
                TextButton(onClick = {
                    showEditCameraModeDialog = false
                    val cacheDir = File(context.cacheDir, "camera")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    val file = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                    editCameraPhotoUri = uri
                    editCameraLauncher.launch(uri)
                }) {
                    Text("Photo")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditCameraModeDialog = false
                    val cacheDir = File(context.cacheDir, "camera")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    val file = File(cacheDir, "video_${System.currentTimeMillis()}.mp4")
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                    editCameraVideoUri = uri
                    editVideoLauncher.launch(uri)
                }) {
                    Text("Video")
                }
            }
        )
    }

    // Date picker dialog for edit mode
    if (showEditDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.editDate)
        DatePickerDialog(
            onDismissRequest = { showEditDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateEditDate(it) }
                    showEditDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Place picker dialog for edit mode
    if (showEditPlacePicker) {
        PlacePickerDialog(
            initialLatitude = state.editLatitude,
            initialLongitude = state.editLongitude,
            onPlaceSelected = { lat, lng, placeName ->
                viewModel.updateEditLocation(lat, lng)
                if (!placeName.isNullOrBlank()) {
                    val shortName = placeName.split(",").firstOrNull()?.trim() ?: placeName
                    viewModel.updateEditPlaceLabel(shortName)
                }
                showEditPlacePicker = false
            },
            onDismiss = { showEditPlacePicker = false }
        )
    }
}

@Composable
private fun AddToCollectionDialog(
    collections: List<CollectionEntity>,
    memberCollectionIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Collection") },
        text = {
            if (collections.isEmpty()) {
                Text("No collections yet. Create one from the Collections screen.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Tap to toggle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    collections.forEach { collection ->
                        val isMember = collection.id in memberCollectionIds
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isMember) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isMember) 2.dp else 1.dp,
                                color = if (isMember) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(collection.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isMember,
                                    onCheckedChange = { onToggle(collection.id) },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = collection.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isMember) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    collection.description?.let { desc ->
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun PhotoHeroSection(
    mediaFiles: List<MediaFileEntity>,
    mood: Mood?,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isEditing: Boolean,
    brokenMediaIds: Set<Long> = emptySet(),
    onImportToMemly: (MediaFileEntity) -> Unit = {},
    onRemoveBroken: (MediaFileEntity) -> Unit = {},
    onMediaClick: (Int) -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { mediaFiles.size })

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, bottom = 200.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 12.dp
        ) { page ->
            val mediaFile = mediaFiles[page]
            val isBroken = mediaFile.id in brokenMediaIds

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (!isBroken && !isEditing) Modifier.clickable { onMediaClick(page) }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isBroken) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Original file removed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { onRemoveBroken(mediaFile) }) {
                                Text("Remove from memory")
                            }
                        }
                    }
                } else if (mediaFile.mediaType == MediaType.VIDEO) {
                    Box {
                        VideoPlayer(
                            videoUri = Uri.parse(mediaFile.mediaStoreUri),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                        )
                        // Invisible overlay to capture tap for full-screen
                        if (!isEditing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onMediaClick(page) }
                            )
                        }
                    }
                } else {
                    AsyncImage(
                        model = Uri.parse(mediaFile.mediaStoreUri),
                        contentDescription = "Media ${page + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }

        // Back button — top left
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                if (isEditing) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (isEditing) "Cancel edit" else "Back",
                tint = Color.White
            )
        }

        // Delete button — top right (only in edit mode)
        if (isEditing) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }

        // Mood chip — top right (read mode)
        if (!isEditing) {
            mood?.let { m ->
                Surface(
                    color = m.color(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        text = m.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Page indicator dots
        if (mediaFiles.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 172.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(mediaFiles.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage)
                                    mood?.color() ?: Color.White
                                else
                                    (mood?.color() ?: Color.White).copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadModeContent(
    memory: MemoryEntity,
    tags: List<TagEntity>,
    dateFormat: SimpleDateFormat,
    moodColor: Color?,
    onAddToCollection: () -> Unit,
    hasExternalMedia: Boolean = false,
    onImportAll: () -> Unit = {}
) {
    val accentColor = moodColor ?: MaterialTheme.colorScheme.primary

    // Title
    Text(
        text = memory.title ?: "Untitled Memory",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )

    // Date
    Text(
        text = dateFormat.format(Date(memory.memoryDate)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Mood chip
    memory.mood?.let { mood ->
        Surface(
            color = mood.color().copy(alpha = 0.2f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = mood.label,
                style = MaterialTheme.typography.labelMedium,
                color = mood.color(),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }

    // Location
    memory.placeLabel?.let { place ->
        Surface(
            color = accentColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = place,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Notes
    memory.notes?.let { notes ->
        if (notes.isNotBlank()) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    // Tags
    if (tags.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tags.forEach { tag ->
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "#${tag.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Import to Memly button
    if (hasExternalMedia) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = accentColor.copy(alpha = 0.12f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onImportAll)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Import to Memly",
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Add to collection button
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddToCollection)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CollectionsBookmark,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Add to collection",
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditModeContent(
    state: DetailUiState,
    viewModel: MemoryDetailViewModel,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onGetLocation: () -> Unit,
    onPickOnMap: () -> Unit,
    onShowDatePicker: () -> Unit
) {
    // Media editing section
    Text("Photos & Videos", style = MaterialTheme.typography.titleMedium)

    val existingVisual = state.mediaFiles.filter { it.mediaType != MediaType.AUDIO && it.id !in state.editRemovedMediaIds }
    val removedVisual = state.mediaFiles.filter { it.mediaType != MediaType.AUDIO && it.id in state.editRemovedMediaIds }
    val newVisual = state.editNewMediaItems.filter { it.mediaType != MediaType.AUDIO }

    // 3-column grid matching CaptureScreen style
    if (existingVisual.isNotEmpty() || newVisual.isNotEmpty()) {
        val columns = 3
        val totalItems = existingVisual.size + newVisual.size
        val rows = (totalItems + columns - 1) / columns

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0 until rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < totalItems) {
                            val isExisting = index < existingVisual.size
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        if (isExisting) MaterialTheme.colorScheme.outline
                                        else MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                if (isExisting) {
                                    val mediaFile = existingVisual[index]
                                    AsyncImage(
                                        model = Uri.parse(mediaFile.mediaStoreUri),
                                        contentDescription = "Media ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Video play icon
                                    if (mediaFile.mediaType == MediaType.VIDEO) {
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            contentDescription = "Video",
                                            modifier = Modifier.align(Alignment.Center).size(32.dp),
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                    // Order badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${index + 1}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    // Remove button
                                    IconButton(
                                        onClick = { viewModel.markMediaForRemoval(mediaFile.id) },
                                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier
                                                .size(18.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                    CircleShape
                                                ),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    val newIndex = index - existingVisual.size
                                    val item = newVisual[newIndex]
                                    val actualIndex = state.editNewMediaItems.indexOf(item)
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = "New media ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Video play icon
                                    if (item.mediaType == MediaType.VIDEO) {
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            contentDescription = "Video",
                                            modifier = Modifier.align(Alignment.Center).size(32.dp),
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                    // Order badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${index + 1}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    // Remove button
                                    IconButton(
                                        onClick = { viewModel.removeNewMedia(actualIndex) },
                                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier
                                                .size(18.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                    CircleShape
                                                ),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Removed media with undo
    if (removedVisual.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            removedVisual.forEach { mediaFile ->
                AssistChip(
                    onClick = { viewModel.unmarkMediaForRemoval(mediaFile.id) },
                    label = { Text("Undo remove") },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                    }
                )
            }
        }
    }

    // Action buttons row (matching CaptureScreen style)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable(onClick = onPickFromGallery)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = "Gallery",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text("Gallery", style = MaterialTheme.typography.labelSmall)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable(onClick = onTakePhoto)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "Camera",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text("Camera", style = MaterialTheme.typography.labelSmall)
        }
    }

    // Title
    OutlinedTextField(
        value = state.editTitle,
        onValueChange = viewModel::updateEditTitle,
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next
        )
    )

    // Notes
    OutlinedTextField(
        value = state.editNotes,
        onValueChange = viewModel::updateEditNotes,
        label = { Text("Notes") },
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        )
    )

    // Mood selector
    Text("Mood", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Mood.entries.forEach { mood ->
            FilterChip(
                selected = state.editMood == mood,
                onClick = { viewModel.selectEditMood(mood) },
                label = { Text(mood.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = mood.color().copy(alpha = 0.3f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }

    // === LOCATION ===
    Text("Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onGetLocation,
            label = {
                if (state.isLocationLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else if (state.editLatitude != null) {
                    Text("\uD83D\uDCCD ${String.format(java.util.Locale.ROOT, "%.4f, %.4f", state.editLatitude, state.editLongitude)}")
                } else {
                    Text("Get Location")
                }
            },
            leadingIcon = {
                if (!state.isLocationLoading && state.editLatitude == null) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                }
            }
        )
        AssistChip(
            onClick = onPickOnMap,
            label = { Text("Pick on Map") },
            leadingIcon = {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
            }
        )
        if (state.editLatitude != null) {
            IconButton(onClick = { viewModel.clearEditLocation() }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Clear location")
            }
        }
    }

    // Place label
    OutlinedTextField(
        value = state.editPlaceLabel,
        onValueChange = viewModel::updateEditPlaceLabel,
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
    Text("Tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    if (state.editTags.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            state.editTags.forEach { tag ->
                AssistChip(
                    onClick = { viewModel.removeEditTag(tag) },
                    label = { Text(tag) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remove tag", modifier = Modifier.size(16.dp))
                    }
                )
            }
        }
    }
    OutlinedTextField(
        value = state.editTagInput,
        onValueChange = viewModel::updateEditTagInput,
        label = { Text("Add a tag") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { viewModel.addEditTag() }),
        trailingIcon = {
            if (state.editTagInput.isNotBlank()) {
                IconButton(onClick = { viewModel.addEditTag() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add tag")
                }
            }
        }
    )

    // === DATE PICKER ===
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    Text("Date", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    AssistChip(
        onClick = onShowDatePicker,
        label = { Text(dateFormat.format(Date(state.editDate))) },
        leadingIcon = {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
        }
    )
}

@Suppress("MissingPermission")
@Composable
private fun FullScreenMediaViewer(
    mediaFiles: List<MediaFileEntity>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { mediaFiles.size }
    )
    var showControls by remember { mutableStateOf(true) }
    var isZoomed by remember { mutableStateOf(false) }

    // Reset zoom when page changes
    LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { mediaFiles[it].id },
            userScrollEnabled = !isZoomed
        ) { page ->
            val mediaFile = mediaFiles[page]

            if (mediaFile.mediaType == MediaType.VIDEO) {
                VideoPlayer(
                    videoUri = Uri.parse(mediaFile.mediaStoreUri),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Zoomable image
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                // Keep parent zoom state in sync
                LaunchedEffect(scale) {
                    isZoomed = scale > 1.01f
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showControls = !showControls },
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 3f
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val pointerCount = event.changes.size

                                    if (pointerCount >= 2) {
                                        // Pinch-to-zoom with 2+ fingers
                                        var zoom = 1f
                                        var pan = Offset.Zero
                                        event.changes.fastForEach { change ->
                                            if (change.positionChanged()) {
                                                pan += change.positionChange()
                                            }
                                        }
                                        // Calculate zoom from two primary pointers
                                        if (event.changes.size >= 2) {
                                            val current = event.changes[0].position - event.changes[1].position
                                            val previous = event.changes[0].previousPosition - event.changes[1].previousPosition
                                            val currentDist = current.getDistance()
                                            val previousDist = previous.getDistance()
                                            if (previousDist > 0f) {
                                                zoom = currentDist / previousDist
                                            }
                                        }
                                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                                        scale = newScale
                                        if (newScale > 1f) {
                                            offset = Offset(
                                                x = offset.x + pan.x / pointerCount,
                                                y = offset.y + pan.y / pointerCount
                                            )
                                        } else {
                                            offset = Offset.Zero
                                        }
                                        event.changes.fastForEach { it.consume() }
                                    } else if (scale > 1.01f) {
                                        // Single-finger pan only when zoomed
                                        val change = event.changes.firstOrNull()
                                        if (change != null && change.positionChanged()) {
                                            offset = Offset(
                                                x = offset.x + change.positionChange().x,
                                                y = offset.y + change.positionChange().y
                                            )
                                            change.consume()
                                        }
                                    }
                                    // When not zoomed + single finger: don't consume → pager swipes
                                } while (event.changes.fastAny { it.pressed })
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = Uri.parse(mediaFile.mediaStoreUri),
                        contentDescription = "Full screen media ${page + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Close button
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        // Page indicator
        if (mediaFiles.size > 1) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${mediaFiles.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun fetchEditLocation(context: android.content.Context, viewModel: MemoryDetailViewModel) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.lastLocation
        .addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
                viewModel.updateEditLocation(lastLocation.latitude, lastLocation.longitude)
            } else {
                val cancellation = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            viewModel.updateEditLocation(location.latitude, location.longitude)
                        } else {
                            viewModel.setLocationLoading(false)
                        }
                    }
                    .addOnFailureListener {
                        viewModel.setLocationLoading(false)
                    }
            }
        }
        .addOnFailureListener {
            viewModel.setLocationLoading(false)
        }
}
