# Memly - Key Code Patterns

## Build Configuration
- AGP 9.0.1 has built-in Kotlin 2.0.21 — do NOT apply `kotlin-android` plugin
- Compose compiler via `org.jetbrains.kotlin.plugin.compose:2.0.21`
- KSP 2.0.21-1.0.28, Hilt 2.59.2, Room 2.7.1
- `android.disallowKotlinSourceSets=false` in gradle.properties (KSP compat with AGP 9)
- No dynamic color (Material You) — custom Memly palette always used

## Navigation
- Screen sealed class with data object variants, string-based routes
- Bottom nav hidden on full-screen routes (Capture, Detail)
- FAB for memory capture, hidden on non-main screens

## Data Layer
- Entities: `@Entity(tableName = "snake_case")`, PK with `autoGenerate = true`, default `id: Long = 0`
- Foreign keys always have `@Index` on FK columns
- DAOs: suspend for writes, `Flow<>` for reactive reads
- Enums stored as strings (Mood, MediaType)
- Relation queries: MemoryWithDetails using `@Embedded` + `@Relation` + `@Junction`

## Repository Layer
- `@Singleton class XRepository @Inject constructor(dao: XDao)`
- Expose Flow for reads, suspend for writes
- Coordinate across multiple DAOs (e.g., MemoryRepository uses MemoryDao + TagDao)
- File operations on `Dispatchers.IO`

## Presentation Layer
- ViewModel: `@HiltViewModel`, exposes `StateFlow<UiState>`
- Screen: `@Composable fun FeatureScreen(...)`, VM via `hiltViewModel()`
- Collect state via `collectAsStateWithLifecycle()`
- Error handling: try/catch in ViewModel, error in UiState, Snackbar in UI

## DI
- `object` modules with `@Provides`, `@InstallIn(SingletonComponent::class)`
- DatabaseModule provides MemlyDatabase + all DAOs
- Repositories are `@Singleton` with `@Inject constructor`

## Theme
- Light-first: Soft Coral primary, Warm Beige surface, Soft Teal accent
- Dark theme: muted equivalents of all colors
- Typography: Poppins (headings), Inter (body) — bundled as static TTF in res/font/
- Shapes: 8/12/16/20dp (small/medium/large/XL)
- Mood.color() extension maps each mood to a distinct color

## File Management
- Content-addressed storage: files named by SHA-256 hash
- Thumbnails: 300px max dimension, JPEG 80%, in filesDir/thumbnails/
- Dedup: check hash in DB before copying
- Reference-first: store URI, only copy for backup/cloud
