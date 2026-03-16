# Changelog

All notable changes to Memly are documented here, organized by phase and section.

---

## Phase 1, Section 1: Project Setup & Configuration
**Date:** 2026-03-16

### Added
- Android project with Kotlin 2.0.21 + Jetpack Compose + AGP 9.0.1
- Room database with 6 entities: Memory, MediaFile, Tag, Collection, MemoryTag, MemoryCollection
- 3 DAOs: MemoryDao, TagDao, CollectionDao with full CRUD + reactive Flow queries
- 2 Repositories: MemoryRepository, CollectionRepository
- Hilt DI setup: DatabaseModule providing database + DAOs
- Navigation scaffold: bottom nav (Timeline, Map, Search) + FAB for Capture
- Placeholder screens: Timeline (with ViewModel), Map, Search, Capture, MemoryDetail
- Utility classes: FileHashUtil (SHA-256), ThumbnailUtil
- Mood and MediaType enums
- MemoryWithDetails relation class for loading memories with media files and tags

### Docs
- docs/PRD.md — Product Requirements Document
- docs/AppDescription.md — Organized brainstorm reference
- docs/Architecture.md — System architecture document
