# Memly - Key Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| D001 | MVVM with Repository, no Clean Architecture / domain layer | Simplicity for MVP; avoids unnecessary abstraction |
| D002 | Single-module app organized by package | Faster builds, simpler structure |
| D003 | Room entities used directly (no separate domain models) | Reduces boilerplate for MVP |
| D004 | Kotlin 2.0.21 + AGP 9.0.1, KSP over KAPT | AGP 9 drops KAPT; KSP is faster |
| D005 | Hilt 2.59.2 | Minimum version with AGP 9 compatibility |
| D006 | Reference-first file management with SHA-256 hash dedup | Avoids duplicate storage; content-addressable |
| D007 | Package: `com.example.memly` | Standard dev convention |
| D008 | Light-first theme: Coral #FF6B6B + Beige #FFF3E6 + Teal #4BC0C8 | Warm nostalgic palette |
| D009 | Bottom nav: Timeline, Map, Search + FAB for Capture | Core actions always reachable |
| D010 | Enums stored as strings in Room | Readable, survives ordinal changes |
| D011 | String-based navigation routes (not type-safe) | Simpler setup for current scale |
| D012 | `android.disallowKotlinSourceSets=false` | KSP compat with AGP 9 |
| D013 | Typography: Poppins + Inter, bundled TTF | Friendly modern sans-serif, free |
| D014 | Image-dominant memory cards with gradient overlay | Photo-first visual journal feel |
| D015 | 3:4 portrait aspect ratio for memory cards | Matches phone photo aspect |
| D016 | Three card variants: timeline, carousel, search row | Different density per context |
| D017 | Full-bleed photo hero in detail screen | Immersive memory viewing |
| D018 | No dynamic color (Material You) | Memly has its own brand identity |
| D019 | Conventional Commits: type(scope): description | Consistent git history |
