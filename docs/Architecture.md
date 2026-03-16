# Memly Architecture Document

## 1. Architecture Overview

Memly follows the **MVVM (Model-View-ViewModel) with Repository pattern**, as recommended by Android's official architecture guidelines. The app is local-first and offline-first -- all data lives on-device in a Room (SQLite) database, with no network layer.

```
┌─────────────────────────────────────────────────────┐
│                       UI Layer                      │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐       │
│  │ Composable│  │ Composable│  │ Composable│  ...   │
│  │  Screens  │  │  Screens  │  │  Screens  │       │
│  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘       │
│        │               │               │            │
│  ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐      │
│  │ ViewModel │  │ ViewModel │  │ ViewModel │  ...  │
│  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘      │
└────────┼───────────────┼───────────────┼────────────┘
         │               │               │
┌────────▼───────────────▼───────────────▼────────────┐
│                   Data Layer                        │
│  ┌──────────────────────────────────────────────┐   │
│  │              Repositories                     │   │
│  │  (MemoryRepository, CollectionRepository)     │   │
│  └──────────────────┬───────────────────────────┘   │
│                     │                               │
│  ┌──────────────────▼───────────────────────────┐   │
│  │            Room Database                      │   │
│  │  ┌──────────┐  ┌──────────┐  ┌────────────┐  │   │
│  │  │MemoryDao │  │  TagDao  │  │CollectionDao│  │   │
│  │  └──────────┘  └──────────┘  └────────────┘  │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

## 2. Package Structure

```
com.example.memly/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── MemoryDao.kt
│   │   │   ├── TagDao.kt
│   │   │   └── CollectionDao.kt
│   │   ├── entity/
│   │   │   ├── MemoryEntity.kt
│   │   │   ├── MediaFileEntity.kt
│   │   │   ├── TagEntity.kt
│   │   │   ├── CollectionEntity.kt
│   │   │   ├── MemoryTagCrossRef.kt
│   │   │   ├── MemoryCollectionCrossRef.kt
│   │   │   └── Mood.kt
│   │   └── MemlyDatabase.kt
│   └── repository/
│       ├── MemoryRepository.kt
│       └── CollectionRepository.kt
├── domain/
│   └── model/
│       (Domain models, used if mapping from entities is needed)
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   ├── MemlyNavHost.kt
│   │   ├── Screen.kt
│   │   └── BottomNavBar.kt
│   ├── timeline/
│   │   ├── TimelineScreen.kt
│   │   └── TimelineViewModel.kt
│   ├── map/
│   │   ├── MapScreen.kt
│   │   └── MapViewModel.kt
│   ├── capture/
│   │   ├── CaptureScreen.kt
│   │   └── CaptureViewModel.kt
│   ├── search/
│   │   ├── SearchScreen.kt
│   │   └── SearchViewModel.kt
│   ├── detail/
│   │   ├── DetailScreen.kt
│   │   └── DetailViewModel.kt
│   └── components/
│       ├── MemoryCard.kt
│       ├── TagChip.kt
│       ├── MoodSelector.kt
│       └── MediaThumbnail.kt
├── di/
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
├── util/
│   ├── FileHashUtil.kt
│   └── ThumbnailUtil.kt
├── MemlyApplication.kt
└── MainActivity.kt
```

## 3. Layer Responsibilities

### UI Layer (Composable Screens)

- Renders UI state provided by the ViewModel.
- Delegates all user actions (clicks, text input, navigation triggers) to the ViewModel.
- Contains zero business logic -- purely declarative rendering via Jetpack Compose.
- Observes ViewModel state using `collectAsState()` on `StateFlow` or `Flow`.

### ViewModel Layer

- Holds and manages UI state via `StateFlow` or `MutableStateFlow`.
- Calls repository methods in response to user actions.
- Scoped to a navigation destination (survives configuration changes).
- Converts raw data from repositories into UI-ready state objects.
- Launches coroutines in `viewModelScope` for all async work.

### Repository Layer

- Single source of truth for a given domain concern (e.g., `MemoryRepository` owns all memory-related data operations).
- Abstracts the data source from the ViewModel -- ViewModels never interact with DAOs directly.
- Coordinates between multiple DAOs when a single operation spans entities (e.g., saving a memory with tags).
- Exposes data as `Flow<T>` for reactive observation and `suspend` functions for one-shot operations.

### DAO / Database Layer

- Room DAOs define SQL queries via annotations (`@Query`, `@Insert`, `@Update`, `@Delete`).
- `MemlyDatabase` is the Room abstract database class, declaring all DAOs and entity tables.
- Handles schema migrations.
- Enforces referential integrity through foreign keys defined on entities.

## 4. Data Flow

### Read Path (displaying data)

```
Database (SQLite)
    │
    ▼
DAO returns Flow<List<Entity>>
    │
    ▼
Repository exposes Flow<List<Entity>> (or maps to domain models)
    │
    ▼
ViewModel collects Flow, maps to UI state, exposes StateFlow<UiState>
    │
    ▼
Composable calls collectAsState(), renders UI
```

Data flows reactively. When the underlying database table changes, Room automatically emits a new value through the `Flow`, which propagates up through the repository and ViewModel to the UI without any manual refresh logic.

### Write Path (creating/updating data)

```
User action in Composable
    │
    ▼
Composable calls ViewModel function (e.g., saveMemory())
    │
    ▼
ViewModel launches coroutine in viewModelScope, calls repository suspend function
    │
    ▼
Repository calls DAO suspend function(s), possibly in a @Transaction
    │
    ▼
DAO executes SQL INSERT/UPDATE/DELETE on Room database
    │
    ▼
Room notifies all active Flow queries on affected tables (automatic)
```

## 5. Key Design Decisions

### Why MVVM?

- It is the architecture officially recommended by Google for Android apps.
- Clean separation of concerns: UI knows nothing about data, data knows nothing about UI.
- ViewModels survive configuration changes (screen rotation), preventing data loss and redundant loading.
- Works naturally with Jetpack Compose's state-driven rendering model.

### Why Room?

- Type-safe SQL queries verified at compile time.
- First-class support for Kotlin coroutines and Flow, enabling reactive data observation.
- Handles SQLite boilerplate (cursor management, schema creation, migrations).
- Built-in support for complex queries, joins, and transactions needed for many-to-many relationships (Memory-Tag, Memory-Collection).

### Why Hilt?

- Official Android dependency injection library, built on Dagger.
- Provides predefined scopes that align with Android component lifecycles (`@Singleton`, `@ViewModelScoped`, `@ActivityScoped`).
- Eliminates manual dependency wiring -- the database, DAOs, and repositories are provided automatically.
- Integrates directly with ViewModel injection via `@HiltViewModel`.

### Why Coil?

- Kotlin-first image loading library, designed for coroutines.
- Lightweight compared to Glide, with a smaller API surface.
- First-class Compose support via `AsyncImage`.
- Handles caching, memory management, and placeholder/error states out of the box.

### Why Local-First / Offline-First?

- Memly is a personal memory app. User data stays on the device -- no cloud dependency, no account required.
- Zero latency for all operations; no loading spinners waiting on network calls.
- Full functionality without an internet connection.
- Simplifies the architecture by removing the need for a network layer, sync logic, or conflict resolution.

## 6. Navigation Structure

Memly uses Jetpack Navigation Compose with a bottom navigation bar as the primary navigation pattern.

```
┌─────────────────────────────────────────────┐
│                 MainActivity                │
│  ┌───────────────────────────────────────┐  │
│  │            MemlyNavHost               │  │
│  │                                       │  │
│  │  ┌─────────┐  ┌─────────┐            │  │
│  │  │Timeline │  │  Detail  │            │  │
│  │  │ Screen  │──▶│  Screen │            │  │
│  │  └─────────┘  └─────────┘            │  │
│  │  ┌─────────┐                         │  │
│  │  │   Map   │                         │  │
│  │  │ Screen  │                         │  │
│  │  └─────────┘                         │  │
│  │  ┌─────────┐                         │  │
│  │  │ Search  │                         │  │
│  │  │ Screen  │                         │  │
│  │  └─────────┘                         │  │
│  │  ┌─────────┐                         │  │
│  │  │Settings │                         │  │
│  │  │ Screen  │                         │  │
│  │  └─────────┘                         │  │
│  │  ┌─────────┐                         │  │
│  │  │ Capture │ (full-screen overlay)   │  │
│  │  │ Screen  │                         │  │
│  │  └─────────┘                         │  │
│  └───────────────────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │           Bottom Nav Bar              │  │
│  │  Timeline  │  Map  │ Search │Settings │  │
│  └───────────────────────────────────────┘  │
│         ┌─────────────────┐                 │
│         │  FAB (Capture)  │                 │
│         └─────────────────┘                 │
└─────────────────────────────────────────────┘
```

### Navigation Routes

| Route           | Description                          | Bottom Nav Tab |
|-----------------|--------------------------------------|----------------|
| `timeline`      | Chronological feed of memories       | Timeline       |
| `map`           | Map view with memory location pins   | Map            |
| `search`        | Full-text and tag-based search       | Search         |
| `settings`      | App preferences and data management  | Settings       |
| `capture`       | Create a new memory (FAB trigger)    | --             |
| `detail/{id}`   | View/edit a single memory            | --             |

The bottom navigation bar uses `NavController.navigate()` with `launchSingleTop = true` and `popUpTo(startDestination)` to avoid stacking duplicate destinations. The Capture screen is triggered by a Floating Action Button (FAB) and opens as a full-screen destination outside the bottom nav.

## 7. Dependency Graph

```
MainActivity
    │
    ▼
MemlyNavHost
    │
    ├── TimelineScreen ──▶ TimelineViewModel ──▶ MemoryRepository ──┐
    ├── MapScreen ────────▶ MapViewModel ────────▶ MemoryRepository ──┤
    ├── SearchScreen ─────▶ SearchViewModel ─────▶ MemoryRepository ──┤
    ├── CaptureScreen ────▶ CaptureViewModel ────▶ MemoryRepository ──┤
    └── DetailScreen ─────▶ DetailViewModel ─────▶ MemoryRepository ──┤
                                                  CollectionRepository┤
                                                                      │
                                                                      ▼
                                                               MemlyDatabase
                                                        ┌──────────┼──────────┐
                                                        ▼          ▼          ▼
                                                   MemoryDao    TagDao   CollectionDao
```

### Hilt Module Provision

```
DatabaseModule (@Singleton)
    ├── provides MemlyDatabase
    ├── provides MemoryDao    (from database)
    ├── provides TagDao       (from database)
    └── provides CollectionDao (from database)

RepositoryModule (@Singleton)
    ├── provides MemoryRepository    (depends on MemoryDao, TagDao)
    └── provides CollectionRepository (depends on CollectionDao)
```

All ViewModels are annotated with `@HiltViewModel` and receive their repository dependencies via constructor injection. Hilt manages the entire object graph from the `MemlyApplication` class (annotated with `@HiltAndroidApp`) down to individual ViewModels.

## 8. Threading Strategy

Memly uses Kotlin coroutines and Flow as its concurrency framework. There are no raw threads, `AsyncTask` calls, or callback-based patterns.

### Coroutine Scopes

| Scope              | Owner          | Lifetime                        | Usage                                |
|--------------------|----------------|---------------------------------|--------------------------------------|
| `viewModelScope`   | ViewModel      | Survives config changes, cleared on ViewModel destruction | All ViewModel-initiated async work |
| Room's internal dispatcher | Room | Per-query | DAO suspend functions and Flow emissions |

### Reactive Data with Flow

- **DAOs** return `Flow<T>` for all read queries. Room handles the observation internally -- whenever a table row changes, Room re-executes the query and emits the new result.
- **Repositories** expose these `Flow<T>` instances directly (or map/combine them).
- **ViewModels** collect repository flows using `stateIn()` to convert them to `StateFlow<UiState>`, providing an initial value and a sharing strategy (`SharingStarted.WhileSubscribed(5000)`). The 5-second timeout prevents restarting the flow during brief configuration changes.
- **Composables** observe the `StateFlow` via `collectAsState()`.

### Suspend Functions for Writes

All write operations (insert, update, delete) are `suspend` functions on DAOs and repositories. They are called from `viewModelScope.launch {}` blocks. Room automatically dispatches these to a background thread, so no explicit dispatcher switching (`withContext(Dispatchers.IO)`) is needed for Room operations.

### File I/O

File operations (hashing, thumbnail generation, file copying) run on `Dispatchers.IO` via `withContext(Dispatchers.IO) {}` since they are not managed by Room.

## 9. File Management Architecture

Memly stores references to media files rather than embedding binary data in the database. This keeps the database small and performant while supporting photos, videos, and audio.

### Reference-First Approach

Media files are stored in the app's internal storage directory. The database stores only the file path (as a string field on `MediaFileEntity`), not the file contents. This means:

- The database remains small regardless of how many photos or videos a user attaches.
- File I/O is decoupled from database transactions.
- Files can be managed (moved, backed up) independently of the database.

### Hash-Based Deduplication

When a user attaches a media file to a memory, the app computes a SHA-256 hash of the file contents using `FileHashUtil`.

```
User selects file
    │
    ▼
FileHashUtil.computeHash(file) -> SHA-256 hex string
    │
    ▼
Query: Does a MediaFileEntity with this hash already exist?
    │
    ├── YES: Reuse existing file path, create new association
    │
    └── NO:  Copy file to internal storage, create new MediaFileEntity
```

This prevents storing duplicate copies of the same image or video, saving device storage. The hash is stored as a column on `MediaFileEntity` and indexed for fast lookup.

### Thumbnail Generation

`ThumbnailUtil` generates smaller preview images for photos and video keyframes. Thumbnails are used in list views (Timeline, Search results) to avoid loading full-resolution images into memory.

- Thumbnails are generated asynchronously on `Dispatchers.IO` when a media file is first attached.
- Thumbnail file paths are stored on `MediaFileEntity` alongside the original file path.
- Coil handles loading thumbnails in Compose UI with built-in memory and disk caching.
- If a thumbnail is missing (e.g., cleared from cache), it is regenerated on demand.

### File Storage Layout

```
app-internal-storage/
├── media/
│   ├── {hash1}.jpg
│   ├── {hash2}.mp4
│   └── {hash3}.aac
└── thumbnails/
    ├── {hash1}_thumb.jpg
    └── {hash2}_thumb.jpg
```

Files are named by their content hash, which naturally prevents collisions and makes deduplication straightforward. Original file metadata (original filename, MIME type, dimensions) is stored in the `MediaFileEntity` database record.
