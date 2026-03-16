# Changelog

All notable changes to Memly are documented here, organized by phase and section.

---

## Phase 1, Section 2: Memory Capture (Core)
**Date:** 2026-03-16

### Added
- CaptureViewModel with CaptureUiState (title, notes, mood, media, location, tags, date, loading/error states)
- Full CaptureScreen form: scrollable layout with media, text, mood, location, tags, date sections
- Photo/video selection via Photo Picker API (PickMultipleVisualMedia)
- Camera capture via TakePicture contract with FileProvider and permission handling
- Mood/emotion selector using FilterChip grid with mood-colored backgrounds
- GPS location capture via FusedLocationProviderClient with fine/coarse permission flow
- Place label text input field
- Tag input with FlowRow chip display and add/remove support
- Date picker using Material 3 DatePickerDialog
- Transactional save flow (MemoryEntity + MediaFileEntity + TagEntity in Room transaction)
- SHA-256 file hash computation with duplicate media detection
- Video thumbnail generation via MediaMetadataRetriever frame extraction
- FileProvider configuration (file_paths.xml) for camera capture URIs
- play-services-location dependency for GPS

### Fixed
- Compose BOM updated from 2024.09.00 to 2025.05.00 (fixed FlowRow runtime crash from foundation version mismatch)
- Fixed hiltViewModel import path in TimelineScreen and CaptureScreen (`androidx.hilt.navigation.compose`)
- CaptureViewModel: replaced AndroidViewModel with ViewModel + @ApplicationContext
- CaptureViewModel: replaced isNullOrBlank() with isBlank() on non-nullable strings

### Changed
- MemoryRepository: added createMemoryWithDetails() transactional method, injected MemlyDatabase
- ThumbnailUtil: refactored to support both image and video thumbnails via MediaType parameter

---

## Phase 1, Section 1: Project Setup & Configuration
**Date:** 2026-03-16

### Added
- Android project with Kotlin 2.0.21 + Jetpack Compose + AGP 9.0.1
- Room database with 6 entities: Memory, MediaFile, Tag, Collection, MemoryTag, MemoryCollection
- 3 DAOs: MemoryDao, TagDao, CollectionDao with full CRUD + reactive Flow queries
- 2 Repositories: MemoryRepository, CollectionRepository
- Hilt DI setup: DatabaseModule providing database + DAOs
- Navigation scaffold: bottom nav (Timeline, Map, Search) + FAB for Capture
- Placeholder screens: Timeline (with ViewModel), Map, Search, Capture, MemoryDetail
- Utility classes: FileHashUtil (SHA-256), ThumbnailUtil
- Mood and MediaType enums
- MemoryWithDetails relation class for loading memories with media files and tags

### Docs
- docs/PRD.md — Product Requirements Document
- docs/AppDescription.md — Organized brainstorm reference
- docs/Architecture.md — System architecture document
