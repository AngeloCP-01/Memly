# Established Code Patterns

## Build

- AGP 9.0.1 with built-in Kotlin support (no separate Kotlin plugin)
- KSP 2.0.21-1.0.28 for annotation processing (not KAPT)
- Hilt 2.59.2 (minimum version supporting AGP 9)
- Gradle property: `android.disallowKotlinSourceSets=false` required for KSP compatibility with AGP 9

## Navigation

- `Screen` sealed class with string route properties
- Bottom nav shown on main tabs (Timeline, Collections, Map, Settings)
- Bottom nav hidden on full-screen routes (Detail, Capture)
- Bottom nav bar has a permanent curved cutout in the center for the FAB
- FAB overlaps the bar via `offset(y = (-22).dp)`, uses `AnimatedVisibility` with scale animation
- FAB visible on Timeline (→ Capture) and CollectionList (→ create collection dialog); hidden on Map/Settings
- Per-screen FAB action wired via `when(currentRoute)` in `onAddClick` callback
- Collection create triggered via incrementing `Int` counter passed through NavGraph → `LaunchedEffect`
- `Screen.Search` removed — search is inline on Timeline and Collections screens
- Top-level nav destinations use custom headers (no TopAppBar/back arrow); only sub-screens use back navigation

## Data Layer

- Entities annotated with `@Entity(tableName = "snake_case")`
- Primary keys use `autoGenerate = true`
- Foreign keys declared in `@Entity` with matching `@ColumnInfo` indices
- DAOs use `suspend` functions for writes (insert, update, delete)
- DAOs return `Flow` for reads (queries)
- Enums (Mood, MediaType) stored as strings via TypeConverters

## Repositories

- Annotated with `@Singleton`
- Constructor-injected with `@Inject constructor`
- Expose `Flow` for read operations
- Expose `suspend` functions for write operations

## Presentation

- ViewModels annotated with `@HiltViewModel` and use `@Inject constructor`
- Expose UI state via `StateFlow`
- Screens receive ViewModel via `hiltViewModel()`

## Dependency Injection

- Hilt modules are `object` classes
- Provide dependencies with `@Provides` functions
- Installed in `@InstallIn(SingletonComponent::class)`

## Relation Queries

- `MemoryWithDetails` data class using:
  - `@Embedded` for the parent entity
  - `@Relation` for direct child entities
  - `@Junction` for many-to-many relationships
  - DAO queries annotated with `@Transaction`

## Capture Flow

- CaptureViewModel extends `ViewModel` with `@ApplicationContext` injection for file I/O
- Media items tracked as `MediaItem(uri, mediaType, displayName)` in UiState
- File operations (hash, copy, thumbnail) run on `Dispatchers.IO` via `withContext`
- Repository's `createMemoryWithDetails()` wraps inserts in `database.withTransaction {}`
- Camera capture uses `TakePicture` contract + `FileProvider` (not CameraX)
- Gallery uses `PickMultipleVisualMedia` contract
- Location uses `FusedLocationProviderClient.getCurrentLocation()` with cancellation token
- Duplicate media detected by SHA-256 hash before insert

## File Management (MediaStore-Based — Implemented)

- **In-app content** (camera, audio) → saved to public storage via MediaStore API: `Pictures/Memly/`, `Movies/Memly/`, `Music/Memly/`
- **Picked content** (gallery) → user chooses: reference only (`EXTERNAL`) or copy to Memly (`IMPORTED`)
- **MediaSource enum:** `APP_OWNED` (created in-app), `EXTERNAL` (URI reference), `IMPORTED` (user copied to Memly)
- **URI resolution:** PhotoPicker URI → resolve to MediaStore content URI via Strategy 1 (name+size match) or Strategy 2 (direct _ID). Requires `READ_MEDIA_IMAGES` runtime permission. Fallback: `takePersistableUriPermission()` for cloud-backed providers
- **Broken reference detection:** `openInputStream()` check (not `query()`) on all media sources. Timeline cards use `LaunchedEffect` + `Dispatchers.IO` readability check with broken image fallback UI
- **File naming:** `memly_<yyyyMMdd_HHmmss>_<shortId>.<ext>`
- **Metadata cached in entity:** `mimeType`, `size`, `dateTaken`, `width`, `height` — avoids repeated MediaStore queries
- **Dedup:** SHA-256 hash. On hash match → create new `MediaFileEntity` row reusing existing URI/metadata (no disk duplication, same photo allowed in multiple memories)
- **Deletion:** APP_OWNED/IMPORTED → ContentResolver delete (createDeleteRequest fallback on Android 11+). EXTERNAL → DB row only
- **Thumbnails** still in `cacheDir/thumbnails/` (app-private, regenerable)
- Camera temp photos stored in `cacheDir/camera/`
- FileProvider paths defined in `res/xml/file_paths.xml`
- Video thumbnails extracted via `MediaMetadataRetriever.getFrameAtTime()`
- **MediaStoreManager** utility class: single source of truth for all public storage I/O

## Voice Memos / Audio

- `AudioRecorder` utility wraps `MediaRecorder` lifecycle; records to `cacheDir/audio/` temp file
- Audio format: AAC encoder, MPEG-4 container (`.m4a`), 128kbps, 44.1kHz
- Recorded audio saved as `APP_OWNED` to `Music/Memly/` via `MediaStoreManager.insertMedia()`
- `AudioPlaybackBar` composable: play/pause with `MediaPlayer`, progress bar, elapsed/total time
- `MediaPlayer` released via `DisposableEffect` `onDispose`
- `durationMs` field on `MediaFileEntity` — cached at save time via `MediaMetadataRetriever`
- Audio files excluded from image pagers/slideshows — filter with `mediaType != MediaType.AUDIO`
- Audio indicator: `Icons.Default.Mic` badge on cards; `AudioPlaybackBar` on DetailScreen below photo hero
- `ThumbnailUtil.generateThumbnail()` returns `null` for `AUDIO` — no visual thumbnail

## Timeline

- `TimelineUiState` contains `allMemories`, `groups: List<TimelineGroup>`, and `timeHopMemories`
- `TimelineGroup(header, memories)` — date-grouped sections
- Grouping logic: Today → Yesterday → day name (within week) → "MMMM yyyy" (older)
- `combine()` merges all memories + grouped memories + Time Hop into single UiState
- Time Hop DAO query uses `strftime('%m-%d', ...)` for cross-year date matching
- Homescreen layout: ProfileHeader → SearchBar+FilterIcon → ActiveFilterChips → MemoryPager (vertically scrollable)
- Memory pager uses `HorizontalPager` with coverflow effect: centered card, side cards rotated on Y-axis
- Coverflow achieved via `graphicsLayer { rotationY, cameraDistance, scaleX/Y, translationX }` per page offset
- Side cards: 85% scale, 15deg Y-rotation, 50% alpha, 30px translation — creates 3D perspective peek
- Each memory card has an auto-sliding image slideshow (nested `HorizontalPager`, `userScrollEnabled = false`)
- Slideshow only runs on the active/current page (`isActive` flag based on pager state)
- Slideshow auto-advances every 4s with 600ms slide animation; resets to image 0 on page change
- Card bottom row: `@location` left + "See more +" frosted glass pill right, in a single `Row` with `SpaceBetween`

## Shared Components

- `MemoryCard` — image-dominant timeline card in `ui/components/MemoryCard.kt`
- `MemoryCarouselCard` — compact card for horizontal carousels
- `MemorySearchResultCard` — horizontal row card for search/collection screens
- All cards accept `Modifier` parameter and use `remember` for date formatters
- Press animation via `pointerInput` + `detectTapGestures` + `animateFloatAsState(0.97f)`

## Detail Screen

- `MemoryDetailViewModel` loads via `SavedStateHandle["memoryId"]`
- `DetailUiState` contains both read and edit fields (edit prefixed with `edit*`)
- Edit mode: `startEditing()` copies current values to edit fields; `cancelEditing()` discards
- Tag changes computed as diff: remove old tags not in new set, add new tags not in old set
- Delete uses Room FK cascade (no manual cleanup of media/tags needed)
- Photo hero uses `HorizontalPager` with page indicator dots
- Back button overlaid as semi-transparent circle on image

## Map Screen

- osmdroid `MapView` wrapped in `AndroidView` for Compose integration
- `Configuration.getInstance().userAgentValue` set to app package name (required by osmdroid)
- Tile cache stored in `cacheDir/osmdroid/`
- Custom mood-colored pin markers via `Bitmap` + `Canvas` drawing (circle + triangle pointer)
- Map auto-centers on average lat/lng of visible memories with adaptive zoom
- Mood filter chips overlay on top of map with `FlowRow`
- Preview card slides in from bottom on pin tap using `AnimatedVisibility`
- `getGeotaggedMemoriesWithDetails()` returns `Flow<List<MemoryWithDetails>>` with `@Transaction`

## Inline Search & Filter (Timeline)

- Search integrated into Timeline screen via `BasicTextField` inside styled `Surface` (no separate search screen)
- `TimelineViewModel` uses `debounce(300)` + `flatMapLatest` for reactive search against `searchMemoriesWithDetails()`
- Multi-select mood filter: `Set<Mood>` toggled via `toggleMoodFilter(mood)`; client-side filtering in `combine`
- Date filter: specific day via Material 3 `DatePickerDialog`; filtered with `isSameDay()` comparison
- Filter icon button next to search bar; turns primary-colored when filters active
- Active filter chips shown below search bar (horizontally scrollable): mood chips with color + X, date chip with calendar icon + X
- `DropdownMenu` with two sections: "Filter by Date" (date picker trigger) and "Filter by Mood" (submenu with checkmarks + Done button)
- Search results replace the memory pager content (same coverflow pager, not a separate list)

## Inline Search (Collections)

- `CollectionListViewModel` uses `debounce(300)` + `flatMapLatest` switching between `getAllCollections()` and `searchCollections(query)`
- `CollectionDao.searchCollections()` uses LIKE query matching name and description
- Search bar styled identically to Timeline search bar (`BasicTextField` + `Surface`)

## Collections

- `CollectionListScreen` is a top-level nav destination (no back arrow); uses custom header with icon + title (create button moved to bottom nav FAB)
- Collection cards use `RoundedCornerShape(20.dp)`, `surfaceContainerHigh` color, icon in `primaryContainer` circle
- `CollectionListViewModel` uses `flatMapLatest` + `combine` to merge collection list with per-collection memory counts
- `CollectionDetailViewModel` loads via `SavedStateHandle["collectionId"]`
- Add-to-collection flow lives on `MemoryDetailScreen` — toggle dialog with checkmarks
- `MemoryDetailViewModel` injects both `MemoryRepository` and `CollectionRepository`
- Collection membership loaded via `getCollectionIdsForMemory()` on dialog open (not continuously observed)
- FK CASCADE on `MemoryCollectionCrossRef` handles cleanup when collection or memory is deleted

## Settings Screen

- `SettingsScreen` is a top-level nav destination (no back arrow); uses custom header with gear icon + title
- Cards use `RoundedCornerShape(20.dp)` and `surfaceContainerHigh` matching other redesigned screens
- `SettingsViewModel` injects `@ApplicationContext Context`, `MemlyDatabase`, and `MemoryDao` directly
- Storage stats loaded on `Dispatchers.IO` via `withContext`; disk usage calculated with `File.walkTopDown()`
- `database.clearAllTables()` for Room data wipe; `File.deleteRecursively()` for media + thumbnails
- Double confirmation pattern: first dialog warns, second shows exact counts and requires explicit "Delete Everything"
- `BuildConfig.VERSION_NAME` and `VERSION_CODE` accessed after enabling `buildFeatures { buildConfig = true }`

## Error Handling Pattern

- All ViewModels with suspend operations wrap them in `try/catch`
- Error exposed as `error: String?` field in UiState
- Screens display errors via `LaunchedEffect(uiState.error)` + `SnackbarHostState.showSnackbar()`
- After showing, call `viewModel.clearError()` to reset
- Flow-based ViewModels (Timeline, Map, Search) rely on Room's internal error handling — Room flows don't throw
- CaptureViewModel is the exemplar: has error, loading, validation, and file I/O error handling
