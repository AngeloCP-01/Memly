package com.example.memly.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.memly.data.local.entity.MediaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
data class MediaMetadata(
    val uri: Uri,
    val displayName: String,
    val relativePath: String,
    val mimeType: String,
    val size: Long,
    val dateTaken: Long?,
    val width: Int?,
    val height: Int?
)

class MediaStoreManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "MediaStoreManager"
        private const val MEMLY_PICTURES_DIR = "Pictures/Memly"
        private const val MEMLY_MOVIES_DIR = "Movies/Memly"
        private const val MEMLY_MUSIC_DIR = "Music/Memly"
    }

    /**
     * Insert media content into public storage via MediaStore.
     * Copies from [sourceUri] to the appropriate public Memly folder.
     * Returns metadata about the saved file, or null on failure.
     */
    fun insertMedia(
        sourceUri: Uri,
        mediaType: MediaType,
        mimeType: String
    ): MediaMetadata? {
        return try {
            val displayName = generateDisplayName(mediaType, mimeType)
            val relativePath = getRelativePath(mediaType)
            val collection = getCollectionUri(mediaType)

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val insertUri = context.contentResolver.insert(collection, values) ?: return null

            // Copy content from source to new MediaStore entry
            context.contentResolver.openOutputStream(insertUri)?.use { output ->
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(insertUri, values, null, null)
            }

            // Query metadata
            val size = queryFileSize(insertUri)
            val dimensions = queryDimensions(insertUri, mediaType, mimeType)

            MediaMetadata(
                uri = insertUri,
                displayName = displayName,
                relativePath = relativePath,
                mimeType = mimeType,
                size = size,
                dateTaken = System.currentTimeMillis(),
                width = dimensions?.first,
                height = dimensions?.second
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert media", e)
            null
        }
    }

    /**
     * Resolve a picker/temporary URI to a stable MediaStore content URI.
     * Falls back to the original URI if resolution fails (e.g. cloud-backed providers).
     * The caller should take persistable URI permission on the fallback URI.
     */
    fun resolveToMediaStoreUri(pickerUri: Uri): Pair<Uri, Boolean> {
        // Try to query the MediaStore for this URI
        try {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            context.contentResolver.query(pickerUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    // Determine the correct external content URI base
                    val mimeType = context.contentResolver.getType(pickerUri) ?: ""
                    val baseUri = when {
                        mimeType.startsWith("video") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        mimeType.startsWith("audio") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val resolved = ContentUris.withAppendedId(baseUri, id)
                    // Verify the resolved URI is accessible
                    context.contentResolver.query(resolved, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use {
                        if (it.moveToFirst()) {
                            return resolved to true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Could not resolve to MediaStore URI, will use persistable permission", e)
        }
        return pickerUri to false
    }

    /**
     * Take persistable URI permission for an external reference.
     * Returns true if permission was granted.
     */
    fun takePersistablePermission(uri: Uri): Boolean {
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not take persistable permission for $uri", e)
            false
        }
    }

    /**
     * Delete an owned media file from public storage.
     * Returns true if deletion was successful.
     * On Android 11+, may need createDeleteRequest if direct delete fails.
     */
    fun deleteOwnedMedia(mediaUri: Uri): Boolean {
        return try {
            val deleted = context.contentResolver.delete(mediaUri, null, null)
            deleted > 0
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException deleting $mediaUri — may need user confirmation on Android 11+", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete media", e)
            false
        }
    }

    /**
     * Query metadata for a given content URI.
     */
    fun queryMetadata(uri: Uri, mediaType: MediaType): MediaMetadata? {
        return try {
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED
            )
            var displayName = ""
            var mimeType = context.contentResolver.getType(uri) ?: ""
            var size = 0L
            var dateTaken: Long? = null

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(0) ?: ""
                    mimeType = cursor.getString(1) ?: mimeType
                    size = cursor.getLong(2)
                    dateTaken = cursor.getLong(3) * 1000 // DATE_ADDED is in seconds
                }
            }

            if (size == 0L) {
                size = queryFileSize(uri)
            }

            val dimensions = queryDimensions(uri, mediaType, mimeType)

            MediaMetadata(
                uri = uri,
                displayName = displayName,
                relativePath = "",
                mimeType = mimeType,
                size = size,
                dateTaken = dateTaken,
                width = dimensions?.first,
                height = dimensions?.second
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query metadata for $uri", e)
            null
        }
    }

    /**
     * Check if a content URI is still accessible.
     */
    fun isUriAccessible(uri: Uri): Boolean {
        return try {
            context.contentResolver.query(
                uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null
            )?.use { it.moveToFirst() } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun generateDisplayName(mediaType: MediaType, mimeType: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val shortId = java.util.UUID.randomUUID().toString().take(4)
        val extension = mimeTypeToExtension(mimeType)
        return "memly_${timestamp}_$shortId.$extension"
    }

    private fun mimeTypeToExtension(mimeType: String): String = when {
        mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
        mimeType.contains("png") -> "png"
        mimeType.contains("webp") -> "webp"
        mimeType.contains("heic") || mimeType.contains("heif") -> "heif"
        mimeType.contains("mp4") -> "mp4"
        mimeType.contains("3gpp") || mimeType.contains("3gp") -> "3gp"
        mimeType.contains("webm") -> "webm"
        mimeType.contains("mpeg") && mimeType.startsWith("audio") -> "mp3"
        mimeType.contains("ogg") -> "ogg"
        mimeType.contains("m4a") || mimeType.contains("mp4a") -> "m4a"
        mimeType.contains("aac") -> "aac"
        else -> "dat"
    }

    private fun getRelativePath(mediaType: MediaType): String = when (mediaType) {
        MediaType.PHOTO -> MEMLY_PICTURES_DIR
        MediaType.VIDEO -> MEMLY_MOVIES_DIR
    }

    private fun getCollectionUri(mediaType: MediaType): Uri = when (mediaType) {
        MediaType.PHOTO -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    private fun queryFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(
                uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun queryDimensions(uri: Uri, mediaType: MediaType, mimeType: String): Pair<Int, Int>? {
        return try {
            when (mediaType) {
                MediaType.PHOTO -> {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, options)
                    }
                    if (options.outWidth > 0 && options.outHeight > 0) {
                        options.outWidth to options.outHeight
                    } else null
                }
                MediaType.VIDEO -> {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                        if (width != null && height != null) width to height else null
                    } finally {
                        retriever.release()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query dimensions", e)
            null
        }
    }
}
