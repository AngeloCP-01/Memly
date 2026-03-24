# Changelog

All notable changes to Memly are documented here, organized by phase and section.

---

## Ad-hoc: Collection Detail Grid + Memory Detail Bottom Sheet Redesign
**Date:** 2026-03-24

### Changed
- **CollectionDetailScreen**: Replaced vertical list with 2-column grid (`LazyVerticalGrid`) using `StackedPhotoCard` composable — up to 3 photos stacked at random slight angles (polaroid style) with white borders, shadows, and title/date/location below.
- **MemoryDetailScreen**: Complete redesign from `Scaffold` to `BottomSheetScaffold`. Media fills the full screen as a carousel (`HorizontalPager` with `contentPadding` and `pageSpacing` for card gaps). Details live in a draggable bottom sheet (peek 160dp, drag up for full details). Auto-expands in edit mode.
- **Mood-based theming**: Detail screen FAB, chips, tags, location pill, and sheet background all derive from the memory's mood color. Sheet background uses `lerp(surface, moodColor, 0.08f)` for opaque tinted color.
- **Image display**: `ContentScale.FillWidth` with `clip(RoundedCornerShape(20.dp))` — landscape images display in full without cropping, with visible rounded corners.

---

## Ad-hoc: Custom MemlyToast (replaces Snackbar)
**Date:** 2026-03-19

### Added
- **MemlyToast composable**: Custom centered overlay toast in `ui/components/MemlyToast.kt`. Displays messages in the center of the screen with fade+scale animation. Auto-dismisses after 3 seconds. Supports error (red errorContainer) and success (teal secondaryContainer) styles.

### Changed
- **SettingsScreen**: Replaced `SnackbarHost` with `MemlyToast`. Success messages (clear data) use teal style, errors use red style.
- **CaptureScreen**: Replaced `SnackbarHost` with `MemlyToast` for error display.
- **MemoryDetailScreen**: Replaced `SnackbarHost` with `MemlyToast` for error display.
- **CollectionDetailScreen**: Replaced `SnackbarHost` with `MemlyToast` for error display.
- **CollectionListScreen**: Replaced `SnackbarHost` with `MemlyToast` for error display.

### Fixed
- Snackbar messages were rendered behind the bottom navigation bar, making them invisible or hard to read.

---

## Phase 2, Section 4: Onboarding Flow
**Date:** 2026-03-19

### Added
- **OnboardingScreen**: 3-page HorizontalPager introducing the app (Welcome, Capture with Emotion, Explore Your Story). Each page has an icon in a primaryContainer circle, title, subtitle, and description.
- **DataStore preferences**: `OnboardingPreferences` class using `datastore-preferences` (1.1.7) for `onboarding_completed` boolean flag. Provided as Hilt singleton.
- **Permission requests**: Bulk request on onboarding completion/skip — camera, location, audio, and media read/write permissions (API-level-aware).
- **"Capture Your First Memory" CTA**: Primary button on final page navigates directly to CaptureScreen.
- **Skip button**: Available on all non-final pages. Final page has "Go to Timeline" alternative.
- **Navigation gating**: `Screen.Onboarding` added to sealed class. `MemlyNavGraph` accepts dynamic `startDestination`. `MainActivity` waits for DataStore to emit before creating NavHost (avoids race condition with `initial = null` loading gate).

---

## Ad-hoc: Map Preview & Place Picker Polish
**Date:** 2026-03-19

### Changed
- **Map preview card**: Redesigned as a compact 120dp square callout (image on top, title below) that appears right above the tapped marker with fade+scale animation. Map centers on tapped marker.
- **Place picker layout**: Restructured to overlay search bar and results on top of a full-screen map. Added "My Location" FAB and auto-detect current location on open.

### Fixed
- **Location spinner**: `isDetectingLocation` no longer resets prematurely when falling back to `getCurrentLocation`.
- **MapView lifecycle**: Added `onPause()` before `onDetach()` on Map Screen to properly release tile download threads.
- **Clear location**: `clearLocation()` now also clears `placeLabel` to avoid stale place name text.
- **HTTP streams**: Nominatim HTTP connections now use `.use {}` to properly close input streams.

---

## Phase 2, Section 3: Enhanced Capture
**Date:** 2026-03-19

### Added
- **Media preview grid**: 3-column grid replaces horizontal scroll. Shows order badges, video play indicators, and X remove buttons on each item.
- **Tap-to-swap reorder**: Tap a media item to select it (highlighted with swap icon), tap another to swap their positions. `sortOrder` field persists media order in `MediaFileEntity`.
- **Save progress indicator**: `LinearProgressIndicator` with step descriptions ("Processing media X of Y…", "Saving memory…") during save flow.
- **Content validation**: Save button disabled when no content (title, notes, or media). Hint text shown below save button.
- **Map-based place picker**: Full-screen `PlacePickerDialog` using osmdroid + Nominatim API. Search for places with autocomplete, tap on map to select location, reverse geocoding for place names. "Pick on Map" chip added next to "Get Location" in CaptureScreen.

### Changed
- Database version bumped 3→4 (destructive migration). `MediaFileEntity` gained `sortOrder: Int` field.
- `MemoryDao.getMediaFilesForMemory()` now orders by `sortOrder ASC`.
- `MemoryDetailViewModel` sorts loaded media files by `sortOrder` and assigns correct sortOrder to new media in edit mode.

---

## Ad-hoc: Fix Video Thumbnails on Timeline Cards
**Date:** 2026-03-19

### Fixed
- **Video thumbnails on timeline cards**: Videos displayed broken images because Coil cannot decode video content URIs. `MemoryPagerCard` and `MemoryCard` now use `thumbnailPath` (pre-generated frame) for VIDEO media, loaded as `File` instead of `Uri`. Readability check updated to use `File.exists()` for thumbnail paths.

---

## Ad-hoc: Video Recording via System Camera
**Date:** 2026-03-19

### Added
- **Video recording from camera button**: Camera button now shows a Photo/Video dialog. "Photo" launches `TakePicture` (existing), "Video" launches `CaptureVideo` (new). Both in CaptureScreen and MemoryDetailScreen edit mode.
- `addCameraMedia()` in both ViewModels now accepts a `mediaType` parameter to correctly tag camera-recorded videos.
- CameraX in-app camera with photo/video toggle added to Phase 3, Section 7 as tasks 7.6–7.8.

---

## Ad-hoc: Timeline Pagination
**Date:** 2026-03-19

### Added
- **Lazy pagination on timeline**: Memories sorted by `memoryDate` DESC (latest first), displaying 10 at a time with automatic lazy loading of the next 10 when scrolling within 3 pages of the end.
- `hasMoreMemories` flag in `TimelineUiState` to signal when more pages are available.
- `loadMore()` in `TimelineViewModel` to increment visible count.
- Pagination resets on search query, mood filter, or date filter changes.

---

## Phase 2, Section 2: Video Playback
**Date:** 2026-03-19

### Added
- **Media3 ExoPlayer** (1.6.0): `media3-exoplayer` and `media3-ui` dependencies for video playback.
- **VideoPlayer composable**: Wraps `PlayerView` in `AndroidView` with built-in playback controls, lifecycle-aware pause/resume, and proper player release on dispose.
- **Video playback in detail screen**: `PhotoHeroSection` detects `VIDEO` media type and renders `VideoPlayer` instead of static `AsyncImage`.
- **Video play icon indicators**: `PlayCircle` badges on timeline cards (`MemoryCard`, `MemoryCarouselCard`, `MemorySearchResultCard`) and timeline slideshow to visually distinguish videos from photos.

### Changed
- `MemoryCard` audio indicator refactored from single icon to `Row` layout supporting both video and audio badges side by side.
- Timeline slideshow now tracks `visualMediaFiles` (not just URIs) to detect video type per slide.

---

## Ad-hoc: Edit Media, Collection UX, Location Fallback
**Date:** 2026-03-18

### Added
- **Edit media in detail screen**: Edit mode now supports adding new photos/videos (gallery or camera) and removing existing media. Import choice dialog (Save to Memly / Keep Original) available during edit.
- **Add memories from collection detail**: FAB on CollectionDetailScreen opens dialog to toggle memories in/out of the collection. No longer need to go to each memory's detail screen individually.
- **Map place picker task**: Added task 3.7 to Phase 2 Section 3 for future map-based place picker with search.

### Changed
- **Collection dialog UX**: AddToCollectionDialog now shows checkboxes, border highlights, "Tap to toggle" hint, and collection descriptions for better discoverability.

### Fixed
- **Location fetching on real devices**: Now tries `lastLocation` first (fast, works indoors) before falling back to `getCurrentLocation` with `BALANCED_POWER_ACCURACY`. Better error message when location unavailable.

---

## Ad-hoc: UI Fixes & Bottom Nav Rounding
**Date:** 2026-03-18

### Fixed
- **TopAppBar double padding**: CaptureScreen and CollectionDetailScreen had extra space at top due to both outer Scaffold and inner TopAppBar adding status bar insets. Added `windowInsets = WindowInsets(0)` to inner TopAppBars.
- **Collection create dialog auto-showing**: Navigating to CollectionList triggered the create dialog because `lastConsumedTrigger` initialized to 0 instead of the current trigger value, treating stale triggers as new.

### Changed
- Bottom nav bar now has rounded corners (64dp) on all outer edges via updated `BottomBarCutoutShape`.

---

## Ad-hoc: Fix Image Loading for Keep Original & Dedup
**Date:** 2026-03-18

### Fixed
- **Dedup blocking**: `findMediaByHash` returned existing entry and skipped media entirely — memories saved with zero images. Now reuses existing file's URI/metadata to create a new reference row, avoiding disk duplication while allowing the same photo in multiple memories.
- **URI resolution**: Photo Picker returns temporary `content://media/picker/...` URIs; old resolver used `_ID` column which was picker-internal, not the real MediaStore ID. New Strategy 1 resolves by matching `DISPLAY_NAME` + `SIZE` against external MediaStore. Strategy 2 (direct ID) kept as fallback.
- **Missing runtime permission**: `READ_MEDIA_IMAGES` was declared in manifest but never requested. Now requested when user taps "Keep Original"; falls back to "Save to Memly" if denied.
- **Broken reference detection**: Only checked `EXTERNAL` source files; used `query()` which can succeed on dead URIs. Now checks ALL sources and uses `openInputStream()` for reliable readability test.
- **Timeline card broken images**: `AsyncImage` failed silently for dead URIs showing blank cards. Now uses `LaunchedEffect` with `openInputStream` readability check + broken image fallback UI.

### Changed
- `MediaStoreManager.isUriAccessible()` uses `openInputStream()` instead of `query()`
- `MemoryDetailViewModel` broken check applies to all media sources, not just EXTERNAL
- `CaptureViewModel` added `hideImportChoiceDialog()` to preserve pending URIs during permission flow

---

## Ad-hoc: FAB Bottom Nav Redesign
**Date:** 2026-03-18

### Changed
- Bottom nav bar now has a permanent curved cutout in the center, created via custom `BottomBarCutoutShape` using cubic bezier curves
- Add button replaced with a proper `FloatingActionButton` that overlaps the bar, centered in the cutout
- FAB shows on Timeline (navigates to Capture) and CollectionList (opens create collection dialog) with scale animation
- FAB hides on Map and Settings screens; cutout remains for visual consistency
- Removed add button from `CollectionHeader` — create action now uses the bottom nav FAB
- Added `createCollectionTrigger` mechanism to wire FAB clicks through NavGraph to CollectionListScreen's ViewModel

---

## Phase 2, Section 1: Voice Memos
**Date:** 2026-03-18

### Added
- `AUDIO` value added to `MediaType` enum — enables audio file handling across the entire data layer
- `AudioRecorder` utility class — wraps `MediaRecorder` lifecycle (start/stop/cancel), records AAC audio at 128kbps/44.1kHz to temp M4A files
- `AudioPlaybackBar` composable — play/pause with `MediaPlayer`, linear progress bar, elapsed/total time display, auto-cleanup via `DisposableEffect`
- `RECORD_AUDIO` permission in AndroidManifest with runtime request on CaptureScreen
- Voice Memo section on CaptureScreen — record button, recording indicator with live timer, cancel/stop controls, playback preview with remove button
- Audio playback on MemoryDetailScreen — `AudioPlaybackBar` shown below photo hero for memories with voice memos
- Audio indicator (mic badge) on all card variants: timeline pager cards, no-image fallback cards, search result cards, carousel cards, map preview cards
- `durationMs` field on `MediaFileEntity` — cached at save time for instant playback info
- `queryDuration()` method on `MediaStoreManager` — extracts duration via `MediaMetadataRetriever`
- `durationMs` field on `MediaMetadata` data class
- `audio/` cache path added to FileProvider paths (`file_paths.xml`)

### Changed
- `MediaStoreManager.getRelativePath()` and `getCollectionUri()` extended for `AUDIO` → `Music/Memly/` and `MediaStore.Audio.Media`
- `MediaStoreManager.queryDimensions()` returns `null` for `AUDIO` (no visual dimensions)
- `ThumbnailUtil.generateThumbnail()` returns `null` for `AUDIO` (no visual thumbnail)
- `CaptureViewModel.processMediaItem()` handles `AUDIO` media type (default mimeType, duration extraction)
- All card components filter audio from visual media for image display, show mic indicator separately
- Timeline slideshow pager excludes audio files from image URIs

---

## Phase 2, Section 0: File Management Refactor
**Date:** 2026-03-18

### Added
- `MediaSource` enum (`APP_OWNED`, `EXTERNAL`, `IMPORTED`) — three-state ownership model
- `MediaStoreManager` utility class — centralized MediaStore API operations (insert, resolve, delete, query metadata)
- Import choice dialog on CaptureScreen — "Save to Memly" vs "Keep original" for picked media
- "Import to Memly" action on MemoryDetailScreen — converts EXTERNAL → IMPORTED
- Broken reference detection and placeholder on MemoryDetailScreen — "Original file removed" with remove action
- `WRITE_EXTERNAL_STORAGE` (API 28) and `READ_MEDIA_AUDIO` (API 33+) permissions in manifest

### Changed
- `MediaFileEntity` schema: replaced `filePath` → `mediaStoreUri`, `isReference` → `source: MediaSource`, added `relativePath`, `displayName`, `mimeType`, `size`, `dateTaken`, `width`, `height`
- `MemlyDatabase` version bumped to 2 with destructive migration (dev stage)
- `MemoryDao`: added `findMediaByUri()`, `getMediaFilesBySource()`, `updateMediaFile()` queries
- `MemoryRepository`: added `deleteMemoryWithFiles()` for source-aware file cleanup, `updateMediaFile()`, `findMediaByUri()`
- `CaptureViewModel`: save flow now uses `MediaStoreManager` — camera photos → `Pictures/Memly/` (APP_OWNED), picked media → user choice (IMPORTED/EXTERNAL)
- `MemoryDetailViewModel`: `deleteMemory()` now deletes owned/imported files from public storage, added `importToMemly()` and `removeBrokenReference()`
- `SettingsViewModel`: clear-all-data now deletes owned/imported files from MediaStore before clearing Room
- Detail screen loads full-size images via `Uri.parse(mediaStoreUri)` instead of `File(filePath)`
- Thumbnails remain file-based in `cacheDir/thumbnails/` (unchanged)

---

## Navigation & Screen Redesign
**Date:** 2026-03-17

### Changed
- Bottom nav items: replaced Favorites (heart) with Map icon, replaced GridView (boxes) with Settings (gear) icon
- Bottom nav tabs are now: Home, Collections, +Add, Map, Settings
- CollectionListScreen redesigned as top-level nav destination: removed TopAppBar/back arrow, added custom header with icon + title + create button, redesigned collection cards with 20dp corners and surfaceContainerHigh color, added search bar for collections
- SettingsScreen redesigned as top-level nav destination: removed TopAppBar/back arrow, added custom header with gear icon, cards updated to match new design language
- Timeline search bar is now functional: BasicTextField with live filtering via debounced search
- Timeline filters replaced: removed static FilterChips row, added filter icon button next to search bar with dropdown menu
- Mood filter is now multi-select (Set<Mood>) with toggle checkmarks and "Done" button
- Date filter changed from sort order to specific day picker via Material 3 DatePickerDialog
- Active filter chips shown below search bar (horizontally scrollable) with dismiss buttons
- Search results display in the same coverflow pager as regular memories (no separate results view)
- MemlyNavGraph: removed onNavigateBack from CollectionList and Settings composables

### Added
- `CollectionDao.searchCollections()` — LIKE query matching name and description
- `CollectionRepository.searchCollections()` — exposed to ViewModels
- `CollectionListViewModel` search query with debounce(300) switching between all/search
- `CollectionSearchBar` composable on Collections screen
- `TimelineViewModel` multi-mood filter (Set<Mood>), date filter (Long?), toggleMoodFilter(), clearMoodFilters()
- `SearchBarWithFilter` composable with filter icon, dropdown menu, mood submenu, date picker
- `ActiveFilterChips` composable with mood-colored chips and date chip
- Empty states for "no search results" on both Timeline and Collections screens

### Removed
- `ui/search/SearchScreen.kt` — entire Search screen deleted
- `ui/search/SearchViewModel.kt` — entire Search ViewModel deleted
- `Screen.Search` from navigation sealed class
- Search route from MemlyNavGraph
- `DateSort` enum (replaced by specific date filter)
- `FilterChips` composable from Timeline screen
- `onNavigateBack` parameter from CollectionListScreen and SettingsScreen

---

## Timeline Card Redesign v2
**Date:** 2026-03-17

### Changed
- Redesigned memory pager cards to match new visual reference (coverflow-style)
- Card height increased from 400dp to 480dp for more immersive feel
- Title upgraded from `headlineSmall` to `headlineLarge` bold display text
- Location display changed from pin icon + text to `@location` format
- "See more" button restyled as frosted glass pill with `+` icon, moved to bottom-right
- Bottom row layout: `@location` left-aligned, "See more +" button right-aligned
- Pager changed from stacked overlap to coverflow: centered card with side cards rotated 15 degrees on Y-axis
- Side cards scale to 85%, fade to 50% opacity, with 3D perspective tilt
- Removed next arrow button from cards for cleaner design
- Removed date from card overlay
- Gradient overlay taller (260dp) with 3-stop softer fade
- Corner radius increased from 24dp to 28dp

### Removed
- ChevronRight icon and next button logic
- Date display from memory pager cards
- IconButtonDefaults and ButtonDefaults imports (no longer needed)

---

## Bottom Navigation Bar Redesign
**Date:** 2026-03-17

### Changed
- Replaced standard Material 3 NavigationBar with custom floating pill-shaped bottom nav bar
- Dark rounded pill background using MaterialTheme.colorScheme tokens (adapts to light/dark theme)
- Icon-only nav items with white circular indicator on selected item
- Updated nav items from 3 (Timeline, Map, Search) to 4 (Home, Collections, Favorites, Settings)
- Removed standalone FAB; integrated center add button directly into the bottom bar
- Bottom bar overlays content via Box instead of Scaffold bottomBar slot (floating effect)

### Added
- `MemlyBottomNavBar` composable in `ui/components/` with theme-aware colors
- Center "Add Memory" button with tertiary color accent
- Bottom padding (80dp) on SearchScreen, CollectionListScreen, SettingsScreen to prevent content clipping

---

## Timeline Homescreen Redesign
**Date:** 2026-03-17

### Changed
- Redesigned Timeline/homescreen from vertical LazyColumn to horizontal stacked card pager
- New layout: ProfileHeader → SearchBar → FilterChips → HorizontalPager
- Stacked card effect with negative page spacing, zIndex layering, scale/alpha depth
- Each memory card shows auto-sliding image slideshow (nested HorizontalPager, 4s interval, 600ms slide)
- Slideshow only plays on the active/current card; stops and resets when swiped away
- Added circular "next" arrow button on current card
- Added slideshow indicator dots (top-left) for multi-image memories
- Static profile header: "Hello, Catt" with avatar and notification bell
- Search bar placeholder (non-functional, design only)
- Filter chips: All, Date, Mood (design only, no functionality yet)

### Added
- `allMemories` field in `TimelineUiState` for flat memory list (pager data source)
- `MemoryPagerCard` composable with stacked card layout, slideshow, mood chip, "See more" button

---

## Phase 2, Section 5: Theme & UI Polish
**Date:** 2026-03-17

### Verified
- Full Memly palette already in Color.kt (light + dark) from Phase 1
- Theme.kt already wired with lightColorScheme() + darkColorScheme() + MemlyShapes
- Typography with Poppins (headings) + Inter (body) already bundled
- Mood-specific colors via Mood.color() extension already used across all screens
- Card designs audited: 16dp for main cards, 12dp for containers, 8dp for chips — consistent
- All text/background colors use MaterialTheme.colorScheme — dark mode compatible
- No source changes needed; section was already complete from Phase 1 implementation

---

## Phase 1, Section 8: Integration & Polish
**Date:** 2026-03-17

### Fixed
- Added try/catch error handling to all ViewModels with suspend operations (Detail, CollectionList, CollectionDetail, Settings)
- Added error fields and Snackbar display to CollectionListScreen and SettingsScreen
- Fixed lint error: added `<uses-feature>` for camera hardware (ChromeOS compatibility)
- Fixed DefaultLocale warning in CaptureScreen (use Locale.ROOT for coordinate formatting)
- Removed unused template colors from colors.xml (purple, teal, black, white)
- Removed redundant activity label from AndroidManifest.xml

### Changed
- Removed unused `isLoading` field from SearchUiState (was always false)
- All ViewModels now have consistent error handling pattern

### Verified
- Lint passes with 0 errors (remaining warnings are dependency version suggestions)
- Back navigation works on all screens (no orphan routes)
- Empty states present on: Timeline, Map, Search, CollectionList, CollectionDetail
- Loading indicators present on: Detail, Map, CollectionList, CollectionDetail, Settings

---

## Phase 1, Section 7: Settings & Profile
**Date:** 2026-03-17

### Added
- SettingsViewModel with storage stats (memory count, media count, disk usage via File API)
- SettingsScreen with preference-style layout (About, Storage, Data Management sections)
- About section displaying app name, version from BuildConfig.VERSION_NAME/VERSION_CODE
- Storage info with formatted memory count, media file count, and human-readable disk usage
- Clear-all-data with double confirmation dialog (two-step: warning → final confirm with counts)
- Clears Room tables + deletes media files + deletes thumbnails
- Settings navigation route and shortcut card on Search screen
- BuildConfig generation enabled in build.gradle.kts
- DAO queries: getMemoryCount(), getMediaFileCount()

---

## Phase 1, Section 6: Search & Organization
**Date:** 2026-03-17

### Added
- SearchViewModel with debounced query (300ms), mood filter, and StateFlow<SearchUiState>
- SearchScreen with OutlinedTextField search bar, mood FilterChips, result count, and MemorySearchResultCard list
- Text search across title, notes, and placeLabel fields via LIKE queries with MemoryWithDetails
- Collections shortcut card on search screen linking to CollectionListScreen
- CollectionListViewModel with reactive collection + memory count loading via combine
- CollectionListScreen with collection cards (name, description, count), FAB to create, delete with confirmation
- CreateCollectionDialog with name and optional description fields
- CollectionDetailViewModel loading collection metadata + memories via SavedStateHandle
- CollectionDetailScreen with memory list using MemorySearchResultCard, empty state
- Add-to-collection flow on MemoryDetailScreen: "Add to collection" button in read mode, toggle dialog with checkmarks
- Navigation routes: CollectionList, CollectionDetail with collectionId argument

### Changed
- MemoryDetailViewModel: added CollectionRepository injection, collection dialog state, toggle logic
- MemoryDetailScreen: added CollectionsBookmark button in read mode, AddToCollectionDialog
- CollectionDao: added getMemoryCountInCollection, getCollectionIdsForMemory, getMemoriesInCollectionWithDetails
- MemoryDao: added searchMemoriesWithDetails with @Transaction
- MemlyNavGraph: added CollectionList and CollectionDetail routes

---

## Phase 1, Section 5: Map View
**Date:** 2026-03-16

### Added
- MapViewModel with MapUiState (geotagged memories, selected memory, mood filter)
- osmdroid (OpenStreetMap) integration via AndroidView wrapper — no API key required
- Mood-colored map pins with custom BitmapDrawable markers
- Animated bottom preview card on pin tap with thumbnail, title, date, location, mood chip
- Mood filter chips overlay for narrowing visible pins
- Empty state for no geotagged memories
- Empty state for filtered results with no matches
- Navigation from preview card to memory detail screen
- DAO query: getGeotaggedMemoriesWithDetails() for memories with media + tags
- INTERNET and ACCESS_NETWORK_STATE permissions for tile loading

### Decision
- D032: osmdroid over Google Maps — no API key, offline-capable, aligns with offline-first philosophy

---

## Phase 1, Section 4: Memory Detail Screen
**Date:** 2026-03-16

### Added
- MemoryDetailViewModel with DetailUiState, load by memoryId via SavedStateHandle
- Full-bleed photo hero with HorizontalPager and page indicator dots
- Read mode: title, date, mood chip, location, notes, tag chips
- Edit mode: editable fields (title, notes, mood selector, place label, tags) with FAB toggle
- Save edits with tag diff (add new tags, remove old tags)
- Delete memory with AlertDialog confirmation and cascade deletion
- Back/cancel button as semi-transparent circle overlaid on photo hero

---

## Phase 1, Section 3: Timeline Screen
**Date:** 2026-03-16

### Added
- TimelineViewModel with StateFlow<TimelineUiState> containing date-grouped memories and Time Hop
- MemoryCard composable: image-dominant with gradient overlay, mood chip, title/date/place, tag chips
- MemoryCard no-image fallback: surface background with mood accent strip
- MemorySearchResultCard: horizontal layout with 80dp thumbnail + text
- MemoryCarouselCard: 180dp wide, 4:5 ratio for Time Hop carousel with "X years ago" label
- Sticky section headers (Today, Yesterday, day name, month year) with opaque background
- Pull-to-refresh via PullToRefreshBox
- Empty timeline state with message and "Create your first memory" CTA button
- Time Hop DAO query using strftime for same-day-in-previous-years matching
- onCaptureClick callback wired from empty state to navigation

### Changed
- TimelineViewModel rewritten from flat list to grouped TimelineUiState
- TimelineScreen rewritten with LazyColumn stickyHeaders, Time Hop section, empty state

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
