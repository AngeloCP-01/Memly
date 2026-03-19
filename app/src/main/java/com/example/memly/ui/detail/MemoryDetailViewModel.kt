package com.example.memly.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.memly.data.local.entity.CollectionEntity
import com.example.memly.data.local.entity.MediaFileEntity
import com.example.memly.data.local.entity.MediaSource
import com.example.memly.data.local.entity.MediaType
import com.example.memly.data.local.entity.MemoryEntity
import com.example.memly.data.local.entity.Mood
import com.example.memly.data.local.entity.TagEntity
import com.example.memly.data.repository.CollectionRepository
import com.example.memly.data.repository.MemoryRepository
import com.example.memly.ui.capture.ImportChoice
import com.example.memly.ui.capture.MediaItem
import com.example.memly.util.FileHashUtil
import com.example.memly.util.MediaStoreManager
import com.example.memly.util.ThumbnailUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class DetailUiState(
    val memory: MemoryEntity? = null,
    val mediaFiles: List<MediaFileEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    // Edit fields
    val editTitle: String = "",
    val editNotes: String = "",
    val editMood: Mood? = null,
    val editPlaceLabel: String = "",
    val editTags: List<String> = emptyList(),
    val editTagInput: String = "",
    // Edit media
    val editRemovedMediaIds: Set<Long> = emptySet(),
    val editNewMediaItems: List<MediaItem> = emptyList(),
    val showImportChoiceDialog: Boolean = false,
    val pendingPickedUris: List<Pair<Uri, MediaType>> = emptyList(),
    // Collections
    val showCollectionDialog: Boolean = false,
    val allCollections: List<CollectionEntity> = emptyList(),
    val memberCollectionIds: Set<Long> = emptySet(),
    // Broken references
    val brokenMediaIds: Set<Long> = emptySet()
)

@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val memoryRepository: MemoryRepository,
    private val collectionRepository: CollectionRepository,
    private val mediaStoreManager: MediaStoreManager
) : ViewModel() {
    companion object {
        private const val TAG = "MemlyDetail"
    }

    private val memoryId: Long = savedStateHandle["memoryId"] ?: -1L

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadMemory()
    }

    private fun loadMemory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val details = memoryRepository.getMemoryWithDetails(memoryId)
                if (details != null) {
                    Log.d(TAG, "━━━ loadMemory id=$memoryId ━━━")
                    Log.d(TAG, "  mediaFiles count=${details.mediaFiles.size}")
                    for (mf in details.mediaFiles) {
                        Log.d(TAG, "  ── media id=${mf.id} ──")
                        Log.d(TAG, "    source=${mf.source}")
                        Log.d(TAG, "    mediaStoreUri=${mf.mediaStoreUri}")
                        Log.d(TAG, "    mimeType=${mf.mimeType}")
                        Log.d(TAG, "    thumbnailPath=${mf.thumbnailPath}")
                        // Readability test
                        try {
                            val uri = Uri.parse(mf.mediaStoreUri)
                            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                                Log.d(TAG, "    ✓ readable, available=${stream.available()}")
                            } ?: Log.e(TAG, "    ✗ openInputStream returned null")
                        } catch (e: SecurityException) {
                            Log.e(TAG, "    ✗ SecurityException: ${e.message}")
                        } catch (e: java.io.FileNotFoundException) {
                            Log.e(TAG, "    ✗ FileNotFoundException: ${e.message}")
                        } catch (e: Exception) {
                            Log.e(TAG, "    ✗ Exception: ${e.message}")
                        }
                    }

                    // Check ALL media files for broken/inaccessible URIs (IO-bound)
                    val brokenIds = withContext(Dispatchers.IO) {
                        details.mediaFiles
                            .filter { !mediaStoreManager.isUriAccessible(Uri.parse(it.mediaStoreUri)) }
                            .map { it.id }
                            .toSet()
                    }
                    Log.d(TAG, "  brokenIds=$brokenIds")

                    _uiState.update {
                        it.copy(
                            memory = details.memory,
                            mediaFiles = details.mediaFiles,
                            tags = details.tags,
                            isLoading = false,
                            brokenMediaIds = brokenIds
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Memory not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load: ${e.message}") }
            }
        }
    }

    fun startEditing() {
        val state = _uiState.value
        val memory = state.memory ?: return
        _uiState.update {
            it.copy(
                isEditing = true,
                editTitle = memory.title ?: "",
                editNotes = memory.notes ?: "",
                editMood = memory.mood,
                editPlaceLabel = memory.placeLabel ?: "",
                editTags = state.tags.map { tag -> tag.name },
                editTagInput = "",
                editRemovedMediaIds = emptySet(),
                editNewMediaItems = emptyList()
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                isEditing = false,
                editRemovedMediaIds = emptySet(),
                editNewMediaItems = emptyList(),
                showImportChoiceDialog = false,
                pendingPickedUris = emptyList()
            )
        }
    }

    fun updateEditTitle(title: String) {
        _uiState.update { it.copy(editTitle = title) }
    }

    fun updateEditNotes(notes: String) {
        _uiState.update { it.copy(editNotes = notes) }
    }

    fun selectEditMood(mood: Mood?) {
        _uiState.update { it.copy(editMood = if (it.editMood == mood) null else mood) }
    }

    fun updateEditPlaceLabel(label: String) {
        _uiState.update { it.copy(editPlaceLabel = label) }
    }

    fun updateEditTagInput(input: String) {
        _uiState.update { it.copy(editTagInput = input) }
    }

    fun addEditTag() {
        val tag = _uiState.value.editTagInput.trim()
        if (tag.isNotEmpty() && tag !in _uiState.value.editTags) {
            _uiState.update { it.copy(editTags = it.editTags + tag, editTagInput = "") }
        }
    }

    fun removeEditTag(tag: String) {
        _uiState.update { it.copy(editTags = it.editTags - tag) }
    }

    // --- Media editing methods ---

    fun markMediaForRemoval(mediaId: Long) {
        _uiState.update { it.copy(editRemovedMediaIds = it.editRemovedMediaIds + mediaId) }
    }

    fun unmarkMediaForRemoval(mediaId: Long) {
        _uiState.update { it.copy(editRemovedMediaIds = it.editRemovedMediaIds - mediaId) }
    }

    fun addPickedMedia(uris: List<Pair<Uri, MediaType>>) {
        _uiState.update {
            it.copy(showImportChoiceDialog = true, pendingPickedUris = uris)
        }
    }

    fun onImportChoiceMade(saveToMemly: Boolean) {
        val pending = _uiState.value.pendingPickedUris
        val choice = if (saveToMemly) ImportChoice.SAVE_TO_MEMLY else ImportChoice.KEEP_ORIGINAL
        val items = pending.map { (uri, type) ->
            MediaItem(uri = uri, mediaType = type, isFromCamera = false, importChoice = choice)
        }
        _uiState.update {
            it.copy(
                editNewMediaItems = it.editNewMediaItems + items,
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
        _uiState.update { it.copy(showImportChoiceDialog = false) }
    }

    fun addCameraMedia(uri: Uri, mediaType: MediaType = MediaType.PHOTO) {
        val item = MediaItem(uri = uri, mediaType = mediaType, isFromCamera = true)
        _uiState.update { it.copy(editNewMediaItems = it.editNewMediaItems + item) }
    }

    fun removeNewMedia(index: Int) {
        _uiState.update {
            it.copy(editNewMediaItems = it.editNewMediaItems.toMutableList().apply { removeAt(index) })
        }
    }

    private suspend fun processMediaItem(
        item: MediaItem,
        thumbDir: File
    ): MediaFileEntity? = withContext(Dispatchers.IO) {
        val context = appContext

        val hash = context.contentResolver.openInputStream(item.uri)?.use { inputStream ->
            FileHashUtil.computeSha256(inputStream)
        } ?: return@withContext null

        val existing = memoryRepository.findMediaByHash(hash)
        if (existing != null) {
            return@withContext MediaFileEntity(
                memoryId = memoryId,
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
            else -> {
                source = MediaSource.EXTERNAL
                val (resolvedUri, wasResolved) = mediaStoreManager.resolveToMediaStoreUri(item.uri)
                if (!wasResolved) {
                    mediaStoreManager.takePersistablePermission(item.uri)
                }
                finalUri = resolvedUri.toString()
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

        val thumbnailFile = ThumbnailUtil.generateThumbnail(
            context = context,
            sourceUri = Uri.parse(finalUri),
            mediaType = item.mediaType,
            outputDir = thumbDir,
            fileName = java.util.UUID.randomUUID().toString()
        )

        MediaFileEntity(
            memoryId = memoryId,
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

    fun saveEdits() {
        val state = _uiState.value
        val memory = state.memory ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val updatedMemory = memory.copy(
                    title = state.editTitle.ifBlank { null },
                    notes = state.editNotes.ifBlank { null },
                    mood = state.editMood,
                    placeLabel = state.editPlaceLabel.ifBlank { null }
                )
                memoryRepository.updateMemory(updatedMemory)

                // Update tags: remove old, add new
                val oldTagNames = state.tags.map { it.name }.toSet()
                val newTagNames = state.editTags.toSet()

                for (tag in state.tags) {
                    if (tag.name !in newTagNames) {
                        memoryRepository.removeTagFromMemory(memory.id, tag.id)
                    }
                }
                for (tagName in newTagNames) {
                    if (tagName !in oldTagNames) {
                        memoryRepository.addTagToMemory(memory.id, tagName)
                    }
                }

                // Remove marked media files
                for (mediaId in state.editRemovedMediaIds) {
                    val mediaFile = state.mediaFiles.find { it.id == mediaId }
                    if (mediaFile != null) {
                        memoryRepository.removeMediaFile(mediaFile)
                    }
                }

                // Add new media files
                if (state.editNewMediaItems.isNotEmpty()) {
                    val thumbDir = File(appContext.cacheDir, "thumbnails")
                    withContext(Dispatchers.IO) {
                        for (item in state.editNewMediaItems) {
                            val entity = processMediaItem(item, thumbDir)
                            if (entity != null) {
                                memoryRepository.addMediaFile(entity)
                            }
                        }
                    }
                }

                // Reload to get fresh data
                loadMemory()
                _uiState.update {
                    it.copy(
                        isEditing = false,
                        isSaving = false,
                        editRemovedMediaIds = emptySet(),
                        editNewMediaItems = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = "Failed to save: ${e.message}")
                }
            }
        }
    }

    fun importToMemly(mediaFile: MediaFileEntity) {
        if (mediaFile.source != MediaSource.EXTERNAL) return
        viewModelScope.launch {
            try {
                val sourceUri = Uri.parse(mediaFile.mediaStoreUri)
                val mimeType = mediaFile.mimeType ?: "image/jpeg"
                val metadata = mediaStoreManager.insertMedia(sourceUri, mediaFile.mediaType, mimeType)
                    ?: throw Exception("Failed to copy file")

                val updated = mediaFile.copy(
                    mediaStoreUri = metadata.uri.toString(),
                    source = MediaSource.IMPORTED,
                    relativePath = metadata.relativePath,
                    displayName = metadata.displayName,
                    size = metadata.size,
                    dateTaken = metadata.dateTaken,
                    width = metadata.width,
                    height = metadata.height
                )
                memoryRepository.updateMediaFile(updated)

                // Reload
                loadMemory()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to import: ${e.message}") }
            }
        }
    }

    fun removeBrokenReference(mediaFile: MediaFileEntity) {
        viewModelScope.launch {
            try {
                memoryRepository.removeMediaFile(mediaFile)
                loadMemory()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to remove: ${e.message}") }
            }
        }
    }

    fun deleteMemory() {
        val state = _uiState.value
        val memory = state.memory ?: return
        viewModelScope.launch {
            try {
                memoryRepository.deleteMemoryWithFiles(memory, state.mediaFiles)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun showCollectionDialog() {
        viewModelScope.launch {
            try {
                val collections = collectionRepository.getAllCollections().first()
                val memberIds = collectionRepository.getCollectionIdsForMemory(memoryId).first().toSet()
                _uiState.update {
                    it.copy(
                        showCollectionDialog = true,
                        allCollections = collections,
                        memberCollectionIds = memberIds
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load collections: ${e.message}") }
            }
        }
    }

    fun hideCollectionDialog() {
        _uiState.update { it.copy(showCollectionDialog = false) }
    }

    fun toggleCollection(collectionId: Long) {
        viewModelScope.launch {
            try {
                val isMember = collectionId in _uiState.value.memberCollectionIds
                if (isMember) {
                    collectionRepository.removeMemoryFromCollection(memoryId, collectionId)
                    _uiState.update {
                        it.copy(memberCollectionIds = it.memberCollectionIds - collectionId)
                    }
                } else {
                    collectionRepository.addMemoryToCollection(memoryId, collectionId)
                    _uiState.update {
                        it.copy(memberCollectionIds = it.memberCollectionIds + collectionId)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update collection: ${e.message}") }
            }
        }
    }
}
