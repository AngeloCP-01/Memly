# CLAUDE.md -- Memly Autopilot

This file is loaded automatically at the start of every Claude conversation.
It contains everything Claude needs to work on the Memly project autonomously.

---

## Project Overview

**App:** Memly -- offline-first Android personal memory journal
**Package:** `com.example.memly`
**Current State:** Phase 2 in progress. Sections 0 (File Management Refactor), 1 (Voice Memos), 2 (Video Playback), 3 (Enhanced Capture), 4 (Onboarding Flow), and 5 (Theme & UI Polish) complete. Sections 6-7 remaining. Onboarding: 3-page HorizontalPager with DataStore preferences for first-launch gating, bulk permission requests, and "Capture Your First Memory" CTA. Enhanced capture: 3-column media preview grid with tap-to-swap reorder, sortOrder persistence, save progress indicator, validation, and map-based place picker (osmdroid + Nominatim). Video recording added via system camera with Photo/Video dialog (CaptureVideo contract). Video playback via Media3 ExoPlayer with lifecycle management, aspect ratio handling, and play icon indicators on all cards. Voice memo recording and playback implemented with AudioRecorder utility, AudioPlaybackBar composable, AUDIO MediaType, and audio indicators on all cards. MediaStore-based public storage with three-state MediaSource model. Major UI redesign complete. Bug fixes: dedup reuses media references instead of skipping, URI resolution via name+size match, broken image fallback on timeline cards. Ad-hoc fixes: edit mode supports media add/remove, collection dialog has checkboxes, location uses lastLocation fallback, add-memory available from collection detail screen. Collection detail redesigned as 2-column grid with stacked polaroid-style photo cards. Memory detail redesigned with BottomSheetScaffold (full-screen media carousel + draggable bottom sheet), mood-based color theming, and non-cropping rounded-corner images. Edit screen now has full parity with add screen: location buttons, date picker, 3-column media grid with order badges. Edit/Save moved from FAB to full-width buttons in bottom sheet. Map and place picker default to Philippines. Place picker has custom zoom controls above My Location button. Full-screen media viewer on detail screen with swipe navigation, pinch-to-zoom, double-tap zoom, and video playback. CameraX in-app camera with photo/video toggle planned for Phase 3, Section 7.
**Scope:** 6 phases, ~300 tasks total. Free, no ads, no accounts, 100% offline (Phase 4 adds optional cloud sync).

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin (no Java) |
| UI | Jetpack Compose, Material 3 |
| Database | Room |
| DI | Hilt |
| Navigation | Navigation Compose (single Activity) |
| Async | Coroutines + Flow |
| Images | Coil |
| compileSdk | 36 |
| minSdk | 28 |
| targetSdk | 36 |
| AGP | 9.0.1 |
| Kotlin | 2.0.21 |

## Architecture

**Pattern:** MVVM with Repository -- two layers only (UI + Data). NOT Clean Architecture. No domain/usecase layer. ViewModels call repositories directly.

**Data flow:** Screen -> ViewModel -> Repository -> DAO -> Room

### Layer Rules

| Layer | Contains | May Import |
|-------|----------|------------|
| `data/` | Room entities, DAOs, Repositories | Only data-layer code |
| `ui/` | Composables, ViewModels | Data layer (repositories, entities) |
| `di/` | Hilt modules | Both layers (wiring) |
| `util/` | Standalone utilities | Standard library, Android SDK |

### Key Patterns

- ViewModels expose `StateFlow<UiState>`. Screens collect via `collectAsStateWithLifecycle()`.
- Navigation uses a `Screen` sealed class with Jetpack Navigation Compose.
- Entities are used directly -- no separate domain models unless mapping is genuinely needed.
- Enums are stored as strings in Room.

## File Organization

```
app/src/main/java/com/example/memly/
├── data/
│   ├── local/
│   │   ├── dao/         (MemoryDao, TagDao, CollectionDao)
│   │   ├── entity/      (MemoryEntity, MediaFileEntity, TagEntity, CollectionEntity, Mood, MediaType, CrossRefs, MemoryWithDetails)
│   │   └── MemlyDatabase.kt
│   └── repository/      (MemoryRepository, CollectionRepository)
├── ui/
│   ├── theme/           (Color, Type, Theme)
│   ├── navigation/      (Screen, MemlyNavGraph)
│   ├── timeline/        (TimelineScreen, TimelineViewModel)
│   ├── map/             (MapScreen placeholder)
│   ├── capture/         (CaptureScreen)
│   ├── detail/          (MemoryDetailScreen)
│   └── components/      (shared composables)
├── di/                  (DatabaseModule)
├── util/                (FileHashUtil, ThumbnailUtil)
├── MemlyApplication.kt
└── MainActivity.kt
```

## Coding Conventions

### Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `MemoryRepository` |
| Functions | camelCase | `getMemoryById()` |
| Constants | SCREAMING_SNAKE | `MAX_PHOTO_SIZE` |
| DB tables | snake_case | `@Entity(tableName = "memories")` |

### Component Patterns

| Component | Pattern |
|-----------|---------|
| Screens | `@Composable fun <Name>Screen(...)`, receive VM via `hiltViewModel()` |
| ViewModels | `@HiltViewModel class <Name>ViewModel @Inject constructor(...)`, expose `StateFlow<UiState>` |
| Entities | `@Entity(tableName = "snake_case")`, PK with `autoGenerate = true`, default `id: Long = 0` |
| DAOs | Interface with `@Dao`, `suspend` for writes, `Flow<>` for reads |
| Repositories | `@Singleton class <Name>Repository @Inject constructor(...)` |

## Design Guidelines

- Full spec in `docs/ui-design-guide.md`
- Light-first theme. Primary: Soft Coral `#FF6B6B`, Accent: Soft Teal `#4BC0C8`, Surface: Warm Beige `#FFF3E6`
- Typography: Poppins (headings), Inter (body)
- Emotion-first interface, each Mood has a distinct color (see `MoodTheme.kt`)
- Bottom nav: Timeline, Map, Search + FAB for Capture
- Cards: 16dp rounded corners, subtle elevation, mood color indicators, tag chips

## Git Conventions

Conventional Commits format. Always include scope.

| Type | Usage |
|------|-------|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Code restructuring, no behavior change |
| `chore` | Build, config, tooling |
| `docs` | Documentation only |
| `style` | Formatting, no logic change |
| `test` | Adding or updating tests |
| `perf` | Performance improvement |

**Examples:**
- `feat(capture): add photo picker and mood selector`
- `fix(timeline): prevent crash on empty memory list`
- `chore(deps): update Room to 2.7.1`

## Session Protocol

### START

1. This file (CLAUDE.md) auto-loads.
2. Read `memory/progress.md` to find the current state pointer.
3. Continue from where the last session left off.

### WORK

1. Complete tasks in section order as defined in the active phase task file.
2. Follow established patterns in `memory/patterns.md`.
3. Commit at the end of each section with a conventional commit message.
4. One conversation = one section. Never leave a section half-done.

### END

Update all of the following before the final commit:

| File | What to Update |
|------|---------------|
| `memory/progress.md` | Mark section complete, set next state pointer |
| `memory/patterns.md` | Add any new patterns established |
| `memory/decisions.md` | Log any architecture decisions made |
| `memory/MEMORY.md` | Update index if new memory files were created |
| `CLAUDE.md` | Update "Current State" in Project Overview |
| Active phase task file (e.g., `docs/phase1-tasks.md`) | Check off completed tasks |
| `CHANGELOG.md` | Add section completion summary |

### Context Recovery (mid-session or new session on same section)

1. Re-read `memory/progress.md`
2. Re-read active phase task file
3. Re-read `memory/patterns.md`
4. Read specific source files being edited

## Code Review Subagent

After completing each section, launch a background code review agent:

1. Read `memory/review-agent.md` for the prompt template.
2. Fill in the template variables: section name, files changed, patterns to verify.
3. Launch as a background agent to review the completed section.
4. Address any findings before the final commit.

## Autonomous Decision Rules

### Decide Alone

- Library version choices
- Implementation details within the established architecture
- Naming of variables, functions, classes (following conventions above)
- UI layout decisions within the design constraints
- Error handling strategies
- Build configuration fixes
- File placement within the existing structure

### Escalate to User

- Changes to PRD features or scope
- Breaking changes to the data model
- Major architectural shifts (e.g., adding a domain layer)
- Changes that affect multiple phases
- Destructive actions (deleting files, resetting state)
- Package name changes
- Adding new third-party libraries not already in the stack

## Key Documentation

| File | Content |
|------|---------|
| `CHANGELOG.md` | Section completion summaries |
| `docs/PRD.md` | Product requirements, data model, features |
| `docs/Architecture.md` | High-level architecture, layers, threading |
| `docs/system-design.md` | Detailed system designs: ER diagram, capture flow, file management, navigation, state management, location, thumbnails |
| `docs/ui-design-guide.md` | Colors, typography, spacing, component specs, UX patterns |
| `docs/AppDescription.md` | Organized brainstorm reference |
| `docs/phase1-tasks.md` | Phase 1: Foundation & MVP (73 tasks, 8 sections) |
| `docs/phase2-tasks.md` | Phase 2: Enhanced Experience (49 tasks, 7 sections) |
| `docs/phase3-tasks.md` | Phase 3: Insights & Engagement (48 tasks, 7 sections) |
| `docs/phase4-tasks.md` | Phase 4: Cloud Sync / Firebase (50 tasks, 7 sections) |
| `docs/phase5-tasks.md` | Phase 5: AI & Premium Features (40 tasks, 6 sections) |
| `docs/phase6-tasks.md` | Phase 6: Launch & Growth (43 tasks, 7 sections) |

## Memory Files

Memory files exist in BOTH `~/.claude/projects/.../memory/` AND `memory/` in the project root. Keep them in sync.

| File | Purpose |
|------|---------|
| `memory/MEMORY.md` | Index of all memory files |
| `memory/progress.md` | Per-section status tracker with state pointer |
| `memory/patterns.md` | Established code patterns to follow |
| `memory/decisions.md` | Architecture decision log |
| `memory/review-agent.md` | Code review subagent prompt template |

## MCP Tools

| Tool | Usage |
|------|-------|
| Serena | Codebase navigation and editing. Prefer symbolic tools (find_symbol, replace_symbol_body) over raw text editing. |
| Sequential Thinking | Use for complex multi-step planning before implementation. |
| Context7 | Library documentation lookup. Always call `resolve-library-id` first, then `query-docs`. |
