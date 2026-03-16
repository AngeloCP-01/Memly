# Memly - Project Overview

## Purpose
Offline-first Android personal memory journal app. Captures photos, videos, notes with emotion/mood tagging, GPS location, and custom tags. Explore memories via timeline, map, and search. Free, no ads, no accounts, 100% offline. Phase 4 adds optional Firebase cloud sync.

## Current State
- **Phase 1 IN PROGRESS** | Section 1 (Project Setup) complete | Next: Section 2 (Memory Capture)
- 6 phases planned, ~305 tasks total

## Tech Stack
- Kotlin 2.0.21, Jetpack Compose, Material 3, Room, Hilt, Navigation Compose
- Coroutines + Flow, Coil 3, AGP 9.0.1, KSP 2.0.21-1.0.28
- compileSdk 36, minSdk 28, targetSdk 36

## Architecture
MVVM with Repository pattern (2 layers, NO Clean Architecture / domain layer):
- **Data**: Room entities, DAOs, Repositories
- **UI/Presentation**: Composables, ViewModels, theme
- **DI**: DatabaseModule (Hilt)
- ViewModels call Repositories directly (no UseCases)
- Entities used directly (no separate domain models)

## Structure
```
app/src/main/java/com/example/memly/
├── MemlyApplication.kt, MainActivity.kt
├── data/
│   ├── local/database/  → MemlyDatabase
│   ├── local/dao/       → MemoryDao, TagDao, CollectionDao
│   ├── local/entity/    → MemoryEntity, MediaFileEntity, TagEntity, CollectionEntity, Mood, MediaType, CrossRefs, MemoryWithDetails
│   └── repository/      → MemoryRepository, CollectionRepository
├── ui/
│   ├── theme/           → Color, Theme, Type, Shape, MoodTheme
│   ├── navigation/      → Screen, MemlyNavGraph
│   ├── timeline/        → TimelineScreen, TimelineViewModel
│   ├── map/             → MapScreen (placeholder)
│   ├── capture/         → CaptureScreen (placeholder)
│   ├── search/          → SearchScreen (placeholder)
│   ├── detail/          → MemoryDetailScreen (placeholder)
│   └── components/      → (shared composables, TBD)
├── di/                  → DatabaseModule
└── util/                → FileHashUtil, ThumbnailUtil
```

## Key Conventions
- Package: `com.example.memly`
- PascalCase classes, camelCase functions, SCREAMING_SNAKE constants
- Entities: `@Entity(tableName = "snake_case")`, PK with `autoGenerate = true`, default `id: Long = 0`
- DAOs: suspend for writes, Flow for reads
- Repositories: `@Singleton @Inject constructor`
- ViewModels: `@HiltViewModel`, expose `StateFlow<UiState>`
- Screens: `@Composable fun FeatureScreen(...)`, VM via `hiltViewModel()`
- Enums stored as strings in Room
- Conventional Commits: type(scope): description

## Design
- Light-first theme, Soft Coral #FF6B6B primary, Warm Beige #FFF3E6 surface, Soft Teal #4BC0C8 accent
- Typography: Poppins (headings), Inter (body)
- Image-dominant memory cards with gradient overlay, 3:4 portrait aspect ratio
- Bottom nav: Timeline, Map, Search + FAB for Capture
