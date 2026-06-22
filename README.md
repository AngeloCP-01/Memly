# Memly

> An offline-first personal memory journal for Android — capture life's moments with photos, videos, voice memos, and notes, each tagged with a *mood*, a *place*, and a *story*.

<p align="left">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Language" src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white">
  <img alt="UI" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="minSdk" src="https://img.shields.io/badge/minSdk-28-blue">
  <img alt="targetSdk" src="https://img.shields.io/badge/targetSdk-36-blue">
  <img alt="Offline first" src="https://img.shields.io/badge/Offline-First-FF6B6B">
</p>

Memly is a 100% offline, no-accounts, no-ads journaling app built as a showcase of modern
Android engineering: Jetpack Compose, MVVM, Room, Hilt, Coroutines/Flow, and careful handling
of Android's scoped-storage media model.

> **A note on the build:** This repo is developed under the name **Memly**, but the shipping
> build is personalized as **"Catt"** — a gift app for someone named Cattleya, complete with a
> custom launcher icon and a personal greeting on the timeline. The codebase, package, and
> documentation all use *Memly*; the *Catt* branding is just the skin on top.

---

## 📸 Screenshots

> _Placeholders — drop the final images into `docs/ss/` and update the paths below._

| Timeline | Memory Detail | Capture |
|:---:|:---:|:---:|
| ![Timeline](docs/ss/placeholder-timeline.png) | ![Detail](docs/ss/placeholder-detail.png) | ![Capture](docs/ss/placeholder-capture.png) |

| Map | Collections | Place Picker |
|:---:|:---:|:---:|
| ![Map](docs/ss/placeholder-map.png) | ![Collections](docs/ss/placeholder-collections.png) | ![Place Picker](docs/ss/placeholder-placepicker.png) |

---

## ✨ Features

- **Rich capture** — photos, videos, voice memos, and text notes in a single memory, with a
  reorderable media grid and an in-flow save progress indicator.
- **Emotion-first** — tag every memory with one of 10 moods, each with its own color that
  themes the cards, map pins, and detail screen.
- **Places & maps** — pin memories to locations via a map-based place picker (osmdroid +
  Photon autocomplete + Nominatim reverse-geocoding), then revisit them on a world map with
  mood-colored pins and 4 switchable tile styles.
- **Timeline** — a 3D, swipeable card pager with auto-advancing photo slideshows, plus mood
  and date filtering, live search, and "Time Hop" (memories from this day in past years).
- **Collections** — group memories into albums, shown as stacked polaroid-style cards.
- **Full-screen viewer** — pinch-to-zoom, double-tap zoom, swipe navigation, and inline video
  playback.
- **Smart storage** — a three-state media-ownership model (see below) that balances durability
  against storage cost, with SHA-256 deduplication so the same photo is never copied twice.
- **Onboarding** — a 3-page intro with bulk permission requests and a "Capture Your First
  Memory" call to action.
- **Light & dark themes**, custom typography (Poppins + Inter), and a bespoke cutout bottom
  navigation bar.

---

## 🛠 Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin (no Java) |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM + Repository (UI + Data layers only) |
| Database | Room (KSP) |
| Dependency Injection | Hilt |
| Navigation | Navigation Compose (single Activity) |
| Async | Coroutines + Flow / StateFlow |
| Images | Coil 3 |
| Maps | osmdroid + Photon / Nominatim geocoding |
| Video | Media3 / ExoPlayer |
| Location | Google Play Services Location |
| Preferences | DataStore |
| Build | AGP 9, Gradle Version Catalog, compileSdk 36 |

---

## 🏗 Architecture

Memly uses **MVVM with a Repository layer** — two layers only (UI + Data), no Clean
Architecture, no domain/usecase layer. ViewModels talk to repositories directly.

```
Screen (Composable)
   │  collects StateFlow<UiState>, sends events
   ▼
ViewModel (@HiltViewModel, exposes StateFlow<UiState>)
   │
   ▼
Repository (@Singleton, transactional writes)
   │
   ▼
DAO (Room, suspend writes / Flow reads)
   │
   ▼
Room Database
```

| Layer | Contains | May import |
|-------|----------|-----------|
| `data/` | Room entities, DAOs, repositories | Only data-layer code |
| `ui/` | Composables, ViewModels | Data layer |
| `di/` | Hilt modules | Both layers |
| `util/` | Standalone utilities | Standard library, Android SDK |

**Conventions:** ViewModels expose a single `StateFlow<UiState>`; screens collect with
`collectAsStateWithLifecycle()`. Navigation is a `Screen` sealed class. Entities are used
directly (no separate domain models). Enums are stored as strings in Room.

### Project structure

```
app/src/main/java/com/example/memly/
├── data/
│   ├── local/
│   │   ├── dao/         MemoryDao, TagDao, CollectionDao
│   │   ├── entity/      Memory, MediaFile, Tag, Collection, Mood,
│   │   │                MediaType, MediaSource, cross-refs, MemoryWithDetails
│   │   ├── MemlyDatabase.kt
│   │   └── OnboardingPreferences.kt   (DataStore)
│   └── repository/      MemoryRepository, CollectionRepository
├── ui/
│   ├── theme/           Color, Type, Shape, Theme, MoodTheme
│   ├── navigation/      Screen, MemlyNavGraph
│   ├── timeline/        TimelineScreen + ViewModel
│   ├── map/             MapScreen + ViewModel
│   ├── capture/         CaptureScreen + ViewModel
│   ├── detail/          MemoryDetailScreen + ViewModel
│   ├── collection/      List + Detail screens & ViewModels
│   ├── onboarding/      OnboardingScreen
│   ├── settings/        SettingsScreen + ViewModel
│   └── components/      MemoryCard, PlacePickerDialog, VideoPlayer,
│                        AudioPlaybackBar, MemlyToast, bottom nav
├── di/                  DatabaseModule
├── util/                MediaStoreManager, AudioRecorder,
│                        ThumbnailUtil, FileHashUtil
├── MemlyApplication.kt
└── MainActivity.kt
```

---

## 🔬 Engineering highlights

A few problems that were interesting to solve:

- **Three-state media ownership** — every media file is `APP_OWNED` (captured in-app, copied to
  public `Pictures|Movies|Music/Memly/`), `IMPORTED` (a gallery file the user chose to copy in),
  or `EXTERNAL` (a zero-cost reference to a file that stays in the user's gallery). The UI
  surfaces broken `EXTERNAL` references and offers a one-tap "Import to Memly" recovery.
- **Robust MediaStore URI resolution** — picker URIs are resolved to stable MediaStore content
  URIs via a two-strategy fallback (display-name + size match, then direct-ID), with
  persistable-permission handling and a real `openInputStream` accessibility check (a
  `query()` alone will lie about readability).
- **Content-hash deduplication** — SHA-256 hashing means the same photo added to multiple
  memories reuses one stored file instead of duplicating bytes on disk.
- **Transactional writes** — creating a memory with its media and tags happens in a single Room
  transaction so a partial failure never leaves orphaned rows.
- **Lifecycle-aware media** — ExoPlayer pauses on background / releases on dispose; audio
  playback prepares off the main thread.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest stable)
- JDK 11+
- An Android device or emulator running **API 28+**

### Build & run
```bash
git clone git@github.com:angelitopaa/Memly.git
cd Memly
./gradlew assembleDebug      # or open in Android Studio and Run
```

No API keys or backend setup required — Memly is fully offline. (Map tiles and place search
use public OpenStreetMap-based services and need a network connection only while picking a
location.)

---

## 🗺 Roadmap

The project is planned across 6 phases. **Phases 1 & 2 are complete.**

| Phase | Focus | Status |
|-------|-------|:---:|
| 1 | Foundation & MVP (timeline, capture, Room) | ✅ Done |
| 2 | Enhanced experience (voice memos, video, onboarding, UI polish) | ✅ Done |
| 3 | Insights & engagement (in-app CameraX, map & data-management enhancements) | 🔜 Next |
| 4 | Optional cloud sync (Firebase) | ⏳ Planned |
| 5 | AI & premium features | ⏳ Planned |
| 6 | Launch & growth | ⏳ Planned |

Detailed task breakdowns live in [`docs/`](docs/) (`phase1-tasks.md` … `phase6-tasks.md`),
alongside the [PRD](docs/PRD.md), [architecture](docs/Architecture.md),
[system design](docs/system-design.md), and [UI design guide](docs/ui-design-guide.md).

---

## 📄 License & Usage

This is a personal portfolio project — **all rights reserved**. The source is published for
review and demonstration purposes. Please don't redistribute or ship it as your own; feel free
to read the code and reach out if you'd like to talk about any of it.

---

<p align="center"><i>Built with Kotlin & Jetpack Compose.</i></p>
