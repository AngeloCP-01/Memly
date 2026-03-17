# Changelog

All notable changes to Memly are documented here, organized by phase and section.

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
