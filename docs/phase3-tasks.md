# Memly -- Phase 3: Insights & Engagement Task Breakdown

Help users discover patterns in their memories through analytics, calendar views, and gamification.

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
Section 1 (Mood Analytics)   ──┐
Section 2 (Calendar View)     ─┤
Section 3 (Stats Dashboard)   ─┼──> Section 7 (Integration & Polish)
Section 4 (Gamification)      ─┤
Section 5 (Map Heatmap)       ─┤
Section 6 (On This Day)       ─┘
```

Sections 1 through 6 are mostly independent of each other and can proceed in parallel.
Section 7 is the final integration pass and depends on all prior sections.

---

## Summary Table

| Section | Name                           | Tasks | Complexity | Risk   |
|---------|--------------------------------|-------|------------|--------|
| 1       | Mood Analytics & Trends        | 9     | High       | Medium |
| 2       | Calendar View                  | 8     | Medium     | Medium |
| 3       | Memory Statistics Dashboard    | 7     | Low        | Low    |
| 4       | Gamification -- Streaks & Scores | 8   | Medium     | Medium |
| 5       | Map Heatmap                    | 6     | Medium     | Medium |
| 6       | "On This Day" Enhancement      | 5     | Medium     | Medium |
| 7       | Integration & Polish           | 8     | Medium     | Low    |
|         | **Total**                      | **51**|            |        |

---

## Section 1: Mood Analytics & Trends

**Status:** NOT STARTED

Visualize mood patterns over time using chart libraries within Compose. Provides mood distribution, mood timeline, and memory frequency charts.

**Risks:**
- Chart library integration with Jetpack Compose may require interop wrappers or a Compose-native library (Vico preferred, MPAndroidChart as fallback via AndroidView).
- Mapping the Mood enum to numeric values for line charts requires a consistent and meaningful ordinal scale.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 1.1  | Add Vico chart library dependency (or MPAndroidChart as alternative)        | ⬜     | Vico is Compose-native; evaluate API stability before committing |
| 1.2  | Create AnalyticsRepository with mood aggregation queries                   | ⬜     | Methods for mood count by week/month, mood values over time    |
| 1.3  | Add Room DAO queries for mood distribution and mood timeline data          | ⬜     | GROUP BY mood with COUNT; ordered by date for timeline         |
| 1.4  | Create AnalyticsScreen (new bottom nav tab or sub-screen of Settings/Profile) | ⬜  | Scrollable column hosting chart composables                    |
| 1.5  | Create AnalyticsViewModel with mood stats state                            | ⬜     | StateFlow exposing distribution data, timeline data, frequency data |
| 1.6  | Build mood distribution pie/donut chart composable                         | ⬜     | Shows moods by frequency; color-coded segments with legend     |
| 1.7  | Build mood timeline line chart composable                                  | ⬜     | Map Mood enum to numeric value (e.g., 1-5); plot over weeks/months |
| 1.8  | Build memory frequency bar chart composable                                | ⬜     | Memories created per week or month; x-axis is time period      |
| 1.9  | Verify: charts display correct data, update when new memories are added    | ⬜     | Add memories and confirm chart values refresh accurately       |

**Checkpoint:** AnalyticsScreen renders three charts (pie, line, bar) with live data from Room. Adding a new memory updates the charts on next visit.

---

## Section 2: Calendar View

**Status:** NOT STARTED

Monthly calendar showing which days have memories, with drill-down to view a specific day's entries.

**Risks:**
- Custom calendar composable can be complex to build correctly (handling month lengths, day-of-week alignment, leap years). Consider LazyVerticalGrid with 7 columns.
- Performance when querying memory dates for each visible month.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 2.1  | Create CalendarScreen (accessible from timeline or as a navigation tab)    | ⬜     | Scaffold with month header and grid body                       |
| 2.2  | Create CalendarViewModel -- load memory dates for the displayed month      | ⬜     | Query distinct dates with memory count; expose as StateFlow    |
| 2.3  | Build month grid composable (7 columns, day cells)                         | ⬜     | LazyVerticalGrid or custom Grid; handle first-day offset       |
| 2.4  | Highlight days that have memories (dot indicator or background color)      | ⬜     | Compare day against set of dates returned from ViewModel       |
| 2.5  | Tap a day to show list of memories from that day (bottom sheet or inline)  | ⬜     | ModalBottomSheet with LazyColumn of MemoryCard items           |
| 2.6  | Navigate between months (previous/next arrows)                             | ⬜     | Update ViewModel month; re-query memory dates for new range    |
| 2.7  | Show memory count per day on tap or long-press                             | ⬜     | Small badge or tooltip displaying count                        |
| 2.8  | Verify: calendar highlights correct days, tapping shows that day's memories | ⬜    | Test with memories spread across multiple months               |

**Checkpoint:** Calendar grid renders correctly for any month. Days with memories are visually distinct. Tapping a highlighted day reveals its memories. Month navigation works.

---

## Section 3: Memory Statistics Dashboard

**Status:** NOT STARTED

High-level stats about the user's memory collection, presented as a set of summary cards.

**Risks:**
- Low overall risk. Simple aggregation queries against Room.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 3.1  | Create StatsSection composable (reusable stat card layout)                 | ⬜     | Card with icon, label, and value; used for each stat           |
| 3.2  | Total memories count                                                       | ⬜     | SELECT COUNT(*) from MemoryEntity                              |
| 3.3  | Total media files with breakdown (photos, videos, voice memos)             | ⬜     | GROUP BY mediaType on MediaFileEntity                          |
| 3.4  | Most common mood                                                           | ⬜     | GROUP BY mood ORDER BY COUNT DESC LIMIT 1                      |
| 3.5  | Most captured location (placeLabel frequency)                              | ⬜     | GROUP BY placeLabel ORDER BY COUNT DESC LIMIT 1; exclude nulls |
| 3.6  | Memories this week, this month, and this year                              | ⬜     | Filter by date range relative to current date                  |
| 3.7  | First memory date and most recent memory date                              | ⬜     | MIN(date) and MAX(date) from MemoryEntity                      |

**Checkpoint:** Stats dashboard displays all seven metrics with accurate values. Stats update after new memory creation.

---

## Section 4: Gamification -- Streaks & Scores

**Status:** NOT STARTED

Encourage regular memory capture through streak tracking and memory completeness scoring.

**Risks:**
- Date logic edge cases around timezone boundaries and midnight crossover may cause incorrect streak calculations.
- DataStore atomicity: concurrent reads/writes to streak fields must be handled safely (use DataStore transactions).

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 4.1  | Create streak tracking logic: consecutive days with at least one memory    | ⬜     | Pure Kotlin function; compare last_capture_date to today       |
| 4.2  | Add DataStore fields: current_streak, longest_streak, last_capture_date    | ⬜     | Preferences DataStore; read/write via suspend functions         |
| 4.3  | Update streak on every memory save (integrate into repository save flow)   | ⬜     | Call streak update after successful Room insert                 |
| 4.4  | Create streak display widget (current streak and longest streak)           | ⬜     | Composable card shown on Timeline header or Dashboard          |
| 4.5  | Memory score: calculate based on completeness (photo, notes, mood, location, tags) | ⬜ | Each field present adds points; max score when all fields filled |
| 4.6  | Display memory score as badge or indicator on MemoryCard                   | ⬜     | Small icon or progress ring on card corner                     |
| 4.7  | Streak milestone celebrations at 7, 30, and 100 days                       | ⬜     | Snackbar or simple animation triggered when milestone reached  |
| 4.8  | Verify: streak increments daily, resets on gap, milestones trigger correctly | ⬜   | Test with simulated dates; confirm DataStore persistence       |

**Checkpoint:** Streak counter persists across app restarts. Saving a memory on a new day increments the streak. Missing a day resets it. Memory score reflects field completeness. Milestone at 7 days triggers celebration.

---

## Section 5: Map Heatmap

**Status:** NOT STARTED

Enhance the existing map view with a density-based heatmap visualization showing where memories are concentrated.

**Risks:**
- Google Maps heatmap utility library (maps-utils) may require additional dependency and has its own API surface.
- Performance may degrade with a large number of data points; consider clustering or sampling.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 5.1  | Create heatmap data model (lat/lng with weight based on memory count)      | ⬜     | WeightedLatLng from maps-utils; aggregate by location          |
| 5.2  | Add heatmap overlay to MapScreen (Google Maps heatmap tile provider)       | ⬜     | HeatmapTileProvider from android-maps-utils library            |
| 5.3  | Toggle between pin view and heatmap view                                   | ⬜     | Switch or SegmentedButton on MapScreen toolbar                 |
| 5.4  | Heatmap intensity based on memory count at each location                   | ⬜     | Weight parameter on WeightedLatLng; normalize across dataset   |
| 5.5  | Legend showing intensity scale (low to high color gradient)                 | ⬜     | Overlay composable anchored to map corner                      |
| 5.6  | Verify: heatmap reflects actual memory density, toggle works correctly     | ⬜     | Add memories at varied locations; confirm visual density match  |

**Checkpoint:** Map view supports two modes: pins and heatmap. Heatmap accurately reflects where memories are geographically concentrated. Toggle switches between views without data loss.

---

## Section 6: "On This Day" Enhancement

**Status:** NOT STARTED

Expand the basic Time Hop feature from Phase 1 into a richer "On This Day" experience with carousel display and daily notifications.

**Risks:**
- WorkManager scheduling for daily notifications requires careful configuration (exact timing, battery optimization, device restart persistence).
- Notification permission must be requested on Android 13+ (POST_NOTIFICATIONS); handle denial gracefully.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 6.1  | Create OnThisDayViewModel -- query memories matching today's date across all years | ⬜ | Filter by dayOfMonth and month, exclude current year           |
| 6.2  | Build OnThisDay card carousel using HorizontalPager                        | ⬜     | Each page shows a past memory with photo, title, and date      |
| 6.3  | Add daily "On This Day" notification via WorkManager                       | ⬜     | PeriodicWorkRequest (24h); check if past memories exist before notifying |
| 6.4  | Show "X years ago" label on each carousel card                             | ⬜     | Calculate year difference between memory date and today         |
| 6.5  | Verify: carousel shows correct memories, notification fires once daily     | ⬜     | Test with memories from prior years on same calendar date      |

**Checkpoint:** On This Day carousel displays memories from the same calendar date in prior years. Daily notification appears when relevant memories exist. "X years ago" labels are accurate.

---

## Section 7: Integration & Polish

**Status:** NOT STARTED

Final pass to ensure all Phase 3 features work together, perform well, and are accessible from the app navigation.

**Risks:**
- Low. Primarily testing and minor UI adjustments.

| Task | Description                                                                 | Status | Notes                                                         |
|------|-----------------------------------------------------------------------------|--------|---------------------------------------------------------------|
| 7.1  | End-to-end test of all analytics, calendar, and gamification features      | ⬜     | Walk through each feature with realistic data                  |
| 7.2  | Verify streak logic across timezone changes and app restarts               | ⬜     | Change device timezone; force-stop and relaunch app            |
| 7.3  | Performance test with large dataset (100+ memories) in charts and calendar | ⬜     | Seed database with bulk data; measure frame rate and load time |
| 7.4  | Update navigation to include analytics entry point                         | ⬜     | Add bottom nav item or menu entry; ensure back navigation works |
| 7.5  | Final build and smoke test on physical device                              | ⬜     | Verify all features on a real device with release build        |
| 7.6  | Build custom CameraX in-app camera with photo/video toggle                 | ⬜     | Messenger-like UX: single camera screen with mode switch; replaces Photo/Video dialog (D082) |
| 7.7  | Integrate CameraX camera into CaptureScreen and MemoryDetailScreen         | ⬜     | Camera button navigates to CameraX screen; result returned via savedStateHandle or nav args |
| 7.8  | Verify: CameraX camera captures photos and videos, returns results correctly | ⬜   | Test photo capture, video recording, mode switching, back navigation |

**Checkpoint:** All Phase 3 features are integrated, performant, and reachable from navigation. In-app camera supports photo/video toggle without system camera dialogs. App is stable with a large memory collection. Ready for Phase 4.
