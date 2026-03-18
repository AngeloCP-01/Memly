package com.example.memly.data.local.entity

/**
 * Tracks ownership/origin of a media file.
 *
 * APP_OWNED  – Created in-app (camera photo/video, audio recording).
 *              Stored in public storage (Pictures/Memly/, Movies/Memly/, Music/Memly/).
 *              App owns and manages the file. Survives uninstall.
 *
 * EXTERNAL   – Reference to a file in the user's gallery.
 *              Zero storage cost; URI only. Can break if user deletes original.
 *
 * IMPORTED   – User chose "Save to Memly" for a picked file.
 *              Copied to Pictures/Memly/. App owns the copy. Survives uninstall.
 */
enum class MediaSource {
    APP_OWNED,
    EXTERNAL,
    IMPORTED
}
