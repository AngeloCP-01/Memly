# Memly — Product Requirements Document

**Version**: 1.0 (MVP)
**Platform**: Android (Kotlin, Jetpack Compose)
**Last Updated**: 2026-03-16

---

## 1. Overview

**App Name**: Memly
**Tagline**: "Your life, beautifully remembered."
**Vision**: A personal memory journal that blends photos, locations, and notes with emotion, context, and discovery — a "time machine for your life."

Memly is more than a gallery. It's a personal archive of life, emotions, and stories. Unlike ordinary gallery apps, Memly focuses on emotions, narrative, and context, turning everyday moments into an immersive journey through your life.

---

## 2. Problem Statement

Existing gallery and photo apps treat memories as flat files — sorted by date, maybe by location, but without any emotional or narrative context. Users cannot:
- Tag how a moment made them feel
- Discover forgotten memories through emotional or contextual search
- See their life story unfold on a map or timeline with meaning

Memly solves this by making emotion and context first-class citizens of every memory.

---

## 3. Target Users

- **Primary**: Individuals who want to preserve and relive personal memories with emotional depth — journaling-inclined, travel enthusiasts, life documenters
- **Secondary**: Anyone who takes photos/videos regularly and wants a more meaningful way to organize and rediscover them
- **Not targeting (MVP)**: Professional photographers, social media creators, enterprise users

---

## 4. MVP Scope

### In Scope (v1)
- Memory capture (photo, video, text notes, mood/emotion, location)
- Chronological timeline view with day/week/month clustering
- Basic map view with memory pins
- Search by tags, location, date, mood
- Collections/albums
- Local-first storage with Room DB
- File management with reference-first strategy and hash deduplication
- Thumbnail generation for performance

### Out of Scope (deferred to post-MVP)
- AI-powered highlights, reels, and collages
- AI face/object recognition
- AR location-based reminders
- Legacy mode (time-delayed sharing)
- Gamification (streaks, memory scores)
- Voice memos
- Cloud sync (Firebase integration)
- Social sharing
- End-to-end encryption

---

## 5. Core Features (MVP)

### 5.1 Memory Capture

**Description**: Users create memories by attaching media and metadata.

**Requirements**:
- [ ] Select photos/videos from device gallery or capture via camera
- [ ] Add text notes to any memory
- [ ] Tag mood/emotion from a predefined set (happy, nostalgic, adventurous, calm, excited, etc.)
- [ ] Automatic GPS location tagging (with permission)
- [ ] Custom place labels ("Home," "Grandma's House," "Paris Trip")
- [ ] Add custom tags for organization
- [ ] Support formats: JPG, PNG, MP4

### 5.2 Memory Timeline

**Description**: Chronological feed of all memories.

**Requirements**:
- [ ] Scrollable vertical timeline
- [ ] Cluster memories by day, week, month, year
- [ ] Show thumbnail, date, location, mood indicator per memory
- [ ] Tap to expand and view full memory details
- [ ] "Time Hop" — surface memories from this day in previous years

### 5.3 Map View

**Description**: Geographical visualization of memories.

**Requirements**:
- [ ] Interactive map with pins for each geotagged memory
- [ ] Tap pin to see memory preview
- [ ] Filter pins by date range or mood

### 5.4 Search & Organization

**Description**: Find and group memories.

**Requirements**:
- [ ] Search by text (notes content, tags, place labels)
- [ ] Filter by mood/emotion
- [ ] Filter by date range
- [ ] Filter by location
- [ ] Create and manage collections/albums
- [ ] Assign memories to collections

### 5.5 Privacy & Offline

**Description**: Local-first, private by default.

**Requirements**:
- [ ] All data stored on device (no network required)
- [ ] No account creation required for MVP
- [ ] App-specific storage (not accessible by other apps)
- [ ] Graceful handling of permission denials

---

## 6. Deferred Features (Post-MVP)

| Feature | Description | Phase |
|---------|-------------|-------|
| Voice Memos | Audio recordings attached to memories | 2 |
| Video Playback | ExoPlayer/Media3 for proper video viewing | 2 |
| Onboarding Flow | First-launch guided experience | 2 |
| Data Export/Backup | JSON backup, CSV export, restore | 2 |
| Mood Analytics | Charts showing mood patterns over time | 3 |
| Calendar View | Monthly calendar with memory indicators | 3 |
| Gamification | Streaks, mood trends, memory scores | 3 |
| Map Heatmap | Density visualization on map | 3 |
| Cloud Backup | Firebase Storage + Firestore sync | 4 |
| Authentication | Optional Google/email sign-in | 4 |
| Background Sync | WorkManager periodic sync | 4 |
| AI Highlights | Auto-select "best memories" | 5 |
| Memory Reels | Auto-generated slideshows | 5 |
| Memory Collages | AI-generated photo collages | 5 |
| AI Tagging | On-device image recognition | 5 |
| Smart Search | Natural language search | 5 |
| Encryption | E2E encryption, biometric lock | 6 |
| Social Sharing | Share memory cards, deep links | 6 |
| Store Launch | Play Store listing, signed release | 6 |

### Phase Roadmap

| Phase | Theme | Tasks | Focus |
|-------|-------|-------|-------|
| 1 | Foundation & MVP | ~73 | Core capture, timeline, map, search, collections |
| 2 | Enhanced Experience | ~49 | Voice memos, video, onboarding, theme, data export |
| 3 | Insights & Engagement | ~48 | Analytics, calendar, gamification, heatmap |
| 4 | Cloud Sync (Firebase) | ~50 | Auth, cloud backup, background sync |
| 5 | AI & Premium | ~40 | Highlights, reels, collages, AI tagging, smart search |
| 6 | Launch & Growth | ~43 | Security, sharing, accessibility, performance, store launch |

---

## 7. Technical Architecture

### Platform & Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room (SQLite wrapper)
- **Media Storage**: App-specific file storage (Android scoped storage)
- **Maps**: Google Maps SDK for Android
- **Min SDK**: TBD (recommend API 26 / Android 8.0)
- **Architecture Pattern**: MVVM with Repository pattern

### Dependencies (anticipated)
- Jetpack Compose (UI)
- Room (database)
- Hilt (dependency injection)
- Coil (image loading)
- Google Maps Compose (map view)
- CameraX (camera capture)
- Google Play Services Location (GPS)

### Future Cloud Integration (Post-MVP)
- Firebase Storage (media files, 1 GB free tier)
- Firestore (metadata sync, 50K reads/day free)
- Only sync when online to minimize bandwidth

---

## 8. Data Model

### Core Entities

#### Memory
| Field | Type | Description |
|-------|------|-------------|
| id | Long (PK) | Auto-generated |
| title | String? | Optional title |
| notes | String? | Text notes |
| mood | Enum | Emotion tag (HAPPY, NOSTALGIC, ADVENTUROUS, CALM, EXCITED, etc.) |
| latitude | Double? | GPS latitude |
| longitude | Double? | GPS longitude |
| placeLabel | String? | Custom location name |
| createdAt | Long | Timestamp of memory creation |
| memoryDate | Long | When the memory occurred |

#### MediaFile
| Field | Type | Description |
|-------|------|-------------|
| id | Long (PK) | Auto-generated |
| memoryId | Long (FK) | Links to Memory |
| filePath | String | URI or sandbox path |
| thumbnailPath | String? | Path to generated thumbnail |
| fileHash | String | MD5/SHA1 for deduplication |
| mediaType | Enum | PHOTO or VIDEO |
| isReference | Boolean | True if referencing original, false if copied to sandbox |

#### Tag
| Field | Type | Description |
|-------|------|-------------|
| id | Long (PK) | Auto-generated |
| name | String | Tag name |

#### MemoryTag (join table)
| Field | Type | Description |
|-------|------|-------------|
| memoryId | Long (FK) | Links to Memory |
| tagId | Long (FK) | Links to Tag |

#### Collection
| Field | Type | Description |
|-------|------|-------------|
| id | Long (PK) | Auto-generated |
| name | String | Collection name |
| description | String? | Optional description |
| coverMediaId | Long? (FK) | Cover image |
| createdAt | Long | Timestamp |

#### MemoryCollection (join table)
| Field | Type | Description |
|-------|------|-------------|
| memoryId | Long (FK) | Links to Memory |
| collectionId | Long (FK) | Links to Collection |

### Relationships
- Memory → MediaFile: One-to-many (a memory can have multiple photos/videos)
- Memory ↔ Tag: Many-to-many (via MemoryTag)
- Memory ↔ Collection: Many-to-many (via MemoryCollection)

---

## 9. File Management Flow

### Strategy: Hybrid (Reference-First + Hash Deduplication)

```
User selects photo/video
        │
        ▼
Check file type & permissions
(supported formats only, MediaStore / File Picker)
        │
        ▼
Compute file hash (MD5/SHA1)
        │
        ▼
Hash exists in DB? ──Yes──► Reference existing entry (no copy)
        │
        No
        ▼
Store as reference to original file
(URI in DB, no duplication)
        │
        ▼
Generate thumbnail → /MemlyApp/thumbnails/
        │
        ▼
Save metadata to Room DB
(memory_id, file_path, hash, timestamp, location, mood)
```

### Directory Structure
```
/MemlyApp/
  /media/          → sandboxed copies (used for backup/cloud sync)
  /thumbnails/     → generated thumbnails for timeline/map performance
```

### Key Principles
1. **Reference-first**: Default to storing URI of original file — zero duplication
2. **Hash deduplication**: MD5/SHA1 prevents re-adding the same file
3. **Thumbnail generation**: Small versions for timeline/map performance
4. **Graceful degradation**: If original file is deleted, show placeholder with "File missing" message
5. **Sandbox copy on backup**: Only copy to app storage when cloud sync is enabled (post-MVP)

---

## 10. Monetization

### Free Tier (MVP)
- Core memory capture (photo/video/text)
- Timeline + map browsing
- Basic search and organization
- Unlimited local memories

### Premium Tier (Post-MVP)
- Unlimited cloud storage & sync
- Advanced AI reels and collages
- Mood analytics & heatmaps
- AR memory reminders
- Legacy mode

---

## 11. UX / Design Guidelines

### Visual Style
- **Palette**: Soft pastels, muted tones, subtle gradients — evoke nostalgia
- **Typography**: Clean, readable, warm
- **Layout**: Card-based memory items, spacious whitespace

### Interactions
- Card flips for memory reveal
- Map pins pop with memory previews on tap
- Gentle, smooth timeline scrolling
- Emotion-first interface: prioritize mood and story over raw file display

### Onboarding
- Guided flow: "Capture your first memory"
- Request permissions (camera, location, storage) with clear explanations
- Show value immediately — display the first memory on the timeline

### Navigation
- Bottom navigation: Timeline | Map | Search | Profile/Settings
- FAB (Floating Action Button) for quick memory capture

---

## 12. Store Listing

### Short Description (80 chars max)
> Capture, relive, and explore your memories with emotion and context.

### Full Description
Memly is more than a gallery — it's your personal archive of life, emotions, and stories. Capture photos, videos, notes, and moods, then explore them in ways that make your memories come alive.

**Why Memly is unique:**
- Emotional Journaling: Tag moods, feelings, and stories with every memory
- Interactive Timeline & Map: Browse chronologically, geographically, or by emotion
- Search & Discovery: Find memories by date, people, emotion, or place
- Private & Secure: Offline-first design, your data stays on your device

Rediscover your life — not just your photos.

### Feature Bullets
- Capture photos, videos, and notes — tag moods and emotions
- Interactive timeline + map to explore memories by time, place, or feeling
- Search memories by location, date, tags, or emotion
- Fully private: offline-first, no account required
- Beautiful, emotion-first design

### App Icon
- PNG, 512x512 px
- Design concept: Winding path or pin with soft heart overlay — journey + emotion

---

## 13. Success Metrics (MVP Launch)

### Launch Criteria
- [ ] User can create a memory with photo, notes, mood, and location
- [ ] Timeline displays all memories chronologically with clustering
- [ ] Map view shows geotagged memories as pins
- [ ] Search returns results by text, mood, date, and location
- [ ] Collections can be created and memories assigned
- [ ] App works fully offline
- [ ] No duplicate files stored on device
- [ ] Thumbnails load smoothly in timeline/map views

### Post-Launch KPIs
- Daily active memories created per user
- Retention rate (7-day, 30-day)
- Average memories per user after 30 days
- App crash rate < 1%
- Timeline scroll performance (60 fps target)
- User satisfaction (Play Store rating target: 4.5+)
