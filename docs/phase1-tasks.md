# Memly -- Phase 1 MVP Task Breakdown

## Task Legend

| Symbol | Meaning      |
|--------|--------------|
| ⬜     | Not Started  |
| 🔄     | In Progress  |
| ✅     | Completed    |
| ❌     | Blocked      |

---

## Dependency Map

```
Section 1: Project Setup & Configuration
    |
    v
Section 2: Memory Capture (Core)
    |
    +---> Section 3: Timeline Screen
    |         |
    |         v
    |     Section 4: Memory Detail Screen
    |
    +---> Section 5: Map View
    |
    +---> Section 6: Search & Organization
    |
    v
Section 7: Settings & Profile
    |
    v
Section 8: Integration & Polish
```

Sections 3, 5, and 6 can proceed in parallel once Section 2 is complete.
Section 4 depends on Section 3 (timeline card tap navigates to detail).
Section 7 is independent but logically follows core feature work.
Section 8 is the final pass and depends on all prior sections.

---

## Summary Table

| Section | Name                          | Tasks | Complexity | Risk    |
|---------|-------------------------------|-------|------------|---------|
| 1       | Project Setup & Configuration | 8     | Low        | Low     |
| 2       | Memory Capture (Core)         | 13    | High       | High    |
| 3       | Timeline Screen               | 11    | Medium     | Medium  |
| 4       | Memory Detail Screen          | 7     | Medium     | Medium  |
| 5       | Map View                      | 9     | Medium     | Medium  |
| 6       | Search & Organization         | 11    | Medium     | Medium  |
| 7       | Settings & Profile            | 6     | Low        | Low     |
| 8       | Integration & Polish          | 10    | Medium     | Medium  |
|         | **Total**                     | **75**|            |         |

---

## Section 1: Project Setup & Configuration

**Status:** COMPLETE

All foundational work is done. The project builds, Room database is configured, Hilt DI is wired, navigation scaffold with bottom nav and FAB is in place, and placeholder screens exist.

| Task | Description                                                        | Status | Notes                                      |
|------|--------------------------------------------------------------------|--------|--------------------------------------------|
| 1.1  | Create Android project with Kotlin and Jetpack Compose             | ✅     | Project created in Android Studio          |
| 1.2  | Configure Gradle: Room, Hilt, KSP, Navigation Compose, Coil       | ✅     | All dependencies added and synced          |
| 1.3  | Set up Hilt application class and annotation processing            | ✅     | @HiltAndroidApp on Application class       |
| 1.4  | Configure theme (Material 3 color scheme, typography)              | ✅     | Light theme in place                       |
| 1.5  | Build navigation scaffold with bottom nav bar and capture FAB      | ✅     | Bottom nav: Timeline, Map, Search, Profile |
| 1.6  | Define Room entities (Memory, MediaFile, Tag, Collection, etc.) and DAOs | ✅ | All entities and DAOs created              |
| 1.7  | Create repositories and Hilt DI modules                            | ✅     | Repository layer wired to DAOs via Hilt    |
| 1.8  | Verify clean build and run on emulator                             | ✅     | App launches with placeholder screens      |

**Checkpoint:** App builds and runs. Navigation scaffold visible. Database schema in place.

---

## Section 2: Memory Capture (Core)

**Status:** COMPLETE

Full capture flow implemented: media selection (gallery + camera), text input with character counters, mood selector, GPS location with permission handling, place label, tag management, date picker, and transactional save with SHA-256 dedup and thumbnail generation.

**Risks:**
- Runtime permission handling for camera, location, and media access across API levels.
- CameraX integration adds build complexity (lifecycle binding, preview surface).
- File hash computation on large videos may need to run on a background thread with progress indication.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 2.1  | Create CaptureViewModel with UiState (title, notes, mood, media list, location, tags, date, loading, error) | ✅ | Single StateFlow<CaptureUiState> exposed to UI               |
| 2.2  | Build CaptureScreen scaffold (scrollable form layout with sections)         | ✅     | Compose Column with sections for media, text, mood, location, tags |
| 2.3  | Implement photo/video selection from gallery using Photo Picker API         | ✅     | Use ActivityResultContracts.PickMultipleVisualMedia; copy to app-private storage |
| 2.4  | Implement camera capture via intent (ACTION_IMAGE_CAPTURE / ACTION_VIDEO_CAPTURE) | ✅ | Use FileProvider for URI; TakePicture contract with permission handling |
| 2.5  | Build text notes input (title field + multi-line notes field)               | ✅     | OutlinedTextField composables with character counters          |
| 2.6  | Build mood/emotion selector (chip grid from Mood enum values)               | ✅     | FlowRow of FilterChip composables; single selection with mood colors |
| 2.7  | Implement GPS location capture with permission handling                     | ✅     | FusedLocationProviderClient; handles fine + coarse location permissions |
| 2.8  | Build custom place label input field                                        | ✅     | Simple text field with location icon                          |
| 2.9  | Build tag input (text field + chip display, add/remove)                     | ✅     | TextField with IME action to add; FlowRow of chips with dismiss icon |
| 2.10 | Add date picker for memory date (default to now)                            | ✅     | Material 3 DatePickerDialog; store as epoch millis            |
| 2.11 | Implement save flow: create MemoryEntity, MediaFileEntity(s), TagEntity(s) in a transaction | ✅ | Repository method wrapping Room withTransaction; navigate back on success |
| 2.12 | Implement file hash computation (SHA-256) and dedup check before save       | ✅     | Compute on Dispatchers.IO; skip insert if hash exists         |
| 2.13 | Implement thumbnail generation on save (Bitmap scaling for images, frame extraction for video) | ✅ | Save thumbnail to app cache dir; store path in MediaFileEntity; video frame extraction via MediaMetadataRetriever |

**Checkpoint:** Can create a memory with photo, text notes, mood, GPS location, place label, tags, and custom date. Memory persists in Room. Duplicate media is detected.

---

## Section 3: Timeline Screen

**Status:** COMPLETE

Fully functional timeline with date-grouped memories, three MemoryCard variants (image-dominant, no-image fallback, search result), Time Hop carousel, sticky headers, pull-to-refresh, and empty state with CTA.

**Risks:**
- Performance with large lists requires proper use of LazyColumn keys and stable items.
- Clustering logic (day/week/month grouping) needs clean implementation to avoid re-computation on every recomposition.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 3.1  | Enhance TimelineViewModel to expose StateFlow<TimelineUiState> with grouped memories | ✅ | TimelineUiState with groups + timeHopMemories via combine     |
| 3.2  | Build MemoryCard composable — image-dominant with gradient overlay (see ui-design-guide.md) | ✅ | 3:4 portrait aspect ratio; dark gradient overlay bottom third; title + date + mood chip over gradient; tag chips below image |
| 3.2a | Build MemoryCard no-image fallback variant (surface background + mood accent strip) | ✅ | Surface variant bg with mood color accent strip at top |
| 3.2b | Build MemoryCard search result variant (horizontal: square thumbnail + text) | ✅ | 80dp thumbnail, MemorySearchResultCard composable |
| 3.3  | Load and display thumbnails in MemoryCard using Coil AsyncImage             | ✅     | AsyncImage with File model from thumbnailPath                  |
| 3.4  | Implement day/week/month clustering with sticky section headers             | ✅     | Today/Yesterday/day name/month grouping; stickyHeader with opaque bg |
| 3.5  | Add pull-to-refresh using Material 3 PullToRefresh                          | ✅     | PullToRefreshBox wrapping timeline; Room Flow auto-updates     |
| 3.6  | Build empty state composable (illustration + message + CTA to capture)      | ✅     | Message + "Create your first memory" button                    |
| 3.7  | Navigate to memory detail on card tap (pass memoryId)                       | ✅     | onMemoryClick callback wired to NavController                  |
| 3.8  | Implement "Time Hop" horizontal carousel for memories from this day in previous years | ✅ | MemoryCarouselCard (180dp, 4:5); "X years ago" label; DAO query with strftime |
| 3.9  | Verify: timeline displays memories with clustering, thumbnails, and navigation | ✅  | Build passes; all components wired                             |

**Checkpoint:** Timeline shows all memories grouped by date. Thumbnails load. Tapping a card navigates to detail. Time Hop surfaces old memories.

---

## Section 4: Memory Detail Screen

**Status:** NOT STARTED

Full view and edit screen for a single memory, including media gallery and metadata editing.

**Risks:**
- Video playback requires ExoPlayer or Media3 setup.
- Edit mode state management must handle partial saves and cancellation cleanly.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 4.1  | Create MemoryDetailViewModel -- load memory with media, tags by memoryId    | ⬜     | Combine Memory + MediaFiles + Tags into DetailUiState          |
| 4.2  | Display full-bleed photo hero with HorizontalPager for multiple media       | ⬜     | Full-width, no side padding, 3:4 ratio or 50% screen height; page indicator dots; back button + mood chip overlaid on image |
| 4.3  | Display metadata below image: title, notes, mood, location, date, tags     | ⬜     | Scrollable column, 16dp horizontal padding; labeled sections   |
| 4.4  | Implement edit mode toggle (FAB or toolbar button)                          | ⬜     | Switch between read-only and editable fields                   |
| 4.5  | Save edits: update MemoryEntity and related entities                        | ⬜     | Repository update method; show success snackbar                |
| 4.6  | Delete memory with confirmation dialog                                      | ⬜     | AlertDialog; delete memory + cascade media/tags; navigate back |
| 4.7  | Verify: navigate from timeline, view all fields, edit, delete               | ⬜     | End-to-end manual test                                         |

**Checkpoint:** Can view any memory in full detail, edit its metadata, and delete it. Navigation back to timeline works.

---

## Section 5: Map View

**Status:** NOT STARTED

Visualize geotagged memories as pins on an interactive map.

**Risks:**
- Google Maps SDK requires an API key in local.properties / AndroidManifest; billing must be enabled.
- Alternative: osmdroid (OpenStreetMap) avoids API key but has a different API surface.
- Rendering many pins (hundreds) may require clustering via Maps Utils library.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 5.1  | Add Google Maps Compose dependency (or osmdroid alternative)                | ⬜     | maps-compose library; configure API key in manifest            |
| 5.2  | Create MapViewModel -- load all memories with non-null lat/lng              | ⬜     | StateFlow<MapUiState> with list of map markers                 |
| 5.3  | Display map with memory pins at lat/lng coordinates                         | ⬜     | GoogleMap composable with Marker for each memory               |
| 5.4  | Customize pin markers (mood-colored pins or mini thumbnails)                | ⬜     | BitmapDescriptor from mood color; consider performance         |
| 5.5  | Show memory preview card on pin tap (bottom sheet or overlay)               | ⬜     | ModalBottomSheet with MemoryCard composable                    |
| 5.6  | Navigate from preview card to memory detail screen                          | ⬜     | Pass memoryId to detail route                                  |
| 5.7  | Add basic filter controls (date range or mood) for map pins                 | ⬜     | Filter chips above map; re-query ViewModel                     |
| 5.8  | Build empty state for no geotagged memories                                 | ⬜     | Overlay message on map or placeholder screen                   |
| 5.9  | Verify: map displays pins, tap shows preview, navigate to detail            | ⬜     | Test with memories that have and lack coordinates              |

**Checkpoint:** Map renders with pins for geotagged memories. Tapping a pin shows a preview. Preview navigates to detail. Filters narrow visible pins.

---

## Section 6: Search & Organization

**Status:** NOT STARTED

Text search across memories and a collections (albums) system for manual grouping.

**Risks:**
- Full-text search in Room requires FTS4 virtual table setup or LIKE queries (less performant).
- UX for assigning memories to collections needs careful design (multi-select or per-memory action).

| Task | Description                                                                  | Status | Notes                                                        |
|------|------------------------------------------------------------------------------|--------|--------------------------------------------------------------|
| 6.1  | Create SearchViewModel with query state, filters, and search results         | ⬜     | Debounced query input; StateFlow<SearchUiState>              |
| 6.2  | Build search bar with text input and clear button                            | ⬜     | Material 3 SearchBar composable                              |
| 6.3  | Implement text search across notes, title, and placeLabel fields             | ⬜     | Room DAO query with LIKE %query%; consider FTS later         |
| 6.4  | Add filter chips: mood selector, date range picker                           | ⬜     | Combine with text query in repository                        |
| 6.5  | Display search results using MemoryCard search-result variant (horizontal layout) | ⬜ | Square thumbnail + text row; LazyColumn; show result count   |
| 6.6  | Build CollectionListScreen -- display all collections                        | ⬜     | LazyColumn of collection cards with name, count, cover image |
| 6.7  | Create collection dialog (name, optional description)                        | ⬜     | AlertDialog with TextField inputs                            |
| 6.8  | Implement add-memory-to-collection flow                                      | ⬜     | From detail screen action menu or long-press on timeline     |
| 6.9  | Build CollectionDetailScreen -- memories belonging to a collection           | ⬜     | Reuse timeline-style list filtered by collection             |
| 6.10 | Delete collection with confirmation (memories remain, only grouping removed) | ⬜     | AlertDialog; delete CollectionEntity and join table entries   |
| 6.11 | Verify: search returns correct results, collections CRUD works end-to-end    | ⬜     | Test with varied queries and multiple collections            |

**Checkpoint:** Search finds memories by text and filters. Collections can be created, populated, viewed, and deleted.

---

## Section 7: Settings & Profile

**Status:** NOT STARTED

Basic settings screen with app info and data management.

**Risks:**
- Low risk overall. Clear-all-data operation must be confirmed and thorough.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 7.1  | Create SettingsViewModel with storage stats                                 | ⬜     | Query memory count, media count, estimate storage via File API |
| 7.2  | Build SettingsScreen layout (preference-style list)                          | ⬜     | Column with Section headers and ListItem rows                  |
| 7.3  | Add About section (app name, version from BuildConfig)                      | ⬜     | Read BuildConfig.VERSION_NAME and VERSION_CODE                 |
| 7.4  | Display storage info (memory count, media count, estimated disk usage)       | ⬜     | Formatted numbers and file sizes                               |
| 7.5  | Implement clear-all-data with double confirmation dialog                     | ⬜     | Delete all Room data and media files from app storage           |
| 7.6  | Verify: settings displays correct info, clear data works                     | ⬜     | Confirm counts match, data is actually removed                 |

**Checkpoint:** Settings screen shows app info and storage stats. Clear-all-data wipes everything after confirmation.

---

## Section 8: Integration & Polish

**Status:** NOT STARTED

Final integration testing, error handling, and UI polish across all screens.

**Risks:**
- Edge cases in navigation (deep links, process death, back stack).
- Permission re-prompting on denied state requires careful UX.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 8.1  | End-to-end flow test: capture, timeline, detail, edit, map, search          | ⬜     | Walk through every major user path manually                    |
| 8.2  | Add empty state composables to all screens that can be empty                | ⬜     | Consistent style across timeline, map, search, collections     |
| 8.3  | Add loading state indicators (CircularProgressIndicator) to all async operations | ⬜ | Show while Room queries or file operations are in flight       |
| 8.4  | Add error handling in all ViewModels (try/catch with user-facing messages)  | ⬜     | Expose error state in UiState; show via Snackbar               |
| 8.5  | Polish permission handling (rationale dialogs before request, settings link on permanent deny) | ⬜ | Camera, location, media permissions                    |
| 8.6  | Verify back navigation on all screens (no orphan routes)                    | ⬜     | Ensure popBackStack works correctly everywhere                 |
| 8.7  | Handle keyboard interactions (dismiss on scroll, correct IME actions)       | ⬜     | BringIntoViewRequester for fields behind keyboard              |
| 8.8  | Set app icon placeholder (simple launcher icon via Image Asset Studio)      | ⬜     | Adaptive icon with brand color background                      |
| 8.9  | Run lint and fix warnings                                                   | ⬜     | Address unused imports, missing content descriptions            |
| 8.10 | Final build: generate debug APK and smoke test on device                    | ⬜     | Install on physical device; verify all features                |

**Checkpoint:** App is stable, handles errors gracefully, and all screens have proper loading/empty/error states. Debug APK runs on a real device.

---

## Architecture Notes

- **Pattern:** MVVM with Repository. No domain/usecase layer.
- **Stack:** Kotlin, Jetpack Compose, Room, Hilt, Navigation Compose, Coil.
- **Data flow:** Screen (Composable) -> ViewModel (StateFlow) -> Repository -> Room DAO -> SQLite.
- **File strategy:** Reference-first (store file path, not blob). SHA-256 hash for dedup. Thumbnails generated at capture time.
- **Privacy:** Fully offline. No network calls. No analytics. No accounts.
