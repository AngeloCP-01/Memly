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
Section 1 (Voice Memos)  ──┐
Section 2 (Video Playback)  ├──> Section 7 (Integration)
Section 3 (Enhanced Capture)│
Section 4 (Onboarding)     ─┤
Section 5 (Theme Polish)   ─┤
Section 6 (Data Management) ┘
```

Sections 1 through 6 are mostly independent and can proceed in parallel.
Section 7 depends on all prior sections and is the final integration pass.

---

## Summary Table

| Section | Name                 | Tasks | Complexity | Risk   |
|---------|----------------------|-------|------------|--------|
| 1       | Voice Memos          | 8     | Medium     | Medium |
| 2       | Video Playback       | 7     | Medium     | Medium |
| 3       | Enhanced Capture     | 6     | Low        | Low    |
| 4       | Onboarding Flow      | 6     | Low        | Low    |
| 5       | Theme & UI Polish    | 8     | Low        | Low    |
| 6       | Data Management      | 9     | High       | Medium |
| 7       | Integration & Polish | 5     | Medium     | Medium |
|         | **Total**            | **49**|            |        |

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
| 1.2  | Create AudioRecorder utility class (start/stop/save using MediaRecorder)    | ⬜     | Wrap MediaRecorder lifecycle; output to app-private storage    |
| 1.3  | Add voice memo UI to CaptureScreen (record button, playback preview, delete) | ⬜    | Integrate into existing capture form layout                    |
| 1.4  | Store audio files in app storage with hash dedup (reuse FileHashUtil)       | ⬜     | SHA-256 hash check before saving; consistent with media dedup  |
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
| 6.3  | Include media file references (paths) in backup metadata                    | ⬜     | Store relative paths so backup is somewhat portable            |
| 6.4  | JSON import: deserialize and insert into Room, handle conflicts (skip duplicates by hash) | ⬜ | Transaction-based insert; report skipped count to user        |
| 6.5  | CSV export: export memories as CSV (date, title, notes, mood, location, tags) | ⬜   | Flat format for spreadsheet consumption; escape commas in text |
| 6.6  | Share backup file via Android share sheet (ACTION_SEND)                     | ⬜     | Use FileProvider to share the JSON file                        |
| 6.7  | Share CSV via share sheet                                                   | ⬜     | Same share mechanism as JSON backup                            |
| 6.8  | Add backup/restore and export options to SettingsScreen                     | ⬜     | New section in settings with list items for each action        |
| 6.9  | Verify: export JSON, clear data, import JSON, all memories restored         | ⬜     | Round-trip test confirming data integrity after restore         |

**Checkpoint:** Users can export a full JSON backup and a CSV summary. Backups can be shared via the system share sheet. Importing a backup restores all memories, skipping duplicates. Settings screen provides access to all data management actions.

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
