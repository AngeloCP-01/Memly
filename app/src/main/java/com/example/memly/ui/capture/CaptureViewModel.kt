package com.example.memly.ui.capture

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memly.data.local.entity.MediaFileEntity
import com.example.memly.data.local.entity.MediaType
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.Mood
import com.example.memly.data.repository.MemoryRepository
import com.example.memly.util.FileHashUtil
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
import java.util.UUID
import javax.inject.Inject

data class MediaItem(
    val uri: Uri,
    val mediaType: MediaType,
    val displayName: String = ""
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
    val isLocationLoading: Boolean = false
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun selectMood(mood: Mood?) {
        _uiState.update { it.copy(mood = if (it.mood == mood) null else mood) }
    }

    fun addMedia(uris: List<Uri>, mediaType: MediaType) {
        val newItems = uris.map { uri ->
            MediaItem(uri = uri, mediaType = mediaType)
        }
        _uiState.update { it.copy(mediaItems = it.mediaItems + newItems) }
    }

    fun addMedia(uri: Uri, mediaType: MediaType) {
        addMedia(listOf(uri), mediaType)
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
                val context = appContext
                val mediaDir = File(context.filesDir, "media")
                val thumbDir = File(context.cacheDir, "thumbnails")

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
                        processMediaItem(context, item, mediaDir, thumbDir)
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
        context: Context,
        item: MediaItem,
        mediaDir: File,
        thumbDir: File
    ): MediaFileEntity? = withContext(Dispatchers.IO) {
        // Compute hash for dedup
        val hash = context.contentResolver.openInputStream(item.uri)?.use { inputStream ->
            FileHashUtil.computeSha256(inputStream)
        } ?: return@withContext null

        // Check for duplicate
        val existing = memoryRepository.findMediaByHash(hash)
        if (existing != null) return@withContext null

        // Copy file to app-private storage
        if (!mediaDir.exists()) mediaDir.mkdirs()
        val extension = if (item.mediaType == MediaType.PHOTO) "jpg" else "mp4"
        val fileName = "${UUID.randomUUID()}.$extension"
        val destFile = File(mediaDir, fileName)

        context.contentResolver.openInputStream(item.uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Generate thumbnail
        val thumbnailFile = ThumbnailUtil.generateThumbnail(
            context = context,
            sourceUri = Uri.fromFile(destFile),
            mediaType = item.mediaType,
            outputDir = thumbDir,
            fileName = UUID.randomUUID().toString()
        )

        MediaFileEntity(
            memoryId = 0, // Will be set by repository in transaction
            filePath = destFile.absolutePath,
            thumbnailPath = thumbnailFile?.absolutePath,
            fileHash = hash,
            mediaType = item.mediaType
        )
    }
}
