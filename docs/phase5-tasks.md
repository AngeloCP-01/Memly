# Memly -- Phase 5 AI & Premium Features Task Breakdown

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
Section 1 (AI Highlights) ──┐
Section 2 (Memory Reels)    ├──> Section 6 (Integration)
Section 3 (Collages)       ─┤
Section 4 (AI Tagging)     ─┤
Section 5 (Smart Search)   ─┘
```

Sections 1 through 5 are independent and can proceed in parallel.
Section 6 depends on all prior sections and is the final integration pass.

---

## Summary Table

| Section | Name                 | Tasks | Complexity | Risk   |
|---------|----------------------|-------|------------|--------|
| 1       | AI Highlights        | 8     | Medium     | Medium |
| 2       | Memory Reels         | 9     | High       | High   |
| 3       | Memory Collages      | 7     | Medium     | Medium |
| 4       | AI Tagging           | 7     | High       | Medium |
| 5       | Smart Search         | 5     | Medium     | Medium |
| 6       | Integration & Polish | 4     | Medium     | Low    |
|         | **Total**            | **40**|            |        |

---

## Section 1: AI Highlights

**Status:** NOT STARTED

Auto-select "best memories" based on signals such as media count, location presence, mood, tag count, and notes length.

**Risks:**
- Defining a meaningful scoring algorithm without ML.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 1.1  | Define highlight scoring algorithm (media count, has location, has mood, tag count, notes length) | ⬜     | Weight each signal; document formula for transparency         |
| 1.2  | Create HighlightsRepository with scoring queries                            | ⬜     | Encapsulate all highlight-related data access                 |
| 1.3  | Room query: get top N memories by computed score                            | ⬜     | Use raw SQL or @RawQuery for composite scoring                |
| 1.4  | Create HighlightsScreen showing curated "best of" memories                  | ⬜     | Distinct from timeline; emphasize visual presentation         |
| 1.5  | Weekly/monthly highlights grouping                                          | ⬜     | Group top-scored memories by time period                      |
| 1.6  | HighlightsViewModel with time range selection                               | ⬜     | Expose Flow of highlights filtered by week/month/all-time     |
| 1.7  | Highlight badge on qualifying MemoryCards in timeline                       | ⬜     | Small visual indicator on cards that meet highlight threshold  |
| 1.8  | Verify: highlights screen shows top memories, scores make sense             | ⬜     | End-to-end test with varied memory data                       |

**Checkpoint:** Highlights screen displays top-scored memories. Scoring algorithm produces sensible rankings. Timeline cards show highlight badge where applicable.

---

## Section 2: Memory Reels

**Status:** NOT STARTED

Auto-generated slideshow/video from memories with transitions and optional background music.

**Risks:**
- HIGH. Video generation is complex on-device. MediaCodec API is low-level. Consider FFmpeg-kit as an alternative.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 2.1  | Create ReelGenerator utility (takes list of media files, produces slideshow) | ⬜     | Core engine; input is ordered list of URIs, output is video file |
| 2.2  | Use Android MediaCodec or a library for slideshow assembly with transitions | ⬜     | Evaluate FFmpeg-kit vs raw MediaCodec; document trade-offs    |
| 2.3  | Add background music support (bundled royalty-free tracks or silent)        | ⬜     | Mix audio track with slideshow video; allow mute option       |
| 2.4  | Create ReelPlayerScreen with full-screen playback                          | ⬜     | Reuse Media3 ExoPlayer if available; landscape support        |
| 2.5  | Create ReelConfigScreen (select time range, mood filter, collection)       | ⬜     | User chooses which memories to include in reel                |
| 2.6  | Generate reel on background thread with progress indicator                 | ⬜     | WorkManager or coroutine with progress callback               |
| 2.7  | Save reel to device gallery (MediaStore insert)                            | ⬜     | Use MediaStore API for scoped storage compliance              |
| 2.8  | Share reel via Android share sheet                                         | ⬜     | ACTION_SEND with video MIME type; FileProvider URI            |
| 2.9  | Verify: generate reel from memories, play, save, share                     | ⬜     | End-to-end test of full reel pipeline                         |

**Checkpoint:** Can generate a video reel from selected memories with transitions. Reel plays in full-screen player. Can save to device gallery and share to other apps.

---

## Section 3: Memory Collages

**Status:** NOT STARTED

AI-generated photo collages from trips, events, or moods with configurable grid layouts.

**Risks:**
- Medium. Layout math for grid templates. Bitmap memory management for multiple large images.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 3.1  | Create CollageGenerator (grid layouts: 2x2, 3x3, 1+2, etc.)               | ⬜     | Canvas-based rendering; support multiple layout templates     |
| 3.2  | Select photos from a collection, date range, or mood filter                | ⬜     | Reuse existing filter infrastructure for photo selection      |
| 3.3  | Auto-layout photos into chosen grid template                               | ⬜     | Scale and crop photos to fit grid cells; handle aspect ratios |
| 3.4  | Add text overlay (date range, collection name, mood)                       | ⬜     | Draw text on Canvas; configurable font size and position      |
| 3.5  | Preview collage before saving                                              | ⬜     | Full-screen preview with option to regenerate or edit         |
| 3.6  | Save collage as image to device                                            | ⬜     | MediaStore insert; PNG or JPEG with quality setting           |
| 3.7  | Verify: generate collage from selected memories, save and share            | ⬜     | End-to-end test with varied photo counts and layouts          |

**Checkpoint:** Can generate a photo collage from filtered memories with text overlay. Collage previews correctly and saves to device storage.

---

## Section 4: AI Tagging

**Status:** NOT STARTED

On-device image recognition for automatic tag suggestions using ML Kit or TFLite.

**Risks:**
- ML Kit model size impact on APK. On-device inference performance on lower-end devices. Label quality and relevance.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 4.1  | Add ML Kit dependency (image labeling, or TFLite)                          | ⬜     | Evaluate bundled vs downloadable model; impact on APK size    |
| 4.2  | Create ImageAnalyzer utility that runs ML model on photos                  | ⬜     | Accept Bitmap or URI; return list of labels with confidence   |
| 4.3  | Extract labels (objects, scenes, activities) from photos                   | ⬜     | Filter by confidence threshold; map to user-friendly names    |
| 4.4  | Auto-suggest tags based on ML labels during capture                        | ⬜     | Show chip suggestions below tag input; non-intrusive UX       |
| 4.5  | User can accept/reject suggested tags                                      | ⬜     | Tap to accept; dismiss to reject; remember rejections         |
| 4.6  | Batch analyze existing photos (background job)                             | ⬜     | WorkManager job; process unanalyzed photos; store results     |
| 4.7  | Verify: photo analysis suggests relevant tags                              | ⬜     | Test with varied photo content; check label relevance         |

**Checkpoint:** ML model runs on-device and produces label suggestions. Tags are suggested during capture and can be accepted or rejected. Batch analysis processes existing library.

---

## Section 5: Smart Search

**Status:** NOT STARTED

Natural language search using on-device processing to interpret user queries and map them to database filters.

**Risks:**
- NLP without a server is limited. May need simple keyword extraction rather than true NLP.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 5.1  | Parse search queries for intent (e.g., "happy moments in Paris" to mood=HAPPY, place=Paris) | ⬜     | Keyword extraction; map known moods, places, tags             |
| 5.2  | Map parsed terms to Room query filters                                     | ⬜     | Build dynamic query from extracted intent components          |
| 5.3  | Combine with existing search infrastructure                                | ⬜     | Augment current search; fall back to text search if no intent |
| 5.4  | Show search suggestions based on existing moods, places, tags              | ⬜     | Autocomplete dropdown with known values from database         |
| 5.5  | Verify: natural language queries return relevant results                   | ⬜     | Test with various query patterns; validate result accuracy    |

**Checkpoint:** Search bar accepts natural language input. Queries are parsed into filter components and return relevant memories. Suggestions appear based on existing data.

---

## Section 6: Integration & Polish

**Status:** NOT STARTED

Final integration pass across all AI and premium features with performance validation and feature gating.

**Risks:**
- None significant; depends on completion of Sections 1-5.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 6.1  | End-to-end test of all AI features                                         | ⬜     | Verify highlights, reels, collages, tagging, search together  |
| 6.2  | Performance profiling (ML inference time, reel generation time)             | ⬜     | Benchmark on target devices; identify bottlenecks             |
| 6.3  | Gate premium features behind feature flag (for future monetization)         | ⬜     | Boolean flags; easy to toggle per build variant or remote config |
| 6.4  | Final build and smoke test                                                 | ⬜     | Clean build; verify no regressions from AI feature additions  |

**Checkpoint:** All AI features work together without conflicts. Performance meets acceptable thresholds. Premium features are gated behind feature flags for future monetization.
