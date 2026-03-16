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
