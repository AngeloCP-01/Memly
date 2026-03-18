package com.example.memly.ui.capture

import android.content.Context
import android.net.Uri
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
    val recordingDurationMs: Long = 0
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val memoryRepository: MemoryRepository,
    private val mediaStoreManager: MediaStoreManager
) : ViewModel() {

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

    fun addCameraMedia(uri: Uri) {
        val item = MediaItem(uri = uri, mediaType = MediaType.PHOTO, isFromCamera = true)
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

    fun removeMedia(index: Int) {
        _uiState.update {
            it.copy(mediaItems = it.mediaItems.toMutableList().apply { removeAt(index) })
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
        if (state.title.isBlank() && state.notes.isBlank() && state.mediaItems.isEmpty()) {
            _uiState.update { it.copy(error = "Add a title, note, or photo to save a memory") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val thumbDir = File(appContext.cacheDir, "thumbnails")

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
                    state.mediaItems.mapNotNull { item ->
                        processMediaItem(item, thumbDir)
                    }
                }

                memoryRepository.createMemoryWithDetails(
                    memory = memoryEntity,
                    mediaFiles = mediaEntities,
                    tagNames = state.tags
                )

                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to save memory: ${e.message}")
                }
            }
        }
    }

    private suspend fun processMediaItem(
        item: MediaItem,
        thumbDir: File
    ): MediaFileEntity? = withContext(Dispatchers.IO) {
        val context = appContext

        // Compute hash for dedup
        val hash = context.contentResolver.openInputStream(item.uri)?.use { inputStream ->
            FileHashUtil.computeSha256(inputStream)
        } ?: return@withContext null

        // Check for duplicate
        val existing = memoryRepository.findMediaByHash(hash)
        if (existing != null) return@withContext null

        val mimeType = context.contentResolver.getType(item.uri) ?: when (item.mediaType) {
            MediaType.PHOTO -> "image/jpeg"
            MediaType.VIDEO -> "video/mp4"
            MediaType.AUDIO -> "audio/mp4"
        }

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
            }
            // Picked → user chose "Save to Memly" (IMPORTED)
            item.importChoice == ImportChoice.SAVE_TO_MEMLY -> {
                source = MediaSource.IMPORTED
                val metadata = mediaStoreManager.insertMedia(item.uri, item.mediaType, mimeType)
                    ?: return@withContext null
                finalUri = metadata.uri.toString()
                relativePath = metadata.relativePath
                displayName = metadata.displayName
                size = metadata.size
                dateTaken = metadata.dateTaken
                width = metadata.width
                height = metadata.height
            }
            // Picked → "Keep in original location" (EXTERNAL)
            else -> {
                source = MediaSource.EXTERNAL
                // Resolve to stable URI
                val (resolvedUri, wasResolved) = mediaStoreManager.resolveToMediaStoreUri(item.uri)
                if (!wasResolved) {
                    mediaStoreManager.takePersistablePermission(item.uri)
                }
                finalUri = resolvedUri.toString()

                // Query metadata from the source
                val metadata = mediaStoreManager.queryMetadata(resolvedUri, item.mediaType)
                if (metadata != null) {
                    displayName = metadata.displayName
                    size = metadata.size
                    dateTaken = metadata.dateTaken
                    width = metadata.width
                    height = metadata.height
                }
            }
        }

        // Generate thumbnail
        val thumbnailFile = ThumbnailUtil.generateThumbnail(
            context = context,
            sourceUri = Uri.parse(finalUri),
            mediaType = item.mediaType,
            outputDir = thumbDir,
            fileName = java.util.UUID.randomUUID().toString()
        )

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
