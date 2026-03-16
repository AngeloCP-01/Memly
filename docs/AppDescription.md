# Memly — App Description & Brainstorm Reference

> **"Your life, beautifully remembered. A Gallery of your Memories."**

A personal memory journal that blends photos, locations, and notes with emotion, context, and discovery. Think of it as a "time machine for your life."

---

## Core Concept

Memly is more than a gallery — it's a personal archive of life, emotions, and stories. Unlike ordinary gallery apps or travel-focused replay apps, Memly focuses on emotions, narrative, and context, turning everyday moments into an immersive journey through your life.

---

## Core Features

### Memory Capture
- Photos/videos with optional text notes
- Automatic location tagging (GPS-based) with custom place labels ("Home," "Paris Trip")
- Mood/emotion tagging (happy, nostalgic, adventurous, etc.)
- Optional voice memos attached to a memory

### Memory Timeline
- Scrollable timeline view showing memories chronologically
- Memories clustered by day/week/month/year
- "Time Hop" feature: shows memories from this day in previous years

### Map View
- Memories plotted on an interactive map
- Filterable by type, emotion, or time
- Heatmap overlay showing "most visited" or "most memorable" places

### Memory Discovery & Highlights
- AI-assisted highlights: app selects "best memories" based on engagement (photos, location uniqueness, emotions tagged)
- Monthly or yearly memory reels (short videos or slideshows generated automatically)

### Search & Organization
- Search by location, tags, people (using AI image recognition), or emotions
- Collections or albums for events ("Trip to Kyoto," "Graduation 2025")

### Privacy & Sharing
- Fully private by default, with optional sharing (link-based or social media integration)
- Local-first storage with optional cloud backup
- End-to-end encryption for sensitive memories

### Gamification / Engagement
- Streaks: "Add a memory every day/week"
- Mood tracker trends over time
- "Memory score" for top locations, photos, or experiences

---

## Advanced Features (Post-MVP)

- **AI-Powered Memory Tagging**: Automatically detects faces, locations, and events in photos
- **AR Reminders**: Walk into a place and the app reminds you of past memories there
- **Memory Collages**: AI-generated collages from trips, events, or certain moods
- **Offline Mode**: Works completely offline; syncs when internet is available
- **Legacy Mode**: Allow memories to be shared with family/friends after a certain date
- **Memory Story Mode**: AI generates short narrative videos from your memories

---

## Technical Architecture

### MVP App Architecture

**Offline-first** with optional cloud storage. No full server backend initially.

#### Option A: Pure Mobile App (No Backend)
- Storage: Local device storage (Room DB for structured data; files for media)
- Pros: Zero server cost, fastest iteration, private by default
- Cons: No cross-device sync; if device is lost, memories are lost

#### Option B: Mobile App + Free Cloud Storage
- Storage: Free cloud services for images/videos
- Metadata: Stored locally or in free-tier DB
- Pros: Cross-device sync optional; free tier reduces cost
- Cons: Limits on storage, some setup required

### Free / Low-Cost Cloud Storage Options

| Service | Free Tier | Notes |
|---------|-----------|-------|
| Firebase Storage + Firestore | 1 GB storage, 50K reads/day | Easy Android integration; stores media & metadata; scales later |
| Supabase | 500 MB storage, 2 GB bandwidth | Open-source Firebase alternative; Postgres DB + storage |
| Cloudinary | 25K images/month, 2 GB storage | Great for images; URL transformations (resizing, thumbnails) |
| Google Drive API / OneDrive | User's own account | Free but integration is clunky |
| AWS S3 Free Tier | 5 GB storage for 12 months | Requires AWS knowledge |

**Verdict**: Firebase Storage + Firestore is the easiest zero-backend option that scales to premium.

### Data Storage Strategy

**On Device (Local-first):**
- Media files: Saved in app-specific storage folder
- Metadata: Room DB (memory ID, timestamp, mood, location, tags)

**Optional Cloud Sync:**
- Upload files to Firebase Storage (images/videos)
- Store metadata in Firestore (links to storage URLs + tags/emotions/locations)
- Only sync when online to save bandwidth and cost

---

## File Management Strategy

### The Problem
User has a photo in device gallery → adds it to Memly → naive copy duplicates storage.

### Strategies

#### A. Reference-Only (No Duplicate)
- Store URI/path of the original file in app's database
- Pros: No storage duplication
- Cons: If user deletes original, app loses access; scoped storage permissions can complicate things

#### B. Copy + Deduplicate
- Copy media into app sandbox but check for duplicates first
- Deduplicate via file hash (MD5/SHA1) — only copy if new
- Pros: Files are safe, app has full control
- Cons: Extra space for first copy

#### C. Hybrid Approach (Recommended for MVP)
- Default: Reference the original file (low storage overhead)
- On backup/cloud sync: Copy to app sandbox first
- Always hash and check before saving to prevent duplicates
- If user deletes original, prompt: "File missing. Restore from cloud?" or keep backup copy

### File Structure
```
/MemlyApp/
  /media/          → sandboxed copies (if user chooses backup)
    123abc.jpg
    456def.mp4
  /thumbnails/     → generated thumbnails for timeline/map
    123abc_thumb.jpg
  /metadata.db     → Room DB
    memory_id, file_path, mood, location, timestamp, hash
```

### File Management Flow
```
[User Gallery / Camera]
        │
        ▼
[App Add Memory Action]
        │
        ├─ Step 1: Check File Type & Permissions
        │      • Only allow supported formats (jpg, png, mp4, etc.)
        │      • Request access via MediaStore / File Picker
        │
        ├─ Step 2: Compute File Hash
        │      • Generate MD5/SHA1 hash of the file
        │      • Check DB for existing hash
        │          └─ If exists → skip copy, reference existing entry
        │          └─ If new → continue
        │
        ├─ Step 3: Decide Storage Strategy
        │      ├─ Option A: Reference Only (default for MVP)
        │      │       • Store URI/path in metadata DB
        │      │       • Low storage impact
        │      │       • Warning if original file deleted
        │      │
        │      └─ Option B: Copy to Sandbox (for backup/cloud sync)
        │              • Copy file into /MemlyApp/media/
        │              • Store internal path in DB
        │              • Generate thumbnail in /thumbnails/
        │
        ├─ Step 4: Metadata Storage (Room DB)
        │      • memory_id, file_path, hash, timestamp
        │      • location, mood/emotion, notes
        │
        ├─ Step 5: Generate Thumbnails
        │      • Small version for timeline/map
        │      • Linked to memory_id
        │
        └─ Step 6: Optional Cloud Sync
               • Copy sandbox file → Firebase Storage
               • Store cloud URL in DB
               • Keep metadata in DB for offline-first access
```

### Key Considerations
- **Avoid Duplication**: Hash check ensures re-added photos aren't duplicated
- **Hybrid Storage**: Reference-first keeps storage minimal; sandbox copy only for backup/cloud
- **Offline-First**: Accessible without internet; cloud sync is optional (zero MVP cost)
- **Thumbnail Management**: Prevents loading full-size media in timeline/map, improving performance
- **Scalable**: Sandbox copies make AI/collage processing easier in later phases

---

## Monetization Strategy

### Free Tier
- Core memory capture (photo/video/text)
- Timeline + map browsing
- Basic AI highlights

### Premium Tier
- Unlimited storage & cloud sync
- Advanced AI reels and collages
- Mood analytics & heatmaps
- AR memory reminders
- Legacy mode: pass memories to family/friends after a set date

---

## UX / Design Guidelines

- **Palette**: Soft pastels, muted tones, or subtle gradients for nostalgia
- **Micro-interactions**: Card flips, map pins pop with memory previews, gentle timeline scrolls
- **Emotion-first interface**: Prioritize mood and story over raw file display
- **Onboarding flow**: "Capture your first memory" with guided steps

---

## Store Listing

### Short Description (80 chars max)
"Capture, relive, and explore your memories with emotion and context."

### Full Description
Memly is more than a gallery — it's your personal archive of life, emotions, and stories. Capture photos, videos, notes, and moods, then explore them in ways that make your memories come alive.

**Why Memly is unique:**
- Emotional Journaling: Tag moods, feelings, and stories with every memory
- Interactive Timeline & Map: Browse memories chronologically, geographically, or by emotion
- AI-Powered Highlights: Automatic reels, collages, and memory summaries
- Search & Discovery: Find memories by date, people, emotion, or place
- Private & Secure: End-to-end encryption, offline-first design, optional cloud backup
- Engaging Insights: Mood trends, memory streaks, and "life heatmaps"

Rediscover your life — not just your photos. Memly turns your everyday moments into an emotional story you can relive anytime.

### Feature Bullets
- Capture photos, videos, notes, and voice memos — tag moods and emotions
- Interactive timeline + map to explore memories by time, place, or feeling
- AI-assisted memory highlights: reels, collages, and monthly/yearly summaries
- Search memories by people, location, date, or emotion
- Fully private: offline-first with optional encrypted cloud backup
- Mood trends and memory streaks for deeper self-reflection

### App Icon
- File type: PNG, 512x512 px (Google Play requirement)
- Design idea: A winding path or pin with a soft heart overlay; evokes journey + emotion

---

## MVP Prioritization Summary

| Priority | Feature | Notes |
|----------|---------|-------|
| Essential | Capture photo/video + note + location + mood → store locally | Core loop |
| Essential | Timeline view with chronological browsing | Primary navigation |
| Essential | Basic search and tagging | Organization |
| Optional | Cloud sync via Firebase free tier | Backup, cross-device |
| Deferred | AI highlights, reels, collages | Post-MVP |
| Deferred | AR reminders, Legacy mode, Gamification | Post-MVP |
