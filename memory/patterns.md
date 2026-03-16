# Established Code Patterns

## Build

- AGP 9.0.1 with built-in Kotlin support (no separate Kotlin plugin)
- KSP 2.0.21-1.0.28 for annotation processing (not KAPT)
- Hilt 2.59.2 (minimum version supporting AGP 9)
- Gradle property: `android.disallowKotlinSourceSets=false` required for KSP compatibility with AGP 9

## Navigation

- `Screen` sealed class with string route properties
- Bottom nav shown on main tabs (Timeline, Map, Search)
- Bottom nav hidden on full-screen routes (Detail, Capture, Settings)

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

## File Management

- Media files copied to `filesDir/media/` with UUID filenames
- Thumbnails stored in `cacheDir/thumbnails/`
- Camera temp photos stored in `cacheDir/camera/`
- FileProvider paths defined in `res/xml/file_paths.xml`
- Video thumbnails extracted via `MediaMetadataRetriever.getFrameAtTime()`

## Timeline

- `TimelineUiState` contains `groups: List<TimelineGroup>` and `timeHopMemories`
- `TimelineGroup(header, memories)` — date-grouped sections
- Grouping logic: Today → Yesterday → day name (within week) → "MMMM yyyy" (older)
- `combine()` merges grouped memories + Time Hop into single UiState
- Sticky headers use `Surface` with opaque background to prevent text overlap on scroll
- Time Hop DAO query uses `strftime('%m-%d', ...)` for cross-year date matching

## Shared Components

- `MemoryCard` — image-dominant timeline card in `ui/components/MemoryCard.kt`
- `MemoryCarouselCard` — compact card for horizontal carousels
- `MemorySearchResultCard` — horizontal row card for search/collection screens
- All cards accept `Modifier` parameter and use `remember` for date formatters
- Press animation via `pointerInput` + `detectTapGestures` + `animateFloatAsState(0.97f)`
