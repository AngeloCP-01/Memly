# Architecture Decision Log

| ID   | Decision | Rationale |
|------|----------|-----------|
| D001 | MVVM with Repository pattern; no Clean Architecture / domain layer | Simplicity for MVP; avoids unnecessary abstraction |
| D002 | Single-module app organized by package (data, ui, di, navigation) | Faster builds, simpler structure for solo project |
| D003 | Room entities used directly across layers (no separate domain models) | Reduces boilerplate for MVP; can refactor later |
| D004 | Kotlin 2.0.21 + AGP 9.0.1; KSP over KAPT | AGP 9 drops KAPT support; KSP is faster |
| D005 | Hilt 2.59.2 | Minimum version with AGP 9 compatibility |
| D006 | ~~Reference-first file management with SHA-256 hash dedup~~ Superseded by D055 | ~~Avoids duplicate media storage~~ See D055 |
| D007 | Package name: `com.example.memly` | Standard convention for development |
| D008 | Soft pastel nostalgia theme with emotion-first UI | Warm, personal feel aligned with memory journaling |
| D009 | ~~Bottom nav: Timeline, Map, Search + FAB for Capture~~ Superseded by D048 | ~~Core actions always reachable~~ See D048 |
| D010 | Enums (Mood, MediaType) stored as strings in Room | Readable in DB, survives ordinal changes |
| D011 | String-based navigation routes (not type-safe) | Simpler setup; sufficient for current scale |
| D012 | `android.disallowKotlinSourceSets=false` in gradle.properties | Required for KSP source set compatibility with AGP 9 |
| D013 | Light-first theme: Soft Coral #FF6B6B + Warm Beige #FFF3E6 + Soft Teal #4BC0C8 | Warm nostalgic palette; light-first suits personal memory app |
| D014 | Typography: Poppins (headings) + Inter (body), bundled as static TTF | Friendly, modern sans-serif; Google Fonts, free |
| D015 | Image-dominant memory cards with gradient overlay text | Inspired by Ronas IT travel app; photo-first visual journal feel |
| D016 | 3:4 portrait aspect ratio for memory card images | Matches phone photo aspect; gives images room to breathe |
| D017 | Three card variants: timeline (image+overlay), carousel (compact), search (horizontal row) | Different contexts need different density/detail levels |
| D018 | Full-bleed photo hero in detail screen | Immersive; image fills top half, metadata scrolls below |
| D019 | No dynamic color (Material You) — custom Memly palette always used | Memly has its own brand identity; dynamic color would dilute it |
| D020 | CaptureViewModel uses @ApplicationContext Context injection | Needs context for file I/O (copy media, generate thumbnails, compute hashes); avoids AndroidViewModel |
| D021 | TakePicture contract instead of CameraX for camera capture | Simpler; avoids CameraX build complexity; sufficient for capture-only use case |
| D022 | play-services-location 21.3.0 for GPS | FusedLocationProviderClient provides best location with minimal setup |
| D023 | ~~Media files stored in filesDir/media with UUID names~~ Superseded by D055 | ~~App-private~~ See D055 |
| D024 | ~~Dedup check skips insert silently when hash exists~~ Superseded by D070 | ~~Non-disruptive UX~~ Skipping caused memories to save with zero media. See D070 |
| D025 | Compose BOM 2025.05.00 | Aligns foundation (FlowRow) with newer navigation 2.9.7 / lifecycle 2.10.0 deps |
| D026 | Three MemoryCard variants in shared `ui/components/` package | Timeline, carousel, and search cards have distinct layouts; shared file avoids duplication |
| D027 | Time Hop query uses SQLite strftime for month-day matching | Cross-year date matching without loading all memories into memory |
| D028 | Date grouping computed in ViewModel via Kotlin groupBy | Avoids re-computation on recomposition; clean separation from UI |
| D029 | Edit fields stored in UiState alongside read fields | Allows cancel without re-fetching; edit* prefix distinguishes from display values |
| D030 | Tag updates as individual add/remove (not transactional) | Acceptable for local-only app; can wrap in transaction later if needed |
| D031 | SavedStateHandle for memoryId in detail ViewModel | Standard Hilt pattern for navigation arguments; survives process death |
| D032 | osmdroid (OpenStreetMap) over Google Maps for map view | No API key needed, truly offline-capable, aligns with offline-first philosophy; tradeoff is AndroidView wrapper |
| D033 | LIKE queries for search instead of FTS4 | Simpler setup; sufficient for MVP scale; FTS4 can be added later if needed |
| D034 | Add-to-collection via toggle dialog on detail screen | Cleaner UX than long-press; direct and discoverable; avoids complex multi-select |
| D035 | Collection memory counts via combine of individual count flows | Reactive; updates when memories added/removed; acceptable for small collection counts |
| D036 | SettingsViewModel injects MemlyDatabase + MemoryDao directly (not via repository) | Settings needs clearAllTables() and count queries; no repository abstraction needed for these |
| D037 | ~~Settings accessible from Search screen shortcut~~ Superseded by D048 | ~~Keeps bottom nav focused~~ Settings is now a bottom nav tab |
| D038 | Double confirmation for clear-all-data | Prevents accidental data loss; second dialog shows exact counts for informed decision |
| D039 | Flow-based ViewModels skip try/catch (Timeline, Map, Search) | Room flows handle errors internally; adding catch would be dead code for local DB |
| D040 | Custom app icon deferred to Phase 2 | Default launcher icon sufficient for MVP; proper adaptive icon needs design assets |
| D041 | Permission rationale dialogs deferred to Phase 2 | Basic permission flow works; advanced rationale + settings link is Phase 2 polish |
| D042 | Timeline redesigned from LazyColumn list to HorizontalPager with stacked cards | Inspired by travel app UI; more visual, photo-first experience matching memory journal concept |
| D043 | Stacked card effect via negative pageSpacing + zIndex | Creates depth illusion showing next card behind current; indicates more content without traditional dots |
| D044 | Nested HorizontalPager for per-card image slideshow | Auto-sliding images within each memory card; only active card runs timer for performance |
| D045 | Coverflow pager replaces stacked overlap effect | 3D Y-rotation on side cards matches reference design; more visual depth than flat stacking |
| D046 | @location format instead of pin icon | Cleaner, social-media-inspired location display; matches design reference |
| D047 | Frosted glass "See more +" pill button | Semi-transparent white (0.2 alpha) blends with image; less intrusive than solid white button |
| D048 | Bottom nav: Home, Collections, +Add, Map, Settings (4 tabs + center FAB) | All core destinations reachable from nav; Search removed as standalone screen |
| D049 | Search screen removed; search inlined into Timeline and Collections screens | Reduces navigation depth; search is contextual to the screen you're on |
| D050 | Multi-select mood filter with Set<Mood> instead of single Mood? | Users often want to see memories from multiple moods at once |
| D051 | Date filter via DatePickerDialog (specific day) instead of sort order | More useful to find memories from a specific day than just sorting |
| D052 | Top-level nav destinations use custom headers, not TopAppBar | Matches redesigned warm/card-based design language; no back arrow on main tabs |
| D053 | CollectionDao.searchCollections() uses LIKE on name + description | Simple and effective for local search; matches existing memory search pattern |
| D054 | Settings icon changed from GridView to Outlined.Settings (gear) | More recognizable/standard icon for settings |
| D055 | MediaStore-based public storage with three-state MediaSource model | In-app content (camera/audio) saved to `Pictures/Memly/`, `Movies/Memly/`, `Music/Memly/` via MediaStore — survives uninstall, visible in gallery. Picked content referenced by URI (EXTERNAL) or optionally copied (IMPORTED). Replaces app-private filesDir approach. Eliminates storage duplication for references. |
| D056 | MediaSource enum: APP_OWNED, EXTERNAL, IMPORTED | Three-state ownership model. APP_OWNED = created in-app, lives in public Memly folder. EXTERNAL = URI reference to gallery photo, zero storage cost. IMPORTED = user chose "Save to Memly", copied to public folder, app owns it. Replaces boolean isReference flag. |
| D057 | URI resolution: MediaStore first, SAF fallback | PhotoPicker URIs are temporary — must resolve to stable MediaStore content URI (query by _ID). If resolution fails (cloud-backed providers like Google Photos), fall back to takePersistableUriPermission(). Never store raw PhotoPicker URIs. |
| D058 | File naming: memly_\<timestamp\>_\<shortId\>.\<ext\> | Structured convention for files in public Memly folders. Sortable, traceable, no collisions. Replaces hash-based and UUID-based naming. |
| D059 | Deletion by source: owned=ContentResolver, external=DB only | APP_OWNED/IMPORTED files deleted via ContentResolver (with createDeleteRequest fallback on Android 11+). EXTERNAL references only remove DB row, never touch original file. |
| D060 | Cache media metadata in entity at capture time | Store mimeType, size, dateTaken, width, height in MediaFileEntity. Avoids repeated MediaStore queries for display. |
| D061 | AudioRecorder wraps MediaRecorder; records to cacheDir temp file | Records AAC @ 128kbps/44.1kHz in M4A container. Temp file in cacheDir/audio/, then saved to Music/Memly/ via MediaStoreManager on save. Avoids partial recordings in public storage. |
| D062 | AudioPlaybackBar uses MediaPlayer with prepareAsync pattern | LaunchedEffect + Dispatchers.IO for prepare(). Avoids main thread blocking. Error fallback UI for unavailable URIs. DisposableEffect releases player. |
| D063 | durationMs cached on MediaFileEntity | Avoids re-querying MediaMetadataRetriever at display time. Nullable for backward compat with existing media. |
| D064 | Audio files excluded from visual pagers/slideshows; shown via separate AudioPlaybackBar | Audio has no visual representation — filtering prevents blank pager pages. AudioPlaybackBar shown below photo hero on DetailScreen and inline on CaptureScreen. |
| D065 | Voice memo uses isFromCamera=true to route as APP_OWNED | Semantic mismatch (not a camera), but avoids adding a new field. Works because the routing logic only cares about "app-created vs picked". Minor tech debt noted for future rename to isAppCreated. |
| D066 | ~~Bottom nav: 4 tabs + center inline FAB~~ Superseded by D067 | ~~FAB embedded in nav row~~ See D067 |
| D067 | Bottom nav: curved cutout with overlapping FAB, per-screen action | Bar always has curved notch cutout. FAB overlaps top of bar via offset. FAB visible on Timeline (→ Capture) and CollectionList (→ create dialog). Hides with scale animation on other screens. Cutout always present for visual consistency. |
| D068 | Collection create trigger via incrementing counter from MainActivity | FAB click on CollectionList increments `createCollectionTrigger` int, passed through NavGraph to CollectionListScreen. LaunchedEffect watches changes and calls `viewModel.showCreateDialog()`. Avoids coupling ViewModel to nav layer. |
| D069 | ~~Dedup skips insert silently when hash exists~~ Superseded by D024 update | ~~Non-disruptive UX~~ See D070 |
| D070 | Dedup reuses existing file reference instead of skipping | When `findMediaByHash` finds an existing media entry, create a new `MediaFileEntity` row referencing the same URI/metadata. Avoids disk duplication (no second copy) while allowing the same photo in multiple memories. Fixes bug where memories were saved with zero media. |
| D071 | URI resolution: name+size match over _ID extraction | Photo Picker `_ID` is internal to the picker provider, not the real MediaStore row ID. Strategy 1 queries picker URI for `DISPLAY_NAME` + `SIZE`, then searches external MediaStore for a match. Requires `READ_MEDIA_IMAGES` permission. Direct `_ID` kept as fallback (Strategy 2). |
| D072 | Runtime permission gate for "Keep Original" | `READ_MEDIA_IMAGES` (API 33+) or `READ_EXTERNAL_STORAGE` requested when user taps "Keep Original". If denied, falls back to "Save to Memly" (copy) with a toast. Pending URIs preserved via `hideImportChoiceDialog()` during permission flow. |
| D073 | Broken reference detection uses openInputStream for all sources | `query()` alone can succeed on dead/expired URIs. `openInputStream()` is the definitive readability test. Applied to all sources (not just EXTERNAL) to catch dead picker URIs stored as any source type. |
| D074 | Timeline card broken image fallback via LaunchedEffect readability check | `LaunchedEffect` runs `openInputStream` on `Dispatchers.IO` keyed on `fullResUri`. If unreadable, shows `BrokenImage` icon + "Image unavailable" text instead of blank card. Avoids relying on Coil error state which doesn't fire reliably for dead content URIs. |
| D075 | Media editing in detail screen reuses CaptureViewModel's processMediaItem logic | Duplicated processMediaItem in MemoryDetailViewModel rather than extracting shared utility. Acceptable for now; can refactor to shared MediaProcessor if a third consumer appears. |
| D076 | Location fallback: lastLocation → getCurrentLocation(BALANCED) | `getCurrentLocation(HIGH_ACCURACY)` often returns null on real devices (GPS not locked). `lastLocation` is instant and works indoors. BALANCED_POWER_ACCURACY as fallback is more reliable than HIGH_ACCURACY for a memory app. |
| D077 | Add memories to collection from CollectionDetailScreen via FAB + dialog | Previously only possible from MemoryDetailScreen. FAB + checkbox dialog allows bulk management. CollectionDetailViewModel now depends on MemoryRepository for full memory list. |
| D078 | Collection dialog UX: checkboxes + borders + hint text | Plain Surface items were not visually clickable. Checkboxes provide immediate affordance. Border highlights reinforce selected state. "Tap to toggle" hint for first-time users. |
| D079 | Media3 ExoPlayer 1.6.0 for video playback | Modern, well-supported library replacing deprecated ExoPlayer standalone. `PlayerView` provides built-in controls (play/pause/seek/fullscreen) — no need to build custom UI. `RESIZE_MODE_FIT` handles aspect ratio and orientation automatically. |
| D080 | VideoPlayer keyed on videoUri with merged DisposableEffect | `remember(videoUri)` ensures player recreates if URI changes. Single `DisposableEffect(videoUri, lifecycleOwner)` handles lifecycle observer + release — avoids disposal ordering ambiguity from separate effects. |
| D081 | Video play icon indicator on all card variants | `PlayCircle` badge distinguishes videos from photos at a glance. Applied consistently across MemoryCard (top-left row with audio), MemoryCarouselCard (top-right), MemorySearchResultCard (center overlay), and timeline slideshow (center overlay). |
| D082 | Video recording via system camera with Photo/Video dialog | Camera button shows AlertDialog to choose Photo or Video, then launches `TakePicture` or `CaptureVideo` contract. Deferred: custom CameraX in-app camera with photo/video toggle (Messenger-like UX) planned for a future phase. |
| D083 | Video thumbnails use thumbnailPath + File model in Coil | Coil's AsyncImage cannot decode video content URIs (mediaStoreUri). For VIDEO media, use pre-generated `thumbnailPath` loaded as `java.io.File` instead of `Uri.parse()`. Applied to MemoryPagerCard and MemoryCard. MemoryCarouselCard and MemorySearchResultCard already used thumbnailPath. |
| D084 | Tap-to-swap reorder instead of drag-and-drop | Reliable without external library. Tap to select first item (highlighted with swap icon), tap second to swap. Works well for small media collections in capture form. |
| D085 | sortOrder field on MediaFileEntity | Explicit media ordering instead of relying on insertion order. Set from list index during save. DAO query orders by `sortOrder ASC`. DB version bumped 3→4. |
| D086 | osmdroid + Nominatim for place picker (not Google Maps) | Consistent with existing map (osmdroid). No API key, no billing, offline-friendly map tiles. Nominatim provides free geocoding/search. Aligns with offline-first, free app philosophy. |
| D087 | PlacePickerDialog as full-screen Dialog composable | Full-screen dialog keeps user in capture flow context. Uses osmdroid MapView with MapEventsOverlay for tap-to-select, Nominatim HTTP API for search + reverse geocoding. Returns lat/lng + place name via callback. |
| D088 | DataStore Preferences over SharedPreferences | Modern, Kotlin-coroutines-native, type-safe preferences. Single `preferencesDataStore` delegate at file level. First use: `onboarding_completed` flag. |
| D089 | Onboarding start destination with null loading gate | `collectAsState(initial = null)` — show blank background while DataStore loads, then create NavHost with correct start destination. Avoids race condition where NavHost starts on Timeline then DataStore emits false. |
| D090 | No ViewModel for OnboardingScreen | Screen is stateless — only writes a boolean on completion. All state is local (pager position, permission launcher). Adding a ViewModel would be over-engineering for this case. |
| D091 | Bulk permission request on onboarding completion | Fire-and-forget: request all permissions at once (camera, location, audio, media). Results don't block onboarding. Permissions re-requested in-context when needed (e.g., camera button, location fetch). |
