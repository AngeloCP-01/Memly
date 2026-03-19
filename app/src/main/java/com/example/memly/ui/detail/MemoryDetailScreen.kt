package com.example.memly.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.platform.LocalContext
import com.example.memly.ui.capture.MediaItem
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
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!state.isLoading && state.memory != null) {
                if (state.isEditing) {
                    FloatingActionButton(
                        onClick = { viewModel.saveEdits() },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                } else {
                    FloatingActionButton(
                        onClick = { viewModel.startEditing() },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.memory == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memory not found", style = MaterialTheme.typography.bodyLarge)
                }
            }
            else -> {
                val memory = state.memory ?: return@Scaffold
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Photo hero section (visual media only)
                    val visualMedia = state.mediaFiles.filter { it.mediaType != MediaType.AUDIO }
                    val audioMedia = state.mediaFiles.filter { it.mediaType == MediaType.AUDIO }

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
                        onRemoveBroken = { viewModel.removeBrokenReference(it) }
                    )

                    // Audio playback section
                    if (audioMedia.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            audioMedia.forEach { audio ->
                                AudioPlaybackBar(
                                    audioUri = Uri.parse(audio.mediaStoreUri),
                                    durationMs = audio.durationMs
                                )
                            }
                        }
                    }

                    // Metadata section
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (state.isEditing) {
                            // Edit mode
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
                                }
                            )
                        } else {
                            // Read mode
                            ReadModeContent(
                                memory = memory,
                                tags = state.tags,
                                dateFormat = dateFormat,
                                onAddToCollection = { viewModel.showCollectionDialog() },
                                hasExternalMedia = state.mediaFiles.any { it.source == MediaSource.EXTERNAL },
                                onImportAll = {
                                    state.mediaFiles
                                        .filter { it.source == MediaSource.EXTERNAL }
                                        .forEach { viewModel.importToMemly(it) }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(80.dp)) // FAB clearance
                }
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
    onRemoveBroken: (MediaFileEntity) -> Unit = {}
) {
    if (mediaFiles.isNotEmpty()) {
        val pagerState = rememberPagerState(pageCount = { mediaFiles.size })

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val mediaFile = mediaFiles[page]
                val isBroken = mediaFile.id in brokenMediaIds

                if (isBroken) {
                    // Broken reference placeholder
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
                    VideoPlayer(
                        videoUri = Uri.parse(mediaFile.mediaStoreUri),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = Uri.parse(mediaFile.mediaStoreUri),
                        contentDescription = "Media ${page + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
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
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(mediaFiles.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == pagerState.currentPage) Color.White
                                    else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }
    } else {
        // No media — compact header with back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    if (isEditing) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isEditing) "Cancel edit" else "Back"
                )
            }
            if (isEditing) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
    onAddToCollection: () -> Unit,
    hasExternalMedia: Boolean = false,
    onImportAll: () -> Unit = {}
) {
    // Title
    Text(
        text = memory.title ?: "Untitled Memory",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    // Date
    Text(
        text = dateFormat.format(Date(memory.memoryDate)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Mood (if no image to show mood chip on)
    memory.mood?.let { mood ->
        Surface(
            color = mood.color().copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = mood.label,
                style = MaterialTheme.typography.labelMedium,
                color = mood.color(),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }

    // Location
    memory.placeLabel?.let { place ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = place,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Notes
    memory.notes?.let { notes ->
        Text(
            text = notes,
            style = MaterialTheme.typography.bodyLarge
        )
    }

    // Tags
    if (tags.isNotEmpty()) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tags.forEach { tag ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "#${tag.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Import to Memly button (for external references)
    if (hasExternalMedia) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onImportAll)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Import to Memly",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }

    // Add to collection button
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddToCollection)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CollectionsBookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Add to collection",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
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
    onTakePhoto: () -> Unit
) {
    // Media editing section
    Text("Photos & Videos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

    val existingVisual = state.mediaFiles.filter { it.mediaType != MediaType.AUDIO }
    val newVisual = state.editNewMediaItems.filter { it.mediaType != MediaType.AUDIO }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Existing media with remove overlay
        itemsIndexed(existingVisual) { _, mediaFile ->
            val isMarkedForRemoval = mediaFile.id in state.editRemovedMediaIds
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (isMarkedForRemoval) 2.dp else 1.dp,
                        color = if (isMarkedForRemoval) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                AsyncImage(
                    model = Uri.parse(mediaFile.mediaStoreUri),
                    contentDescription = "Media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = if (isMarkedForRemoval) 0.3f else 1f
                )
                if (isMarkedForRemoval) {
                    // Undo removal
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        TextButton(onClick = { viewModel.unmarkMediaForRemoval(mediaFile.id) }) {
                            Text("Undo", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    // Remove button
                    IconButton(
                        onClick = { viewModel.markMediaForRemoval(mediaFile.id) },
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // New media items
        itemsIndexed(newVisual) { _, item ->
            val actualIndex = state.editNewMediaItems.indexOf(item)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = "New media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { viewModel.removeNewMedia(actualIndex) },
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Gallery button
        item {
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
                    Icons.Default.Add,
                    contentDescription = "Add from gallery",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text("Gallery", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Camera button
        item {
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
                    Icons.Default.Add,
                    contentDescription = "Take photo",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text("Camera", style = MaterialTheme.typography.labelSmall)
            }
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

    // Place label
    OutlinedTextField(
        value = state.editPlaceLabel,
        onValueChange = viewModel::updateEditPlaceLabel,
        label = { Text("Place name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next
        )
    )

    // Tags
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
}
