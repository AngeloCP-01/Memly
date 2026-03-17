# Architecture Decision Log

| ID   | Decision | Rationale |
|------|----------|-----------|
| D001 | MVVM with Repository pattern; no Clean Architecture / domain layer | Simplicity for MVP; avoids unnecessary abstraction |
| D002 | Single-module app organized by package (data, ui, di, navigation) | Faster builds, simpler structure for solo project |
| D003 | Room entities used directly across layers (no separate domain models) | Reduces boilerplate for MVP; can refactor later |
| D004 | Kotlin 2.0.21 + AGP 9.0.1; KSP over KAPT | AGP 9 drops KAPT support; KSP is faster |
| D005 | Hilt 2.59.2 | Minimum version with AGP 9 compatibility |
| D006 | Reference-first file management with SHA-256 hash dedup | Avoids duplicate media storage; content-addressable |
| D007 | Package name: `com.example.memly` | Standard convention for development |
| D008 | Soft pastel nostalgia theme with emotion-first UI | Warm, personal feel aligned with memory journaling |
| D009 | Bottom nav: Timeline, Map, Search + FAB for Capture | Core actions always reachable; capture is primary action |
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
| D023 | Media files stored in filesDir/media with UUID names | App-private; no MediaStore dependency; survives cache clearing |
| D024 | Dedup check skips insert silently when hash exists | Non-disruptive UX; user doesn't need to know about duplicates |
| D025 | Compose BOM 2025.05.00 | Aligns foundation (FlowRow) with newer navigation 2.9.7 / lifecycle 2.10.0 deps |
| D026 | Three MemoryCard variants in shared `ui/components/` package | Timeline, carousel, and search cards have distinct layouts; shared file avoids duplication |
| D027 | Time Hop query uses SQLite strftime for month-day matching | Cross-year date matching without loading all memories into memory |
| D028 | Date grouping computed in ViewModel via Kotlin groupBy | Avoids re-computation on recomposition; clean separation from UI |
| D029 | Edit fields stored in UiState alongside read fields | Allows cancel without re-fetching; edit* prefix distinguishes from display values |
| D030 | Tag updates as individual add/remove (not transactional) | Acceptable for local-only app; can wrap in transaction later if needed |
| D031 | SavedStateHandle for memoryId in detail ViewModel | Standard Hilt pattern for navigation arguments; survives process death |
| D032 | osmdroid (OpenStreetMap) over Google Maps for map view | No API key needed, truly offline-capable, aligns with offline-first philosophy; tradeoff is AndroidView wrapper |
