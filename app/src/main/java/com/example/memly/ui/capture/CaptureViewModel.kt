package com.example.memly.ui.capture

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memly.data.local.entity.MediaFileEntity
import com.example.memly.data.local.entity.MediaSource
import com.example.memly.data.local.entity.MediaType
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.Mood
import com.example.memly.data.repository.MemoryRepository
import com.example.memly.util.AudioRecorder
import com.example.memly.util.FileHashUtil
import com.example.memly.util.MediaStoreManager
import com.example.memly.util.ThumbnailUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class ImportChoice { SAVE_TO_MEMLY, KEEP_ORIGINAL }

data class MediaItem(
    val uri: Uri,
    val mediaType: MediaType,
    val isFromCamera: Boolean = false,
    val importChoice: ImportChoice? = null
)

data class SaveProgress(
    val current: Int,
    val total: Int,
    val step: String
)

data class CaptureUiState(
    val title: String = "",
    val notes: String = "",
    val mood: Mood? = null,
    val mediaItems: List<MediaItem> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeLabel: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val memoryDate: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isLocationLoading: Boolean = false,
    val showImportChoiceDialog: Boolean = false,
    val pendingPickedUris: List<Pair<Uri, MediaType>> = emptyList(),
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0,
    val saveProgress: SaveProgress? = null,
    val selectedForSwap: Int? = null
) {
    val canSave: Boolean
        get() = title.isNotBlank() || notes.isNotBlank() || mediaItems.isNotEmpty()
}

@HiltViewModel
class CaptureViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val memoryRepository: MemoryRepository,
    private val mediaStoreManager: MediaStoreManager
) : ViewModel() {
    companion object {
        private const val TAG = "MemlyCapture"
    }

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val audioRecorder = AudioRecorder(appContext)
    private var recordingTimerJob: kotlinx.coroutines.Job? = null

    fun startRecording() {
        val uri = audioRecorder.start()
        if (uri != null) {
            _uiState.update { it.copy(isRecording = true, recordingDurationMs = 0) }
            recordingTimerJob = viewModelScope.launch {
                while (true) {
                    kotlinx.coroutines.delay(100)
                    _uiState.update { it.copy(recordingDurationMs = it.recordingDurationMs + 100) }
                }
            }
        } else {
            _uiState.update { it.copy(error = "Failed to start recording") }
        }
    }

    fun stopRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        val uri = audioRecorder.stop()
        if (uri != null) {
            val item = MediaItem(uri = uri, mediaType = MediaType.AUDIO, isFromCamera = true)
            _uiState.update {
                it.copy(
                    isRecording = false,
                    recordingDurationMs = 0,
                    mediaItems = it.mediaItems + item
                )
            }
        } else {
            _uiState.update { it.copy(isRecording = false, recordingDurationMs = 0, error = "Recording failed") }
        }
    }

    fun cancelRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        audioRecorder.cancel()
        _uiState.update { it.copy(isRecording = false, recordingDurationMs = 0) }
    }

    override fun onCleared() {
        super.onCleared()
        if (audioRecorder.isRecording) audioRecorder.cancel()
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun selectMood(mood: Mood?) {
        _uiState.update { it.copy(mood = if (it.mood == mood) null else mood) }
    }

    fun addCameraMedia(uri: Uri, mediaType: MediaType = MediaType.PHOTO) {
        val item = MediaItem(uri = uri, mediaType = mediaType, isFromCamera = true)
        _uiState.update { it.copy(mediaItems = it.mediaItems + item) }
    }

    fun addPickedMedia(uris: List<Pair<Uri, MediaType>>) {
        // Show import choice dialog
        _uiState.update {
            it.copy(
                showImportChoiceDialog = true,
                pendingPickedUris = uris
            )
        }
    }

    fun onImportChoiceMade(saveToMemly: Boolean) {
        val pending = _uiState.value.pendingPickedUris
        val choice = if (saveToMemly) ImportChoice.SAVE_TO_MEMLY else ImportChoice.KEEP_ORIGINAL
        val items = pending.map { (uri, type) ->
            MediaItem(
                uri = uri,
                mediaType = type,
                isFromCamera = false,
                importChoice = choice
            )
        }
        _uiState.update {
            it.copy(
                mediaItems = it.mediaItems + items,
                showImportChoiceDialog = false,
                pendingPickedUris = emptyList()
            )
        }
    }

    fun dismissImportChoice() {
        _uiState.update {
            it.copy(showImportChoiceDialog = false, pendingPickedUris = emptyList())
        }
    }

    fun hideImportChoiceDialog() {
        _uiState.update {
            it.copy(showImportChoiceDialog = false)
        }
    }

    fun removeMedia(index: Int) {
        _uiState.update {
            it.copy(
                mediaItems = it.mediaItems.toMutableList().apply { removeAt(index) },
                selectedForSwap = null
            )
        }
    }

    fun toggleSelectForSwap(index: Int) {
        _uiState.update { state ->
            val current = state.selectedForSwap
            when {
                current == null -> state.copy(selectedForSwap = index)
                current == index -> state.copy(selectedForSwap = null)
                else -> {
                    // Swap the two items
                    val items = state.mediaItems.toMutableList()
                    val temp = items[current]
                    items[current] = items[index]
                    items[index] = temp
                    state.copy(mediaItems = items, selectedForSwap = null)
                }
            }
        }
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(latitude = latitude, longitude = longitude, isLocationLoading = false)
        }
    }

    fun setLocationLoading(loading: Boolean) {
        _uiState.update { it.copy(isLocationLoading = loading) }
    }

    fun clearLocation() {
        _uiState.update { it.copy(latitude = null, longitude = null) }
    }

    fun updatePlaceLabel(label: String) {
        _uiState.update { it.copy(placeLabel = label) }
    }

    fun updateTagInput(input: String) {
        _uiState.update { it.copy(tagInput = input) }
    }

    fun addTag() {
        val tag = _uiState.value.tagInput.trim()
        if (tag.isNotEmpty() && tag !in _uiState.value.tags) {
            _uiState.update { it.copy(tags = it.tags + tag, tagInput = "") }
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag) }
    }

    fun updateMemoryDate(date: Long) {
        _uiState.update { it.copy(memoryDate = date) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun saveMemory() {
        val state = _uiState.value
        if (!state.canSave) {
            _uiState.update { it.copy(error = "Add a title, note, or photo to save a memory") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, saveProgress = null) }
            try {
                val thumbDir = File(appContext.cacheDir, "thumbnails")
                val totalItems = state.mediaItems.size

                val memoryEntity = MemoryEntity(
                    title = state.title.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    mood = state.mood,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    placeLabel = state.placeLabel.ifBlank { null },
                    memoryDate = state.memoryDate
                )

                val mediaEntities = withContext(Dispatchers.IO) {
                    state.mediaItems.mapIndexedNotNull { index, item ->
                        _uiState.update {
                            it.copy(saveProgress = SaveProgress(
                                current = index + 1,
                                total = totalItems,
                                step = "Processing media ${index + 1} of $totalItems…"
                            ))
                        }
                        processMediaItem(item, thumbDir)?.copy(sortOrder = index)
                    }
                }

                _uiState.update {
                    it.copy(saveProgress = SaveProgress(
                        current = totalItems,
                        total = totalItems,
                        step = "Saving memory…"
                    ))
                }

                memoryRepository.createMemoryWithDetails(
                    memory = memoryEntity,
                    mediaFiles = mediaEntities,
                    tagNames = state.tags
                )

                _uiState.update { it.copy(isLoading = false, isSaved = true, saveProgress = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, saveProgress = null, error = "Failed to save memory: ${e.message}")
                }
            }
        }
    }

    private suspend fun processMediaItem(
        item: MediaItem,
        thumbDir: File
    ): MediaFileEntity? = withContext(Dispatchers.IO) {
        val context = appContext

        Log.d(TAG, "━━━ processMediaItem START ━━━")
        Log.d(TAG, "  originalUri=${item.uri}")
        Log.d(TAG, "  mediaType=${item.mediaType}")
        Log.d(TAG, "  isFromCamera=${item.isFromCamera}")
        Log.d(TAG, "  importChoice=${item.importChoice}")

        // Compute hash for dedup
        val hash = context.contentResolver.openInputStream(item.uri)?.use { inputStream ->
            FileHashUtil.computeSha256(inputStream)
        } ?: run {
            Log.e(TAG, "  FAILED: could not open input stream for hash")
            return@withContext null
        }

        // Check for duplicate — if the same file already exists in another memory,
        // reuse its stored URI/metadata instead of copying again.
        // This avoids duplicate files on disk while allowing the same photo in multiple memories.
        val existing = memoryRepository.findMediaByHash(hash)
        if (existing != null) {
            Log.d(TAG, "  REUSE: duplicate hash=$hash, existingId=${existing.id}, source=${existing.source}")
            // Reuse the existing file reference — no new copy needed
            return@withContext MediaFileEntity(
                memoryId = 0,
                mediaStoreUri = existing.mediaStoreUri,
                thumbnailPath = existing.thumbnailPath,
                fileHash = hash,
                mediaType = existing.mediaType,
                source = existing.source,
                relativePath = existing.relativePath,
                displayName = existing.displayName,
                mimeType = existing.mimeType,
                size = existing.size,
                dateTaken = existing.dateTaken,
                width = existing.width,
                height = existing.height,
                durationMs = existing.durationMs
            )
        }

        val mimeType = context.contentResolver.getType(item.uri) ?: when (item.mediaType) {
            MediaType.PHOTO -> "image/jpeg"
            MediaType.VIDEO -> "video/mp4"
            MediaType.AUDIO -> "audio/mp4"
        }
        Log.d(TAG, "  mimeType=$mimeType")

        val source: MediaSource
        val finalUri: String
        var relativePath: String? = null
        var displayName: String? = null
        var size: Long = 0
        var dateTaken: Long? = null
        var width: Int? = null
        var height: Int? = null
        var durationMs: Long? = null

        when {
            // Camera photo / voice memo → save to public storage (APP_OWNED)
            item.isFromCamera -> {
                source = MediaSource.APP_OWNED
                val metadata = mediaStoreManager.insertMedia(item.uri, item.mediaType, mimeType)
                    ?: return@withContext null
                finalUri = metadata.uri.toString()
                relativePath = metadata.relativePath
                displayName = metadata.displayName
                size = metadata.size
                dateTaken = metadata.dateTaken
                width = metadata.width
                height = metadata.height
                durationMs = metadata.durationMs
                Log.d(TAG, "  APP_OWNED → finalUri=$finalUri")
            }
            // Picked → user chose "Save to Memly" (IMPORTED)
            item.importChoice == ImportChoice.SAVE_TO_MEMLY -> {
                source = MediaSource.IMPORTED
                val metadata = mediaStoreManager.insertMedia(item.uri, item.mediaType, mimeType)
                    ?: run {
                        Log.e(TAG, "  FAILED: insertMedia returned null for IMPORTED")
                        return@withContext null
                    }
                finalUri = metadata.uri.toString()
                relativePath = metadata.relativePath
                displayName = metadata.displayName
                size = metadata.size
                dateTaken = metadata.dateTaken
                width = metadata.width
                height = metadata.height
                Log.d(TAG, "  IMPORTED → finalUri=$finalUri")
            }
            // Picked → "Keep in original location" (EXTERNAL)
            else -> {
                source = MediaSource.EXTERNAL
                Log.d(TAG, "  EXTERNAL path — resolving picker URI...")
                // Resolve to stable MediaStore URI (requires READ_MEDIA_IMAGES permission)
                val (resolvedUri, wasResolved) = mediaStoreManager.resolveToMediaStoreUri(item.uri)
                Log.d(TAG, "  resolvedUri=$resolvedUri, wasResolved=$wasResolved")
                if (!wasResolved) {
                    val permResult = mediaStoreManager.takePersistablePermission(item.uri)
                    Log.d(TAG, "  takePersistablePermission=$permResult")
                }
                finalUri = resolvedUri.toString()

                // Query metadata from the source
                val metadata = mediaStoreManager.queryMetadata(resolvedUri, item.mediaType)
                Log.d(TAG, "  metadata=${metadata != null}, displayName=${metadata?.displayName}, size=${metadata?.size}")
                if (metadata != null) {
                    displayName = metadata.displayName
                    size = metadata.size
                    dateTaken = metadata.dateTaken
                    width = metadata.width
                    height = metadata.height
                }
            }
        }

        // Verify the final URI is actually readable
        Log.d(TAG, "  ── READABILITY CHECK ──")
        Log.d(TAG, "  finalUri=$finalUri, source=$source")
        try {
            val testUri = Uri.parse(finalUri)
            context.contentResolver.openInputStream(testUri)?.use { stream ->
                val available = stream.available()
                Log.d(TAG, "  ✓ URI readable, available=$available bytes")
            } ?: Log.e(TAG, "  ✗ openInputStream returned null for $finalUri")
        } catch (e: SecurityException) {
            Log.e(TAG, "  ✗ SecurityException reading $finalUri", e)
        } catch (e: java.io.FileNotFoundException) {
            Log.e(TAG, "  ✗ FileNotFoundException reading $finalUri", e)
        } catch (e: Exception) {
            Log.e(TAG, "  ✗ Exception reading $finalUri", e)
        }

        // Generate thumbnail
        val thumbnailFile = ThumbnailUtil.generateThumbnail(
            context = context,
            sourceUri = Uri.parse(finalUri),
            mediaType = item.mediaType,
            outputDir = thumbDir,
            fileName = java.util.UUID.randomUUID().toString()
        )
        Log.d(TAG, "  thumbnail=${thumbnailFile?.absolutePath}")
        Log.d(TAG, "━━━ processMediaItem END ━━━")

        MediaFileEntity(
            memoryId = 0, // Will be set by repository in transaction
            mediaStoreUri = finalUri,
            thumbnailPath = thumbnailFile?.absolutePath,
            fileHash = hash,
            mediaType = item.mediaType,
            source = source,
            relativePath = relativePath,
            displayName = displayName,
            mimeType = mimeType,
            size = size,
            dateTaken = dateTaken,
            width = width,
            height = height,
            durationMs = durationMs
        )
    }
}
