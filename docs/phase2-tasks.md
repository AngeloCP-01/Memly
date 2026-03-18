# Memly -- Phase 2 Enhanced Experience Task Breakdown

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
Section 0 (File Management) ──> ALL SECTIONS (must complete first)
Section 1 (Voice Memos)  ──┐
Section 2 (Video Playback)  ├──> Section 7 (Integration)
Section 3 (Enhanced Capture)│
Section 4 (Onboarding)     ─┤
Section 5 (Theme Polish)   ─┤
Section 6 (Data Management) ┘
```

**Section 0 is a prerequisite for all other sections.** It refactors file storage from app-private to public MediaStore-based storage. Sections 1 (Voice Memos), 2 (Video), and 3 (Enhanced Capture) all depend on the new file management layer.
Sections 1 through 6 are mostly independent and can proceed in parallel after Section 0.
Section 7 depends on all prior sections and is the final integration pass.

---

## Summary Table

| Section | Name                          | Tasks | Complexity | Risk   |
|---------|-------------------------------|-------|------------|--------|
| **0**   | **File Management Refactor**  | **14**| **High**   | **High** |
| 1       | Voice Memos                   | 8     | Medium     | Medium |
| 2       | Video Playback                | 7     | Medium     | Medium |
| 3       | Enhanced Capture              | 6     | Low        | Low    |
| 4       | Onboarding Flow               | 6     | Low        | Low    |
| 5       | Theme & UI Polish             | 8     | Low        | Low    |
| 6       | Data Management               | 10    | High       | Medium |
| 7       | Integration & Polish          | 5     | Medium     | Medium |
|         | **Total**                     | **64**|            |        |

---

## Section 0: File Management Refactor (Priority)

**Status:** COMPLETE

Refactor file storage from app-private (`filesDir/media/`) to public MediaStore-based storage. Content created in-app (camera, audio) is saved directly to public directories (`Pictures/Memly/`, `Movies/Memly/`, `Music/Memly/`) — visible in gallery, survives uninstall. Content picked from gallery is referenced by URI or optionally imported. App stores MediaStore URIs in Room instead of absolute file paths.

**Design Decisions:**
- **In-app content** (camera photo/video, audio recording) → write directly to public storage via MediaStore API (`MediaSource.APP_OWNED`). Only copy that exists. Survives uninstall. Visible in gallery/file manager.
- **Picked content** (selected from gallery) → user chooses: reference only (`MediaSource.EXTERNAL`) or save to Memly (`MediaSource.IMPORTED`). Reference = zero duplication but can break. Import = copy to `Pictures/Memly/`, app owns it.
- **URI persistence** → For external references, first attempt to resolve PhotoPicker URI to a stable **MediaStore content URI** (query by `_ID`). If resolution fails (cloud-backed providers like Google Photos), fall back to `takePersistableUriPermission()` on the original URI. PhotoPicker URIs are temporary and MUST NOT be stored directly — always resolve or persist.
- **Media metadata caching** → Store `mimeType`, `size`, `dateTaken`, `width`, `height` in entity at capture time. Avoids repeated MediaStore queries for display.
- **Deletion logic** → `APP_OWNED`/`IMPORTED`: delete via ContentResolver directly. If deletion fails (scoped storage edge case on Android 11+), fall back to `MediaStore.createDeleteRequest()` for user confirmation. `EXTERNAL`: only remove the Room reference, never touch the original file.
- **Deduplication** → On import/save, compute SHA-256 hash. If hash already exists in DB: for `IMPORTED`, reuse existing file URI (skip copy). For `EXTERNAL`, just reference (no file to dedup). Prevents duplicate storage.
- **File naming** → Structured convention: `memly_<yyyyMMdd_HHmmss>_<shortId>.<ext>` (e.g. `memly_20260318_143022_a7f3.jpg`). Clean, sortable, traceable, no collisions.
- **Text/mood/tags** → Room DB only (unchanged).
- **Thumbnails** → app-private cache (small, regenerable, unchanged).
- **Permissions:** No permission needed for MediaStore inserts on Android 10+. `WRITE_EXTERNAL_STORAGE` for Android 9 (our minSdk 28). `READ_MEDIA_IMAGES/VIDEO/AUDIO` for Android 13+ to read referenced files.

**Risks:**
- High. Touches the entire file management layer: entity schema, DAO, repository, capture flow, detail display, and timeline cards.
- URI fragility: PhotoPicker URIs are temporary (must resolve to MediaStore URI or persist via SAF). Cloud-backed providers (Google Photos) may not resolve to MediaStore — SAF fallback required. External references can break if user deletes original from gallery — must handle gracefully.
- Room migration required for existing data (dev-only at this stage, so destructive migration is acceptable).
- Must handle API level differences (Android 9 vs 10+ vs 13+).

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 0.1  | Create `MediaSource` enum (`APP_OWNED`, `EXTERNAL`, `IMPORTED`)             | ✅     | Replaces boolean flag. `APP_OWNED` = created in-app (camera/recording), lives in `Pictures/Memly/`. `EXTERNAL` = reference to user's gallery photo, URI only. `IMPORTED` = user chose "Save to Memly", copied to `Pictures/Memly/`, app owns it. |
| 0.2  | Create `MediaStoreManager` utility class for all MediaStore operations      | ✅     | Insert, query, resolve, delete via ContentResolver. Handles API level branching (Android 9 legacy vs 10+ scoped storage). Includes: `insertMedia()`, `resolveUri()` (try MediaStore resolution first → fall back to `takePersistableUriPermission()` for cloud-backed URIs), `deleteOwnedMedia()` (with `createDeleteRequest()` fallback on Android 11+), `queryMetadata()`. File naming: `memly_<yyyyMMdd_HHmmss>_<shortId>.<ext>`. Single source of truth for all public storage I/O. |
| 0.3  | Update `MediaFileEntity` schema with new fields                             | ✅     | Replace `filePath` → `mediaStoreUri: String`. Replace `isReference: Boolean` → `source: MediaSource`. Add: `relativePath`, `displayName`, `mimeType`, `size: Long`, `dateTaken: Long?`, `width: Int?`, `height: Int?`. Cache metadata at capture time to avoid repeated MediaStore queries. Destructive migration OK (dev stage). |
| 0.4  | Update `MemoryDao` queries for new `MediaFileEntity` fields                 | ✅     | Update any queries referencing old `filePath` column. Add query to find media by `mediaStoreUri`. Add query to find media by `source` type. |
| 0.5  | Update `MemoryRepository` to use `MediaStoreManager` for all file operations | ✅    | Replace direct file I/O with MediaStore calls. Save in-app content to public dirs. Handle deletion by source: `APP_OWNED`/`IMPORTED` → delete file via ContentResolver (fall back to `createDeleteRequest()` if scoped storage blocks it) + delete DB row, `EXTERNAL` → delete DB row only, never touch original file. |
| 0.6  | Add permission handling: `WRITE_EXTERNAL_STORAGE` (API 28), `READ_MEDIA_*` (API 33+) | ✅ | Runtime permission requests. No permission needed for MediaStore inserts on API 29+. `READ_MEDIA_*` needed to access external references on Android 13+. |
| 0.7  | Update `CaptureViewModel` save flow: in-app camera → MediaStore insert to `Pictures/Memly/` | ✅ | Photos/videos taken via camera are saved directly to public storage. Store returned MediaStore URI in entity. Set `source = APP_OWNED`. Cache metadata (size, mimeType, dimensions) at save time. |
| 0.8  | Update `CaptureViewModel` save flow: picked media → user choice dialog      | ✅     | Show dialog: "Keep in original location" (reference, `EXTERNAL`) vs "Save to Memly" (copy to `Pictures/Memly/`, `IMPORTED`). When user picks "Keep original", show subtle warning: *"If the original is deleted from your gallery, this photo may no longer appear in Memly."* For `EXTERNAL`: resolve via `MediaStoreManager.resolveUri()` — tries MediaStore URI first, falls back to `takePersistableUriPermission()` for cloud-backed providers. Never store raw PhotoPicker URIs. |
| 0.9  | Update `FileHashUtil` to compute hash from content URI (InputStream)        | ✅     | Must work with both `content://` URIs and MediaStore URIs. Used for dedup check across all source types. **Dedup behavior:** on hash match for `IMPORTED` → reuse existing file URI, skip copy. For `EXTERNAL` → just add reference (no file to dedup). |
| 0.10 | Update `ThumbnailUtil` to generate thumbnails from content URIs             | ✅     | Thumbnails still saved to app-private cache. Must handle both owned and external URIs. |
| 0.11 | Update all UI screens to load media from URIs instead of file paths         | ✅     | Timeline cards, MemoryDetailScreen, CaptureScreen preview. Coil supports content URIs natively. Use cached `width`/`height` for aspect ratio placeholders. |
| 0.12 | Add broken reference handling: detect unavailable URIs, show placeholder    | ✅     | When external reference is broken (file deleted from gallery), show "Original file removed" placeholder with option to remove from memory. Check availability when loading memory list (lightweight `ContentResolver.query()` existence check on `EXTERNAL` items). No background job — checked at list load and detail open. |
| 0.13 | "Import to Memly" action for external references                            | ✅     | Converts `EXTERNAL` → `IMPORTED`. Copies file to `Pictures/Memly/` via MediaStoreManager. Updates entity with new URI and `source = IMPORTED`. Available from MemoryDetailScreen context menu. Protects the file from future deletion by user in gallery. |
| 0.14 | Verify: full end-to-end flow                                                | ✅     | Test: (1) camera capture saves to `Pictures/Memly/`, visible in gallery. (2) picked photo with "Keep original" → reference works, shows in app. (3) picked photo with "Save to Memly" → copied, app-owned. (4) "Import to Memly" converts reference → owned. (5) files persist after app uninstall. (6) deleting external original → placeholder shown. (7) deleting app-owned memory → file removed from public storage. |

**Checkpoint:** All media files created in-app are stored in public directories via MediaStore and survive app uninstall. Users choose whether picked photos are referenced or imported. Three-state `MediaSource` model handles ownership correctly. Metadata is cached in Room. Broken references show a placeholder. "Import to Memly" converts external references to owned files.

---

## Section 1: Voice Memos

**Status:** NOT STARTED

Add audio recording support so users can attach voice memos to memories during capture and play them back in the detail view.

**Risks:**
- MediaRecorder lifecycle management across configuration changes and process death.
- Audio focus handling when other apps are playing media.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 1.1  | Add MediaRecorder permission (RECORD_AUDIO) to AndroidManifest             | ⬜     | Runtime permission request required on API 23+                |
| 1.2  | Create AudioRecorder utility class (start/stop/save using MediaRecorder)    | ⬜     | Wrap MediaRecorder lifecycle; output to `Music/Memly/` via MediaStoreManager |
| 1.3  | Add voice memo UI to CaptureScreen (record button, playback preview, delete) | ⬜    | Integrate into existing capture form layout                    |
| 1.4  | Store audio files in public storage via MediaStoreManager with hash dedup   | ⬜     | SHA-256 hash check before saving; uses new MediaStore flow from Section 0 |
| 1.5  | Add AUDIO to MediaType enum                                                 | ⬜     | Update Room entity and any exhaustive when-blocks              |
| 1.6  | Create audio playback component (play/pause/seek) using MediaPlayer         | ⬜     | Composable with seekbar and elapsed time display               |
| 1.7  | Display audio indicator on MemoryCard and DetailScreen                       | ⬜     | Icon or badge showing memory has attached audio                |
| 1.8  | Verify: record voice memo during capture, playback in detail                 | ⬜     | End-to-end test of record, save, navigate, play                |

**Checkpoint:** Can record a voice memo during memory capture. Audio file is saved and deduplicated. Playback works in the detail view with play/pause/seek controls.

---

## Section 2: Video Playback

**Status:** NOT STARTED

Replace placeholder video handling with proper video playback in the detail view using Media3 ExoPlayer.

**Risks:**
- ExoPlayer Compose integration requires careful lifecycle management.
- Memory leaks if the player instance is not released when navigating away or on configuration change.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 2.1  | Add Media3 ExoPlayer dependency to build.gradle                             | ⬜     | androidx.media3:media3-exoplayer and media3-ui                 |
| 2.2  | Create VideoPlayer composable (play/pause/seekbar/fullscreen toggle)        | ⬜     | Wrap PlayerView in AndroidView; expose controls via Compose    |
| 2.3  | Integrate video player in MemoryDetailScreen media gallery                  | ⬜     | Detect VIDEO MediaType in HorizontalPager; swap image for player |
| 2.4  | Video thumbnail extraction for timeline cards (MediaMetadataRetriever)      | ⬜     | Extract frame at first second; save to cache like image thumbs |
| 2.5  | Handle video orientation and aspect ratio                                   | ⬜     | Read rotation metadata; apply correct aspect ratio to player   |
| 2.6  | Memory management: release player on navigation away (DisposableEffect)     | ⬜     | Release in onDispose; stop playback on lifecycle pause         |
| 2.7  | Verify: play videos in detail view, thumbnails display in timeline          | ⬜     | Test with various video formats and orientations               |

**Checkpoint:** Videos play inline in the detail screen with full controls. Thumbnails appear on timeline cards. Player is properly released on navigation.

---

## Section 3: Enhanced Capture

**Status:** NOT STARTED

Improve the capture experience with multi-photo selection, reordering, and better feedback during save.

**Risks:**
- Low. Builds on the existing capture flow established in Phase 1.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 3.1  | Support selecting multiple photos/videos in a single capture session        | ⬜     | Extend existing Photo Picker usage to allow batch selection    |
| 3.2  | Draggable reorder of media items in capture form (before save)              | ⬜     | Long-press drag using LazyColumn or dedicated reorder library  |
| 3.3  | Media preview grid in CaptureScreen with remove button per item             | ⬜     | Grid of thumbnails; X button overlay to remove individual items |
| 3.4  | Progress indicator during save (hash computation + thumbnail generation)    | ⬜     | LinearProgressIndicator or dialog with step description        |
| 3.5  | Validation: require at least one piece of content (media OR notes) before save | ⬜  | Disable save button and show hint when both are empty          |
| 3.6  | Verify: add multiple photos, reorder, remove, save successfully             | ⬜     | Confirm media order persists and all items appear in detail    |

**Checkpoint:** Users can select multiple photos/videos, reorder them, remove unwanted items, and see progress feedback during save. Validation prevents empty memories.

---

## Section 4: Onboarding Flow

**Status:** NOT STARTED

First-launch guided experience that introduces the app and requests necessary permissions with context.

**Risks:**
- Low. Standard onboarding pattern with HorizontalPager.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 4.1  | Create OnboardingScreen with 3 welcome pages using HorizontalPager          | ⬜     | Page 1: Welcome to Memly and app concept. Page 2: Capture memories with emotion. Page 3: Explore timeline, map, and search. |
| 4.2  | Add DataStore preference for onboarding_completed flag                      | ⬜     | Boolean preference; checked at app startup to determine start destination |
| 4.3  | Request permissions during onboarding (camera, location, media) with rationale | ⬜  | Show explanation text before each permission request            |
| 4.4  | "Capture your first memory" CTA on final page, navigates to CaptureScreen  | ⬜     | Set onboarding_completed to true, then navigate                |
| 4.5  | Skip button on all pages                                                    | ⬜     | Marks onboarding complete and navigates to main timeline       |
| 4.6  | Verify: fresh install shows onboarding, subsequent launches skip it         | ⬜     | Clear app data to re-test; confirm DataStore flag works        |

**Checkpoint:** Fresh installs show the onboarding flow. Permissions are requested with rationale. Completing or skipping onboarding sets a persistent flag so it does not show again.

---

## Section 5: Theme & UI Polish

**Status:** COMPLETE

Memly visual identity fully implemented in Phase 1 — soft pastel palette, Poppins/Inter typography, mood-specific colors, and card designs. Phase 2 audit confirmed consistency across all screens.

**Risks:**
- ~~Low. Visual-only changes with no data or logic impact.~~

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 5.1  | Design custom color scheme: pastel primaries, warm surfaces, muted accents (light + dark) | ✅ | Full palette in Color.kt: Soft Coral, Warm Beige, Soft Teal + dark variants |
| 5.2  | Update Color.kt with the full Memly palette                                 | ✅     | Light + Dark colors for all Material 3 slots + MoodColors object |
| 5.3  | Update Theme.kt with custom light and dark color schemes                    | ✅     | lightColorScheme() + darkColorScheme() with all Memly colors; MemlyShapes |
| 5.4  | Update Typography with a warmer font family or adjust weights and sizes     | ✅     | Poppins (headings) + Inter (body) with all weights bundled as TTF |
| 5.5  | Mood-specific colors: assign a color to each Mood enum value               | ✅     | Mood.color() extension in MoodTheme.kt; 10 distinct mood colors |
| 5.6  | Add mood color indicators throughout the app (timeline cards, detail, chips) | ✅    | Mood chips on cards, detail hero, filter chips, map pins, accent strips |
| 5.7  | Polish card designs: rounded corners, subtle elevation, spacing consistency  | ✅     | Audit confirmed: 16dp cards, 12dp containers, 8dp chips — consistent |
| 5.8  | Verify: all screens reflect the new theme in both light and dark mode        | ✅     | All colors via MaterialTheme.colorScheme; dark scheme fully wired |

**Checkpoint:** App has a cohesive pastel visual identity. Mood colors appear as indicators on cards and chips. Light and dark themes are both polished and consistent.

---

## Section 6: Data Management

**Status:** NOT STARTED

Backup, restore, and export functionality for data safety and portability.

**Risks:**
- Medium. Large backup files may be slow to write and read. Import conflict resolution requires careful handling of duplicate detection by hash. File paths in backups may not be portable across devices or reinstalls.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 6.1  | Create BackupRepository with export and import logic                        | ⬜     | Central class coordinating serialization and file I/O          |
| 6.2  | JSON full backup: serialize all Room data (memories, media metadata, tags, collections) to JSON | ⬜ | Use kotlinx.serialization or Gson; write to external/shared storage |
| 6.3  | Include media file references (MediaStore URIs + relative paths) in backup metadata | ⬜ | Store `relativePath` + `displayName` for portability; URIs for same-device restore |
| 6.4  | JSON import: deserialize and insert into Room, handle conflicts (skip duplicates by hash) | ⬜ | Transaction-based insert; report skipped count to user        |
| 6.5  | CSV export: export memories as CSV (date, title, notes, mood, location, tags) | ⬜   | Flat format for spreadsheet consumption; escape commas in text |
| 6.6  | Share backup file via Android share sheet (ACTION_SEND)                     | ⬜     | Use FileProvider to share the JSON file                        |
| 6.7  | Share CSV via share sheet                                                   | ⬜     | Same share mechanism as JSON backup                            |
| 6.8  | Add backup/restore and export options to SettingsScreen                     | ⬜     | New section in settings with list items for each action        |
| 6.9  | Orphan file cleanup: scan `Pictures/Memly/` for files not referenced in DB  | ⬜     | Dev/settings tool: query `Pictures/Memly/` via MediaStore, compare against DB entries, offer to delete unreferenced files. Handles cases where DB row was deleted but file deletion failed (crash, user cancelled). |
| 6.10 | Verify: export JSON, clear data, import JSON, all memories restored         | ⬜     | Round-trip test confirming data integrity after restore. Also verify orphan cleanup correctly identifies and removes unreferenced files. |

**Checkpoint:** Users can export a full JSON backup and a CSV summary. Backups can be shared via the system share sheet. Importing a backup restores all memories, skipping duplicates. Orphan cleanup removes unreferenced files from Memly's public folder. Settings screen provides access to all data management actions.

---

## Section 7: Integration & Polish

**Status:** NOT STARTED

Final integration testing and edge case handling across all Phase 2 features.

**Risks:**
- Medium. Cross-feature interactions may surface unexpected issues (e.g., voice memo in backup, video playback after restore).

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 7.1  | End-to-end test of all Phase 2 features                                     | ⬜     | Walk through every new flow: voice, video, capture, onboarding, backup |
| 7.2  | Handle edge cases: recording interrupted, large video files, import failures | ⬜    | Graceful error messages; prevent data corruption               |
| 7.3  | Verify voice memos, video playback, onboarding, backup/restore all work together | ⬜ | Cross-feature scenarios (e.g., backup includes audio files)   |
| 7.4  | Update empty states and error messages for new features                      | ⬜     | Consistent messaging style; actionable hints for the user      |
| 7.5  | Final build and smoke test                                                   | ⬜     | Generate debug APK; test on physical device                    |

**Checkpoint:** All Phase 2 features work individually and together. Edge cases are handled gracefully. Debug APK runs on a real device without crashes.
