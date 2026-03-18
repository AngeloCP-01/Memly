# Memly System Design Document

A comprehensive system design reference for the Memly Android app -- a personal memory
journal built with Kotlin, Jetpack Compose, Room, Hilt, Navigation Compose, and Coil.

Architecture: **MVVM with Repository** (no domain layer).

Package root: `com.example.memly`

---

## Table of Contents

1. [Entity Relationship Diagram](#1-entity-relationship-diagram)
2. [Memory Capture System](#2-memory-capture-system)
3. [File Management System](#3-file-management-system)
4. [Navigation & Screen Flow](#4-navigation--screen-flow)
5. [State Management Patterns](#5-state-management-patterns)
6. [Location System](#6-location-system)
7. [Thumbnail System](#7-thumbnail-system)

---

## 1. Entity Relationship Diagram

### 1.1 Full ER Diagram (ASCII)

```
+-------------------------------+
|        MemoryEntity           |
|        (memories)             |
+-------------------------------+
| PK  id: Long (auto)          |
|     title: String?            |
|     notes: String?            |
|     mood: Mood? (enum/String) |
|     latitude: Double?         |
|     longitude: Double?        |
|     placeLabel: String?       |
|     createdAt: Long           |
|     memoryDate: Long          |
+-------------------------------+
        |           |                |
        |           |                |
        | 1         | M              | M
        |           |                |
        v N         v                v
+------------------+   +---------------------+   +-------------------------+
|  MediaFileEntity |   | MemoryTagCrossRef   |   | MemoryCollectionCrossRef|
|  (media_files)   |   | (memory_tag_cross_  |   | (memory_collection_     |
|                  |   |  ref)               |   |  cross_ref)             |
+------------------+   +---------------------+   +-------------------------+
| PK id: Long      |   | PK,FK memoryId:Long |   | PK,FK memoryId: Long   |
| FK memoryId:Long |   | PK,FK tagId: Long   |   | PK,FK collectionId:Long|
|    filePath:Str   |   +---------------------+   +-------------------------+
|    thumbnailPath: |           |                            |
|      String?      |           | M                          | M
|    fileHash:Str   |           v                            v
|    mediaType:     |   +------------------+       +---------------------+
|      MediaType    |   |    TagEntity     |       |  CollectionEntity   |
|    isReference:   |   |    (tags)        |       |  (collections)      |
|      Boolean      |   +------------------+       +---------------------+
+------------------+   | PK id: Long      |       | PK id: Long         |
   idx: memoryId       |    name: String   |       |    name: String     |
   idx: fileHash       |   (unique index)  |       |    description:Str? |
                       +------------------+       |    coverMediaId:    |
                          idx: name (unique)      |      Long?          |
                                                   |    createdAt: Long  |
                                                   +---------------------+
```

### 1.2 Relationship Summary

```
MemoryEntity  ----< 1:N >----  MediaFileEntity
     FK: media_files.memoryId -> memories.id  (CASCADE DELETE)

MemoryEntity  ----< M:N >----  TagEntity
     Junction: MemoryTagCrossRef(memoryId, tagId)
     FK: memoryId -> memories.id  (CASCADE DELETE)
     FK: tagId   -> tags.id      (CASCADE DELETE)

MemoryEntity  ----< M:N >----  CollectionEntity
     Junction: MemoryCollectionCrossRef(memoryId, collectionId)
     FK: memoryId     -> memories.id     (CASCADE DELETE)
     FK: collectionId -> collections.id  (CASCADE DELETE)
```

### 1.3 Relation Class: MemoryWithDetails

This is Room's way of loading a memory with all its related data in one query.

```kotlin
data class MemoryWithDetails(
    @Embedded val memory: MemoryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "memoryId"
    )
    val mediaFiles: List<MediaFileEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MemoryTagCrossRef::class,
            parentColumn = "memoryId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
```

Under the hood, Room executes:

```
1. SELECT * FROM memories ...                                   --> MemoryEntity
2. SELECT * FROM media_files WHERE memoryId IN (?)              --> List<MediaFileEntity>
3. SELECT t.* FROM tags t
     INNER JOIN memory_tag_cross_ref x ON t.id = x.tagId
     WHERE x.memoryId IN (?)                                    --> List<TagEntity>
```

DAO methods that return `MemoryWithDetails` MUST be annotated with `@Transaction` to
ensure all three queries see a consistent snapshot.

### 1.4 Enum Storage

Room stores enums as strings by default (no explicit TypeConverter needed for basic enums):

| Enum       | Values                                                                                 |
|------------|----------------------------------------------------------------------------------------|
| `Mood`     | HAPPY, NOSTALGIC, ADVENTUROUS, CALM, EXCITED, GRATEFUL, ROMANTIC, REFLECTIVE, SAD, FUNNY |
| `MediaType`| PHOTO, VIDEO                                                                           |

### 1.5 Index Strategy

| Table                         | Index Columns           | Unique | Purpose                          |
|-------------------------------|-------------------------|--------|----------------------------------|
| media_files                   | memoryId                | No     | Fast FK lookup on cascade/join   |
| media_files                   | fileHash                | No     | Dedup check in O(1)              |
| tags                          | name                    | Yes    | Prevent duplicate tag names      |
| memory_tag_cross_ref          | memoryId                | No     | Fast join from memory side       |
| memory_tag_cross_ref          | tagId                   | No     | Fast join from tag side          |
| memory_collection_cross_ref   | memoryId                | No     | Fast join from memory side       |
| memory_collection_cross_ref   | collectionId            | No     | Fast join from collection side   |

---

## 2. Memory Capture System

The capture flow is the most complex user interaction in Memly. It coordinates media
selection, permission handling, file processing, and transactional database writes.

### 2.1 CaptureUiState

```kotlin
data class CaptureUiState(
    // Form fields
    val title: String = "",
    val notes: String = "",
    val selectedMood: Mood? = null,
    val mediaItems: List<CaptureMediaItem> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeLabel: String = "",
    val tags: List<String> = emptyList(),
    val memoryDate: Long = System.currentTimeMillis(),

    // Process state
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false
)

data class CaptureMediaItem(
    val uri: Uri,
    val mediaType: MediaType,
    val thumbnailBitmap: Bitmap? = null
)
```

**Field notes:**
- `mediaItems` holds transient content URIs from the picker or camera. These are NOT
  yet persisted -- they are raw references that will be processed on save.
- `thumbnailBitmap` is an in-memory preview for the capture form's media grid. It is
  generated immediately after the user selects/takes a photo so they see a preview.
- `isSaving` disables the save button and shows a progress indicator.
- `savedSuccessfully` triggers navigation back to the timeline.

### 2.2 Capture Flow Sequence Diagram

```
  User          CaptureScreen       CaptureViewModel      MemoryRepository       Room DB        FileSystem
   |                 |                    |                      |                   |               |
   |  Open screen    |                    |                      |                   |               |
   |---------------->|                    |                      |                   |               |
   |                 |  collectAsState     |                      |                   |               |
   |                 |<-------------------|                      |                   |               |
   |                 |  (empty form)      |                      |                   |               |
   |                 |                    |                      |                   |               |
   |  Tap "Add Photo"|                    |                      |                   |               |
   |---------------->|                    |                      |                   |               |
   |                 | launch PhotoPicker |                      |                   |               |
   |                 |  (PickMultipleVisualMedia)                 |                   |               |
   |                 |                    |                      |                   |               |
   |  Select photos  |                    |                      |                   |               |
   |<- - - - - - - ->|                    |                      |                   |               |
   |                 | onMediaSelected(uris)                     |                   |               |
   |                 |------------------->|                      |                   |               |
   |                 |                    | generate preview     |                   |               |
   |                 |                    | thumbnails (IO)      |                   |               |
   |                 |                    |                      |                   |               |
   |                 |                    | update uiState       |                   |               |
   |                 |                    | .mediaItems += items |                   |               |
   |                 |                    |                      |                   |               |
   |  Tap "Take Photo"                   |                      |                   |               |
   |---------------->|                    |                      |                   |               |
   |                 | check CAMERA perm  |                      |                   |               |
   |                 | (granted?)-------->|                      |                   |               |
   |                 |                    | create temp URI       |                   |               |
   |                 |                    | via FileProvider     |                   |               |
   |                 | launch camera      |                      |                   |               |
   |                 |  ACTION_IMAGE_CAPTURE                     |                   |               |
   |  Take photo     |                    |                      |                   |               |
   |<- - - - - - - ->|                    |                      |                   |               |
   |                 | onCameraResult(uri)|                      |                   |               |
   |                 |------------------->|                      |                   |               |
   |                 |                    | update uiState       |                   |               |
   |                 |                    |                      |                   |               |
   |  Fill form      |                    |                      |                   |               |
   |  (title, notes, |                    |                      |                   |               |
   |   mood, tags,   |                    |                      |                   |               |
   |   location)     |                    |                      |                   |               |
   |---------------->| onTitleChange()    |                      |                   |               |
   |                 |------------------->| update uiState       |                   |               |
   |                 |                    |                      |                   |               |
   |  Tap "Save"     |                    |                      |                   |               |
   |---------------->| onSave()          |                      |                   |               |
   |                 |------------------->|                      |                   |               |
   |                 |                    | isSaving = true      |                   |               |
   |                 |                    |                      |                   |               |
   |                 |                    | saveMemory(state)    |                   |               |
   |                 |                    |--------------------->|                   |               |
   |                 |                    |                      |  @Transaction {   |               |
   |                 |                    |                      |                   |               |
   |                 |                    |                      |  FOR EACH media URI:              |
   |                 |                    |                      |  1. openInputStream|               |
   |                 |                    |                      |------------------||               |
   |                 |                    |                      |  2. computeSHA256 |               |
   |                 |                    |                      |                   |               |
   |                 |                    |                      |  3. findMediaByHash(hash)         |
   |                 |                    |                      |------------------>|               |
   |                 |                    |                      |  <-- result       |               |
   |                 |                    |                      |                   |               |
   |                 |                    |                      |  IF exists:       |               |
   |                 |                    |                      |    reuse filePath  |               |
   |                 |                    |                      |    isReference=true|               |
   |                 |                    |                      |                   |               |
   |                 |                    |                      |  IF new:          |               |
   |                 |                    |                      |    copy to media/ |-------------->|
   |                 |                    |                      |    gen thumbnail  |-------------->|
   |                 |                    |                      |    isReference=false               |
   |                 |                    |                      |                   |               |
   |                 |                    |                      |  insertMemory()   |               |
   |                 |                    |                      |------------------>|               |
   |                 |                    |                      |  <-- memoryId     |               |
   |                 |                    |                      |                   |               |
   |                 |                    |                      |  FOR EACH media:  |               |
   |                 |                    |                      |  insertMediaFile()|               |
   |                 |                    |                      |------------------>|               |
   |                 |                    |                      |                   |               |
   |                 |                    |                      |  FOR EACH tag:    |               |
   |                 |                    |                      |  getTagByName()   |               |
   |                 |                    |                      |  or insertTag()   |               |
   |                 |                    |                      |  insertCrossRef() |               |
   |                 |                    |                      |------------------>|               |
   |                 |                    |                      |                   |               |
   |                 |                    |                      |  } // end @Transaction            |
   |                 |                    |                      |                   |               |
   |                 |                    | <-- success          |                   |               |
   |                 |                    | savedSuccessfully=true                   |               |
   |                 |                    |                      |                   |               |
   |                 | LaunchedEffect     |                      |                   |               |
   |                 | (savedSuccessfully)|                      |                   |               |
   |                 | onMemorySaved()    |                      |                   |               |
   |                 | --> navController  |                      |                   |               |
   |                 |     .popBackStack()|                      |                   |               |
   |                 |                    |                      |                   |               |
   |  (Back on       |                    |                      |                   |               |
   |   Timeline)     |                    |                      |                   |               |
```

### 2.3 CaptureViewModel Method Signatures

```kotlin
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    // --- Form field updates ---
    fun onTitleChange(title: String)
    fun onNotesChange(notes: String)
    fun onMoodSelected(mood: Mood?)
    fun onMemoryDateChange(date: Long)
    fun onPlaceLabelChange(label: String)

    // --- Media ---
    fun onMediaSelected(uris: List<Uri>, mediaType: MediaType)
    fun onCameraPhotoTaken(uri: Uri)
    fun onRemoveMedia(index: Int)

    // --- Tags ---
    fun onAddTag(tag: String)
    fun onRemoveTag(tag: String)

    // --- Location ---
    fun onLocationObtained(lat: Double, lng: Double)
    fun onLocationFailed()

    // --- Save ---
    fun saveMemory()
    fun clearError()
}
```

### 2.4 Repository saveMemory() -- Transactional Write

```kotlin
// In MemoryRepository (proposed addition)
suspend fun saveMemory(
    memory: MemoryEntity,
    mediaItems: List<ProcessedMediaItem>,
    tags: List<String>
): Long {
    return db.withTransaction {
        val memoryId = memoryDao.insertMemory(memory)

        for (item in mediaItems) {
            memoryDao.insertMediaFile(
                MediaFileEntity(
                    memoryId = memoryId,
                    filePath = item.filePath,
                    thumbnailPath = item.thumbnailPath,
                    fileHash = item.hash,
                    mediaType = item.mediaType,
                    isReference = item.isReference
                )
            )
        }

        for (tagName in tags) {
            val existingTag = tagDao.getTagByName(tagName)
            val tagId = existingTag?.id ?: tagDao.insertTag(TagEntity(name = tagName))
            memoryDao.insertMemoryTagCrossRef(MemoryTagCrossRef(memoryId, tagId))
        }

        memoryId
    }
}
```

Where `ProcessedMediaItem` is:

```kotlin
data class ProcessedMediaItem(
    val filePath: String,
    val thumbnailPath: String?,
    val hash: String,
    val mediaType: MediaType,
    val isReference: Boolean
)
```

### 2.5 Permission Handling

#### Permission Decision Tree

```
                           +---------------------------+
                           | User taps "Add Photo"     |
                           +---------------------------+
                                       |
                          +------------+------------+
                          |                         |
                   Photo Picker API            Camera Intent
                   (PickMultipleVisualMedia)    (ACTION_IMAGE_CAPTURE)
                          |                         |
                    No permission                   |
                    needed                   +------v--------+
                          |                  | Has CAMERA    |
                          v                  | permission?   |
                    Launch picker            +------+--------+
                                              |Yes       |No
                                              |          |
                                              v    +-----v-----------+
                                         Launch    | Should show     |
                                         camera    | rationale?      |
                                                   +-----+-----------+
                                                    |Yes       |No
                                                    |          |
                                              +-----v----+ +---v--------------+
                                              | Show      | | Request          |
                                              | rationale | | permission       |
                                              | dialog    | | (first time)     |
                                              +-----+----+ +---+--------------+
                                                    |          |
                                                    v          v
                                              Request     +---+----------+
                                              permission  | Granted?     |
                                                          +---+----------+
                                                         |Yes      |No
                                                         |         |
                                                    Launch    +----v----------+
                                                    camera    | Permanently   |
                                                              | denied?       |
                                                              +----+----------+
                                                              |Yes      |No
                                                              |         |
                                                         +----v----+   Show
                                                         | "Go to  |   snackbar
                                                         | Settings"|   "Permission
                                                         | link    |   required"
                                                         +---------+
```

#### Permission Implementation Pattern

```kotlin
// In CaptureScreen composable:

// Camera permission
val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted: Boolean ->
    if (isGranted) {
        launchCamera()
    } else {
        viewModel.onError("Camera permission is required to take photos")
    }
}

// Location permission
val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted: Boolean ->
    if (isGranted) {
        fetchLocation()
    }
    // Silently skip if denied -- location is optional
}
```

#### Media Permission Matrix (by API level)

| API Level | Photo Picker               | Camera                | Gallery (fallback)             |
|-----------|----------------------------|-----------------------|--------------------------------|
| 33+ (T)   | No permission needed       | CAMERA                | READ_MEDIA_IMAGES,             |
|           | PickMultipleVisualMedia    |                       | READ_MEDIA_VIDEO               |
| 29-32     | No permission needed       | CAMERA                | READ_EXTERNAL_STORAGE          |
| < 29      | Not available, use intent  | CAMERA                | READ_EXTERNAL_STORAGE          |

**Preferred path:** Always use the Photo Picker API (available on most devices via
Google Play Services backport). It requires zero permissions and provides a consistent
UI. Fall back to intent-based gallery access only if Photo Picker is unavailable.

### 2.6 Media Selection Strategy

#### Photo Picker API (Preferred)

```kotlin
// Launcher setup
val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
) { uris: List<Uri> ->
    viewModel.onMediaSelected(uris, MediaType.PHOTO)
}

// Launch
photoPickerLauncher.launch(
    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
)
```

#### Camera Intent

```kotlin
// 1. Create a temp file URI via FileProvider
val photoUri = FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
)

// 2. Launch camera
val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
) { success: Boolean ->
    if (success) {
        viewModel.onCameraPhotoTaken(photoUri)
    }
}

cameraLauncher.launch(photoUri)
```

#### FileProvider Setup

**AndroidManifest.xml:**
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

**res/xml/file_paths.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="capture_images" path="/" />
    <files-path name="media" path="media/" />
    <files-path name="thumbnails" path="thumbnails/" />
</paths>
```

---

## 3. File Management System

### 3.1 MediaStore-Based Public Storage

Media files are stored in **public directories** via Android's MediaStore API. This ensures files survive app uninstall and are visible in the device's gallery and file manager.

**Three-state ownership model:**

```kotlin
enum class MediaSource {
    APP_OWNED,   // Created in-app (camera/recording), lives in Pictures/Memly/
    EXTERNAL,    // URI reference to user's gallery photo, zero storage cost
    IMPORTED     // User chose "Save to Memly", copied to Pictures/Memly/
}
```

**Directory layout (public storage):**

```
Pictures/Memly/
  +-- memly_20260318_143022_a7f3.jpg     <-- APP_OWNED or IMPORTED
  +-- memly_20260318_150511_b2c1.jpg
  +-- ...
Movies/Memly/
  +-- memly_20260318_160000_c4d2.mp4
Music/Memly/
  +-- memly_20260318_170000_e5f3.aac

cacheDir/thumbnails/                      <-- app-private, regenerable
  +-- a7f3_thumb.jpg                       <-- 300px max dimension, JPEG 80%
  +-- c4d2_thumb.jpg
```

**Naming convention:**
- Media files: `memly_<yyyyMMdd_HHmmss>_<shortId>.<ext>`
- Thumbnails: `{shortId}_thumb.jpg`

**Why MediaStore-based:**
- Files survive app uninstall (public storage, not app-private).
- Visible in gallery and file manager automatically.
- No storage duplication for referenced files (EXTERNAL source).
- Modern Android-compliant (scoped storage compatible).

### 3.2 File Operations Flow -- Adding Media

#### Flow A: In-App Camera / Audio Recording (APP_OWNED)

```
Camera or AudioRecorder produces content
   |
   v
+-----------------------------------------------------+
| 1. Insert via MediaStore                             |
|    val values = ContentValues().apply {               |
|        put(DISPLAY_NAME, "memly_<ts>_<id>.jpg")     |
|        put(MIME_TYPE, "image/jpeg")                  |
|        put(RELATIVE_PATH, "Pictures/Memly")          |
|    }                                                 |
|    val uri = contentResolver.insert(                  |
|        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,  |
|        values                                        |
|    )                                                 |
+-----------------------------------------------------+
   |
   v
+-----------------------------------------------------+
| 2. Write content to URI output stream                |
|    contentResolver.openOutputStream(uri)              |
|        .use { output -> content.copyTo(output) }     |
+-----------------------------------------------------+
   |
   v
+-----------------------------------------------------+
| 3. Compute SHA-256 hash                              |
|    val hash = FileHashUtil.computeSha256(             |
|        contentResolver.openInputStream(uri))          |
+-----------------------------------------------------+
   |
   v
+-----------------------------------------------------+
| 4. Cache metadata from MediaStore                    |
|    Query: mimeType, size, dateTaken, width, height   |
+-----------------------------------------------------+
   |
   v
+-----------------------------------------------------+
| 5. Generate thumbnail → cacheDir/thumbnails/         |
+-----------------------------------------------------+
   |
   v
Return MediaFileEntity(
    mediaStoreUri = uri.toString(),
    source = APP_OWNED,
    relativePath = "Pictures/Memly",
    displayName = "memly_<ts>_<id>.jpg",
    fileHash = hash,
    // + cached metadata fields
)
```

#### Flow B: Picked from Gallery (EXTERNAL or IMPORTED)

```
PhotoPicker returns temporary content:// URI
   |
   v
+-----------------------------------------------------+
| 1. Show user choice dialog:                         |
|    "Keep in original location" → EXTERNAL            |
|    "Save to Memly" → IMPORTED                        |
|    (Warning on "Keep": "If original is deleted,      |
|     this photo may no longer appear in Memly.")      |
+-----------------------------------------------------+
   |
   +--- EXTERNAL (reference only) ---------------------+
   |                                                    |
   |  +------------------------------------------------+
   |  | 2a. Resolve URI                                |
   |  |     Try: query MediaStore by _ID → stable URI  |
   |  |     Fallback: takePersistableUriPermission()   |
   |  |         (for cloud-backed providers)            |
   |  +------------------------------------------------+
   |       |
   |       v
   |  +------------------------------------------------+
   |  | 3a. Compute hash + cache metadata              |
   |  +------------------------------------------------+
   |       |
   |       v
   |  +------------------------------------------------+
   |  | 4a. Generate thumbnail → cacheDir/thumbnails/  |
   |  +------------------------------------------------+
   |       |
   |       v
   |  Return MediaFileEntity(
   |      mediaStoreUri = resolvedUri.toString(),
   |      source = EXTERNAL,
   |      fileHash = hash,
   |      // + cached metadata fields
   |  )
   |                                                    |
   +--- IMPORTED (copy to Memly) ----------------------+
   |                                                    |
   |  +------------------------------------------------+
   |  | 2b. Compute hash, check dedup                  |
   |  |     val existing = findMediaByHash(hash)       |
   |  |     IF EXISTS: reuse existing URI, skip copy   |
   |  +------------------------------------------------+
   |       |
   |       +--- NOT EXISTS:
   |       |
   |  +------------------------------------------------+
   |  | 3b. Copy to Pictures/Memly/ via MediaStore     |
   |  |     insert + openOutputStream + copyTo         |
   |  +------------------------------------------------+
   |       |
   |       v
   |  +------------------------------------------------+
   |  | 4b. Generate thumbnail → cacheDir/thumbnails/  |
   |  +------------------------------------------------+
   |       |
   |       v
   |  Return MediaFileEntity(
   |      mediaStoreUri = newUri.toString(),
   |      source = IMPORTED,
   |      fileHash = hash,
   |      // + cached metadata fields
   |  )
   +----------------------------------------------------+
```

### 3.3 FileHashUtil Implementation

```kotlin
object FileHashUtil {

    fun computeSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
```

Key properties:
- Streams in 8KB chunks -- constant memory usage regardless of file size.
- Returns a 64-character lowercase hex string.
- The caller is responsible for closing the InputStream.
- Works with any content:// URI via `contentResolver.openInputStream()`.

### 3.4 File Deletion Strategy

Deletion behavior depends on the `MediaSource` of the file.

#### Deletion Flow

```
deleteMemoryWithCleanup(memory: MemoryEntity)
   |
   v
+----------------------------------------------------------+
| 1. Collect media files BEFORE deletion                   |
|    val mediaFiles = memoryDao                             |
|        .getMediaFilesForMemorySync(memory.id)            |
+----------------------------------------------------------+
   |
   v
+----------------------------------------------------------+
| 2. Delete the memory (CASCADE deletes MediaFileEntity    |
|    rows and cross-ref rows)                              |
|    memoryDao.deleteMemory(memory)                         |
+----------------------------------------------------------+
   |
   v
+----------------------------------------------------------+
| 3. For each media file, handle by source:                |
|                                                          |
|    FOR mediaFile IN mediaFiles:                           |
|                                                          |
|      IF source == EXTERNAL:                               |
|        // Do nothing. Only DB row was deleted.            |
|        // Never touch the original file.                  |
|                                                          |
|      IF source == APP_OWNED or IMPORTED:                  |
|        // Check if other memories still reference hash    |
|        val count = memoryDao.countMediaByHash(hash)       |
|        IF count == 0:                                     |
|          try:                                             |
|            contentResolver.delete(mediaStoreUri)          |
|          catch:                                           |
|            // Scoped storage restriction (Android 11+)    |
|            // Fall back to createDeleteRequest()          |
|          // Also delete thumbnail from cache              |
+----------------------------------------------------------+
```

#### Required DAO Queries

```kotlin
@Query("SELECT COUNT(*) FROM media_files WHERE fileHash = :hash")
suspend fun countMediaByHash(hash: String): Int
```

**Important:** File deletion is performed in the Repository layer via `MediaStoreManager`,
NOT via Room CASCADE. Room CASCADE only handles row deletion; file deletion is application logic.

### 3.5 Broken Reference Handling

External references can break if the user deletes the original from their gallery,
or if a persisted SAF URI permission is revoked.

```
When loading memory list or opening detail:
   |
   v
+------------------------------------------------------+
| For each EXTERNAL media item:                        |
|   contentResolver.query(mediaStoreUri) != null ?     |
+------------------------------------------------------+
   |
   +--- AVAILABLE ---> Load normally with Coil AsyncImage
   |
   +--- UNAVAILABLE:
        |
        +--- Is thumbnail available in cache?
        |    |
        |    +--- YES ---> Show thumbnail as fallback
        |    +--- NO  ---> Show placeholder:
        |                   - Surface-variant colored box
        |                   - "Original file removed" text
        |                   - Broken-image icon
        |                   - Option to remove from memory
        |
        +--- Do NOT delete the MemoryEntity
        |    (metadata like title, notes, tags is still valuable)
        |
        +--- Log warning for diagnostics
```

### 3.6 Permissions by API Level

| API Level | Requirement |
|-----------|-------------|
| 28 (Android 9) | `WRITE_EXTERNAL_STORAGE` for writing to public dirs |
| 29+ (Android 10+) | No permission needed for MediaStore inserts (app owns entries) |
| 30+ (Android 11+) | `createDeleteRequest()` available for delete confirmation |
| 33+ (Android 13+) | `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` for reading EXTERNAL references |

---

## 4. Navigation & Screen Flow

### 4.1 Complete Screen Map

```
+===========================================================================+
|                           MEMLY APP                                       |
+===========================================================================+
|                                                                           |
|  +-- Onboarding (shown once, first launch) -------------------------+    |
|  |   [Welcome] --> [How it works] --> [Permissions] --> [Get Started]|    |
|  +------------------------------------------------------------------+    |
|       |                                                                   |
|       | (completes, sets SharedPreferences flag)                          |
|       v                                                                   |
|  +====================================================================+  |
|  |                     MAIN SCAFFOLD                                   |  |
|  |                                                                     |  |
|  |  +------+   +------+   +-------+                    +----------+   |  |
|  |  | Time |   | Map  |   |Search |                    | Settings |   |  |
|  |  | line |   |      |   |       |                    |          |   |  |
|  |  +--+---+   +--+---+   +---+---+                    +----+-----+   |  |
|  |     |           |           |                             ^         |  |
|  |     |   BOTTOM NAV BAR     |         (profile/gear icon)  |         |  |
|  |  ===+===========+===========+=====  =====================+===      |  |
|  |                                                                     |  |
|  |                      [  FAB (+)  ]                                  |  |
|  +=====================================================+===============+  |
|                         |                              |                  |
|                         v                              |                  |
|              +--------------------+                    |                  |
|              |   Capture Screen   |  (full screen,     |                  |
|              |   (hides bottom    |   no bottom nav)   |                  |
|              |    nav bar)        |                    |                  |
|              +--------+-----------+                    |                  |
|                       |                                |                  |
|                       | onMemorySaved()                |                  |
|                       | popBackStack()                 |                  |
|                       v                                |                  |
|                  Back to Timeline                      |                  |
|                                                        |                  |
|  +-----------------------------------------------------+                 |
|  |  Navigation from list/map/search to detail:                           |
|  |                                                                       |
|  |  Timeline ----+                                                       |
|  |  Map pin  ----+--> MemoryDetail Screen (hides bottom nav)             |
|  |  Search   ----+         |                                             |
|  |  Collection --+         +--> Edit mode (inline toggle, same screen)   |
|  |                         +--> Delete (confirmation dialog)             |
|  |                         +--> Share                                    |
|  +-----------------------------------------------------------------------+
|                                                                           |
|  +-----------------------------------------------------------------------+
|  |  Collection flow:                                                     |
|  |                                                                       |
|  |  Search tab -----> Collection List -----> Collection Detail           |
|  |  (or Settings)                                  |                     |
|  |                                                 +--> MemoryDetail     |
|  +-----------------------------------------------------------------------+
+===========================================================================+
```

### 4.2 Screen Sealed Class

```kotlin
sealed class Screen(val route: String) {
    data object Timeline : Screen("timeline")
    data object Map : Screen("map")
    data object Search : Screen("search")
    data object Settings : Screen("settings")
    data object Capture : Screen("capture")
    data object Onboarding : Screen("onboarding")

    data object MemoryDetail : Screen("memory/{memoryId}") {
        fun createRoute(memoryId: Long) = "memory/$memoryId"
    }

    data object CollectionDetail : Screen("collection/{collectionId}") {
        fun createRoute(collectionId: Long) = "collection/$collectionId"
    }
}
```

### 4.3 Navigation Data Contracts

| Screen             | Route Pattern                   | Arguments                        | Type       | How Data is Passed                |
|--------------------|---------------------------------|----------------------------------|------------|-----------------------------------|
| Timeline           | `timeline`                      | None                             | --         | --                                |
| Map                | `map`                           | None                             | --         | --                                |
| Search             | `search`                        | None                             | --         | --                                |
| Settings           | `settings`                      | None                             | --         | --                                |
| Capture            | `capture`                       | None                             | --         | Result via popBackStack()         |
| Onboarding         | `onboarding`                    | None                             | --         | Completion via SharedPreferences  |
| MemoryDetail       | `memory/{memoryId}`             | memoryId                         | Long       | NavArgument (path parameter)      |
| CollectionDetail   | `collection/{collectionId}`     | collectionId                     | Long       | NavArgument (path parameter)      |

#### Navigation Argument Extraction

```kotlin
// MemoryDetail route registration
composable(
    route = Screen.MemoryDetail.route,
    arguments = listOf(
        navArgument("memoryId") { type = NavType.LongType }
    )
) { backStackEntry ->
    MemoryDetailScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}

// In MemoryDetailViewModel, argument extracted via SavedStateHandle:
@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoryRepository: MemoryRepository
) : ViewModel() {
    private val memoryId: Long = savedStateHandle["memoryId"]!!
    // ...
}
```

### 4.4 Bottom Navigation Bar Visibility

```kotlin
// In MainActivity or MainScaffold composable:
val currentRoute = navController.currentBackStackEntryAsState()
    .value?.destination?.route

val showBottomBar = currentRoute in listOf(
    Screen.Timeline.route,
    Screen.Map.route,
    Screen.Search.route
)

val showFab = showBottomBar  // FAB visible only when bottom bar is visible

Scaffold(
    bottomBar = { if (showBottomBar) MemlyBottomNavBar(...) },
    floatingActionButton = {
        if (showFab) {
            FloatingActionButton(onClick = { navController.navigate(Screen.Capture.route) }) {
                Icon(Icons.Default.Add, contentDescription = "New Memory")
            }
        }
    }
) { ... }
```

### 4.5 Navigation Graph Structure

```
NavHost(startDestination = "timeline")
  |
  +-- "timeline"           --> TimelineScreen
  +-- "map"                --> MapScreen
  +-- "search"             --> SearchScreen
  +-- "settings"           --> SettingsScreen
  +-- "capture"            --> CaptureScreen
  +-- "onboarding"         --> OnboardingScreen
  +-- "memory/{memoryId}"  --> MemoryDetailScreen
  +-- "collection/{collectionId}" --> CollectionDetailScreen
```

Start destination logic:

```kotlin
val startDestination = if (isFirstLaunch) {
    Screen.Onboarding.route
} else {
    Screen.Timeline.route
}
```

---

## 5. State Management Patterns

### 5.1 UiState Definitions for All Screens

#### TimelineUiState

```kotlin
data class TimelineUiState(
    val groups: List<TimelineGroup> = emptyList(),
    val timeHopMemories: List<MemoryWithDetails> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class TimelineGroup(
    val header: String,                    // e.g., "March 16, 2026" or "This Week"
    val memories: List<MemoryWithDetails>
)
```

#### MapUiState

```kotlin
data class MapUiState(
    val markers: List<MapMemoryMarker> = emptyList(),
    val selectedMemory: MemoryWithDetails? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class MapMemoryMarker(
    val memoryId: Long,
    val latitude: Double,
    val longitude: Double,
    val title: String?,
    val mood: Mood?,
    val thumbnailPath: String?
)
```

#### SearchUiState

```kotlin
data class SearchUiState(
    val query: String = "",
    val selectedMoodFilter: Mood? = null,
    val selectedTagFilter: String? = null,
    val dateRangeStart: Long? = null,
    val dateRangeEnd: Long? = null,
    val results: List<MemoryWithDetails> = emptyList(),
    val availableTags: List<TagEntity> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)
```

#### CaptureUiState

(Defined in section 2.1 above.)

#### MemoryDetailUiState

```kotlin
data class MemoryDetailUiState(
    val memory: MemoryWithDetails? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,

    // Edit mode fields (populated from memory when entering edit mode)
    val editTitle: String = "",
    val editNotes: String = "",
    val editMood: Mood? = null,
    val editPlaceLabel: String = "",
    val editTags: List<String> = emptyList(),

    val error: String? = null
)
```

#### CollectionListUiState

```kotlin
data class CollectionListUiState(
    val collections: List<CollectionWithPreview> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class CollectionWithPreview(
    val collection: CollectionEntity,
    val memoryCount: Int,
    val coverThumbnailPath: String?
)
```

#### CollectionDetailUiState

```kotlin
data class CollectionDetailUiState(
    val collection: CollectionEntity? = null,
    val memories: List<MemoryWithDetails> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
```

#### SettingsUiState

```kotlin
data class SettingsUiState(
    val mediaFileCount: Int = 0,
    val totalStorageBytes: Long = 0L,
    val thumbnailStorageBytes: Long = 0L,
    val memoryCount: Int = 0,
    val appVersion: String = "",
    val isLoading: Boolean = true
)
```

### 5.2 Standard ViewModel Pattern

Every ViewModel in Memly follows this template:

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: SomeRepository
) : ViewModel() {

    // -- State --
    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    // -- Init: load data --
    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getData()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { data ->
                    _uiState.update { it.copy(data = data, isLoading = false) }
                }
        }
    }

    // -- User actions --
    fun onAction(action: SomeAction) {
        viewModelScope.launch {
            try {
                repository.performAction(action)
                // Success: state will update automatically via Flow
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // -- Error dismissal --
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
```

**Key conventions:**
- `MutableStateFlow` is private; only `StateFlow` is exposed.
- `init` block kicks off initial data load.
- Read operations collect from Room `Flow<T>` (auto-updating).
- Write operations use `try/catch` and store errors in UiState.
- No `LiveData` -- pure `StateFlow` throughout.

### 5.3 Reactive Data Flow (End-to-End)

```
+============+     +============+     +=============+     +=================+     +============+
|  Room DB   |     |    DAO     |     | Repository  |     |   ViewModel     |     | Composable |
+============+     +============+     +=============+     +=================+     +============+
|            |     |            |     |             |     |                 |     |            |
| INSERT     |     |            |     |             |     |                 |     |            |
| UPDATE  -------->| Flow<T>   -------->| Flow<T>   -------->| collect into  |     |            |
| DELETE     |     | auto-emits |     | pass-through|     | StateFlow<      |     |            |
|            |     |            |     |             |     |   UiState>      |     |            |
|            |     |            |     |             |     |       |         |     |            |
|            |     |            |     |             |     |       v         |     |            |
|            |     |            |     |             |     | _uiState.update -------->| collectAs  |
|            |     |            |     |             |     |                 |     | StateWith  |
|            |     |            |     |             |     |                 |     | Lifecycle()|
|            |     |            |     |             |     |                 |     |     |      |
|            |     |            |     |             |     |                 |     |     v      |
|            |     |            |     |             |     |                 |     | Recompose  |
+============+     +============+     +=============+     +=================+     +============+
```

**Data flow is one-way and fully reactive:**

1. Room detects a table change (INSERT, UPDATE, DELETE).
2. Any `Flow<T>` query observing that table automatically re-emits with fresh data.
3. Repository passes the Flow through without transformation (thin layer).
4. ViewModel collects the Flow into a `MutableStateFlow<UiState>`, calling
   `_uiState.update { it.copy(...) }` on each emission.
5. Composable reads `uiState` via `collectAsStateWithLifecycle()` and recomposes
   whenever the StateFlow value changes.

**No manual refresh is ever needed.** When the user saves a memory on the Capture
screen and navigates back to Timeline, the Timeline's Flow automatically emits the
updated list including the new memory.

### 5.4 Error Handling Pattern

```
User action (e.g., save, delete)
   |
   v
ViewModel.onAction()
   |
   v
viewModelScope.launch {
    try {
        repository.doSomething()           // suspend call
        // success -- reactive flow handles UI update
    } catch (e: Exception) {
        _uiState.update { it.copy(error = e.message) }
    }
}
   |
   v (error stored in UiState)
   |
Composable observes uiState.error:

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    uiState.error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = "Dismiss"
            )
            viewModel.clearError()
        }
    }

   |
   v
User sees Snackbar --> dismisses --> clearError() sets error = null
```

**Rules:**
- All write operations (save, update, delete) are wrapped in `try/catch`.
- Errors are stored as `String?` in UiState (not thrown to the UI layer).
- The Composable shows a `Snackbar` when `error` is non-null.
- After the Snackbar is dismissed, `clearError()` resets the error to null.
- Read errors from Flows are handled via `.catch { }` operator.

---

## 6. Location System

### 6.1 Location Capture Flow

```
User taps "Add Location" on Capture screen
   |
   v
+--------------------------------------------+
| Check ACCESS_FINE_LOCATION permission      |
+--------------------------------------------+
   |
   +--- GRANTED ----+
   |                 |
   +--- DENIED -----+--> Check shouldShowRequestPermissionRationale()
                     |         |
                     |    +----+-----+
                     |    | YES      | NO (first request or permanently denied)
                     |    v          v
                     | Show       Request permission
                     | rationale
                     | dialog
                     |    |
                     |    v
                     | Request permission
                     |    |
                     +----+
                          |
                     +----+-----+
                     | GRANTED  | DENIED
                     v          v
              Fetch location   Skip location
                     |         (fields stay null)
                     v
+--------------------------------------------+
| FusedLocationProviderClient                |
|   .getCurrentLocation(                     |
|       Priority.PRIORITY_HIGH_ACCURACY,     |
|       CancellationTokenSource().token      |
|   )                                        |
|                                            |
| Timeout: 10 seconds                        |
+--------------------------------------------+
   |
   +--- SUCCESS ---> viewModel.onLocationObtained(lat, lng)
   |                    |
   |                    v
   |                 _uiState.update {
   |                     it.copy(latitude = lat, longitude = lng)
   |                 }
   |                    |
   |                    v (optional)
   |                 +----------------------------------+
   |                 | Reverse Geocode                   |
   |                 | Geocoder(context)                 |
   |                 |   .getFromLocation(lat, lng, 1)   |
   |                 |   ?.firstOrNull()                 |
   |                 |   ?.let { address ->              |
   |                 |       val label = listOfNotNull(  |
   |                 |           address.locality,       |
   |                 |           address.adminArea        |
   |                 |       ).joinToString(", ")        |
   |                 |       onPlaceLabelChange(label)    |
   |                 |   }                               |
   |                 +----------------------------------+
   |
   +--- FAILURE / TIMEOUT ---> viewModel.onLocationFailed()
                                   |
                                   v
                                Continue without location
                                (latitude, longitude stay null)
```

### 6.2 Location Helper Class

```kotlin
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")  // caller checks permission
    suspend fun getCurrentLocation(): LocationResult {
        return try {
            withTimeout(10_000L) {
                val cancellationSource = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationSource.token
                ).await()

                if (location != null) {
                    LocationResult.Success(location.latitude, location.longitude)
                } else {
                    LocationResult.Unavailable
                }
            }
        } catch (e: TimeoutCancellationException) {
            LocationResult.Timeout
        } catch (e: Exception) {
            LocationResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult()
    data object Unavailable : LocationResult()
    data object Timeout : LocationResult()
    data class Error(val message: String) : LocationResult()
}
```

### 6.3 Map Display System

```
+======================================================================+
|                           MapScreen                                   |
+======================================================================+
|                                                                      |
|  +----------------------------------------------------------------+  |
|  |                     GoogleMap Composable                        |  |
|  |                                                                |  |
|  |    (marker)  (marker)       (marker)                           |  |
|  |       *         *              *                               |  |
|  |                                                                |  |
|  |              (marker)                                          |  |
|  |                 *                                              |  |
|  |                                                                |  |
|  +----------------------------------------------------------------+  |
|                                                                      |
|  +----------------------------------------------------------------+  |
|  |   Preview Bottom Sheet (shown on marker tap)                   |  |
|  |  +----------------------------------------------------------+  |  |
|  |  | [Thumbnail]  Title of Memory                             |  |  |
|  |  |              March 15, 2026 - Happy                      |  |  |
|  |  |              Tap to view details >                       |  |  |
|  |  +----------------------------------------------------------+  |  |
|  +----------------------------------------------------------------+  |
+======================================================================+
```

#### Data Flow for Map

```kotlin
@HiltViewModel
class MapViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            memoryRepository.getGeotaggedMemories()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { memories ->
                    val markers = memories.map { memory ->
                        MapMemoryMarker(
                            memoryId = memory.id,
                            latitude = memory.latitude!!,
                            longitude = memory.longitude!!,
                            title = memory.title,
                            mood = memory.mood,
                            thumbnailPath = null  // loaded separately
                        )
                    }
                    _uiState.update {
                        it.copy(markers = markers, isLoading = false)
                    }
                }
        }
    }

    fun onMarkerClick(memoryId: Long) {
        viewModelScope.launch {
            val detail = memoryRepository.getMemoryWithDetails(memoryId)
            _uiState.update { it.copy(selectedMemory = detail) }
        }
    }

    fun onDismissPreview() {
        _uiState.update { it.copy(selectedMemory = null) }
    }
}
```

#### Camera Auto-Fit

```kotlin
// In MapScreen composable:
val cameraPositionState = rememberCameraPositionState()

LaunchedEffect(markers) {
    if (markers.isNotEmpty()) {
        val boundsBuilder = LatLngBounds.builder()
        markers.forEach { marker ->
            boundsBuilder.include(LatLng(marker.latitude, marker.longitude))
        }
        val bounds = boundsBuilder.build()
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngBounds(bounds, /* padding */ 64)
        )
    }
}
```

---

## 7. Thumbnail System

### 7.1 Generation Strategy

**When:** Thumbnails are generated immediately after a media file is copied to app
storage during the capture save flow. This happens on `Dispatchers.IO`.

**Specification:**

| Property        | Value                                |
|-----------------|--------------------------------------|
| Max dimension   | 300px (longest side)                 |
| Aspect ratio    | Maintained (scaled proportionally)   |
| Format          | JPEG                                 |
| Quality         | 80%                                  |
| Storage path    | `filesDir/thumbnails/{hash}_thumb.jpg` |

#### Image Thumbnail Generation

```kotlin
object ThumbnailUtil {

    private const val THUMBNAIL_MAX_SIZE = 300

    fun generateThumbnail(
        context: Context,
        sourceUri: Uri,
        outputDir: File,
        fileName: String
    ): File? {
        return try {
            val inputStream = context.contentResolver
                .openInputStream(sourceUri) ?: return null

            // Decode with inSampleSize for memory efficiency
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Calculate inSampleSize
            val maxDim = maxOf(options.outWidth, options.outHeight)
            var inSampleSize = 1
            while (maxDim / inSampleSize > THUMBNAIL_MAX_SIZE * 2) {
                inSampleSize *= 2
            }

            // Decode with calculated sample size
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val newStream = context.contentResolver
                .openInputStream(sourceUri) ?: return null
            val sampledBitmap = BitmapFactory.decodeStream(
                newStream, null, decodeOptions
            )
            newStream.close()

            if (sampledBitmap == null) return null

            // Scale to exact max dimension
            val scale = THUMBNAIL_MAX_SIZE.toFloat() /
                maxOf(sampledBitmap.width, sampledBitmap.height)
            val width = (sampledBitmap.width * scale).toInt()
            val height = (sampledBitmap.height * scale).toInt()
            val thumbnail = Bitmap.createScaledBitmap(
                sampledBitmap, width, height, true
            )

            // Save
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, "${fileName}_thumb.jpg")
            FileOutputStream(outputFile).use { out ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            sampledBitmap.recycle()
            thumbnail.recycle()
            outputFile
        } catch (e: Exception) {
            null
        }
    }
}
```

#### Video Thumbnail Generation

```kotlin
fun generateVideoThumbnail(
    context: Context,
    sourceUri: Uri,
    outputDir: File,
    fileName: String
): File? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, sourceUri)
        val frame = retriever.getFrameAtTime(0)    // first frame
        retriever.release()

        if (frame == null) return null

        val scale = THUMBNAIL_MAX_SIZE.toFloat() /
            maxOf(frame.width, frame.height)
        val width = (frame.width * scale).toInt()
        val height = (frame.height * scale).toInt()
        val thumbnail = Bitmap.createScaledBitmap(frame, width, height, true)

        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "${fileName}_thumb.jpg")
        FileOutputStream(outputFile).use { out ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }

        frame.recycle()
        thumbnail.recycle()
        outputFile
    } catch (e: Exception) {
        null
    }
}
```

### 7.2 Loading Strategy with Coil

#### In Lists (Timeline, Search results, Collection detail)

```kotlin
@Composable
fun MemoryThumbnail(
    thumbnailPath: String?,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(thumbnailPath?.let { File(it) })
            .crossfade(true)
            .build(),
        contentDescription = "Memory thumbnail",
        modifier = modifier,
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
        error = painterResource(R.drawable.ic_broken_image)  // or composable fallback
    )
}
```

#### In Detail View (Full-Size)

```kotlin
@Composable
fun FullSizeMediaImage(
    filePath: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(File(filePath))
            .crossfade(true)
            .size(Size.ORIGINAL)
            .build(),
        contentDescription = "Full size media",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
        error = painterResource(R.drawable.ic_broken_image)
    )
}
```

#### Coil Configuration (Application-Level)

```kotlin
@HiltAndroidApp
class MemlyApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)       // 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizePercent(0.05)       // 5% of disk
                    .build()
            }
            .build()
    }
}
```

### 7.3 Regeneration on Missing Thumbnail

If the thumbnail file is missing but the original media file still exists, the
thumbnail can be regenerated on demand.

```
Coil attempts to load thumbnail
   |
   v
+----------------------------+
| File(thumbnailPath).exists()|
+----------------------------+
   |
   +--- EXISTS ---> Load and display normally
   |
   +--- MISSING:
        |
        +--- File(filePath).exists()?
        |         |
        |    +----+-----+
        |    | YES       | NO
        |    v           v
        | Regenerate   Show placeholder
        | thumbnail    (broken image icon)
        | on IO thread
        |    |
        |    v
        | Update MediaFileEntity
        | with new thumbnailPath
        | (if it changed)
        |    |
        |    v
        | Coil retries
        | with new path
```

Implementation in the ViewModel or a helper:

```kotlin
suspend fun ensureThumbnailExists(
    context: Context,
    mediaFile: MediaFileEntity
): String? {
    val thumbFile = mediaFile.thumbnailPath?.let { File(it) }
    if (thumbFile != null && thumbFile.exists()) {
        return mediaFile.thumbnailPath
    }

    // Thumbnail missing -- can we regenerate?
    val sourceFile = File(mediaFile.filePath)
    if (!sourceFile.exists()) return null

    return withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "thumbnails")
        val hash = mediaFile.fileHash

        val generated = when (mediaFile.mediaType) {
            MediaType.PHOTO -> ThumbnailUtil.generateThumbnail(
                context, Uri.fromFile(sourceFile), outputDir, hash
            )
            MediaType.VIDEO -> ThumbnailUtil.generateVideoThumbnail(
                context, Uri.fromFile(sourceFile), outputDir, hash
            )
        }

        generated?.absolutePath
    }
}
```

---

## Appendix: Layer Diagram

```
+=======================================================+
|                    UI Layer                            |
|  (Jetpack Compose screens, composables, navigation)   |
|                                                       |
|  Screens: Timeline, Map, Search, Capture, Detail,     |
|           Collection, Settings, Onboarding            |
+=======================================================+
                        |
                        | collectAsStateWithLifecycle()
                        v
+=======================================================+
|                 ViewModel Layer                        |
|  (StateFlow<UiState>, viewModelScope, Hilt-injected)  |
|                                                       |
|  ViewModels: TimelineVM, MapVM, SearchVM, CaptureVM,  |
|              MemoryDetailVM, CollectionVM, SettingsVM  |
+=======================================================+
                        |
                        | suspend fun / Flow<T>
                        v
+=======================================================+
|                Repository Layer                       |
|  (Business logic, transaction coordination,           |
|   file management, @Singleton)                        |
|                                                       |
|  Repos: MemoryRepository, CollectionRepository        |
|  Utils: FileHashUtil, ThumbnailUtil, LocationHelper   |
+=======================================================+
                        |
                        | suspend fun / Flow<T>
                        v
+=======================================================+
|                 Data Layer                             |
|  (Room DAOs, entities, database)                      |
|                                                       |
|  DAOs: MemoryDao, TagDao, CollectionDao               |
|  DB:   MemlyDatabase (v1, exportSchema = true)        |
|  DI:   DatabaseModule (Hilt @Module)                  |
+=======================================================+
                        |
                        v
+=======================================================+
|              SQLite (memly_database)                   |
|  Tables: memories, media_files, tags, collections,    |
|          memory_tag_cross_ref,                         |
|          memory_collection_cross_ref                   |
+=======================================================+
```

---

## Appendix: Dependency Injection Graph

```
DatabaseModule (@InstallIn SingletonComponent)
   |
   +-- provideDatabase(@ApplicationContext) --> MemlyDatabase @Singleton
   |       |
   |       +-- provideMemoryDao(db)    --> MemoryDao
   |       +-- provideTagDao(db)       --> TagDao
   |       +-- provideCollectionDao(db)--> CollectionDao
   |
   v
MemoryRepository @Singleton
   +-- @Inject constructor(MemoryDao, TagDao)
   |
CollectionRepository @Singleton
   +-- @Inject constructor(CollectionDao)
   |
   v
ViewModels (@HiltViewModel)
   +-- TimelineViewModel @Inject constructor(MemoryRepository)
   +-- CaptureViewModel  @Inject constructor(MemoryRepository, @ApplicationContext)
   +-- MapViewModel       @Inject constructor(MemoryRepository)
   +-- SearchViewModel    @Inject constructor(MemoryRepository)
   +-- MemoryDetailViewModel @Inject constructor(SavedStateHandle, MemoryRepository)
   +-- CollectionViewModel   @Inject constructor(CollectionRepository, MemoryRepository)
   +-- SettingsViewModel     @Inject constructor(MemoryRepository, @ApplicationContext)
```
