package com.example.memly.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.memly.data.local.entity.MediaType
import java.io.File
import java.io.FileOutputStream

object ThumbnailUtil {

    private const val TAG = "ThumbnailUtil"
    private const val THUMBNAIL_MAX_SIZE = 300

    fun generateThumbnail(
        context: Context,
        sourceUri: Uri,
        mediaType: MediaType,
        outputDir: File,
        fileName: String
    ): File? {
        return try {
            val bitmap = when (mediaType) {
                MediaType.PHOTO -> decodeImageThumbnail(context, sourceUri)
                MediaType.VIDEO -> extractVideoFrame(context, sourceUri)
                MediaType.AUDIO -> null // Audio has no visual thumbnail
            } ?: return null

            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, "${fileName}_thumb.jpg")
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            bitmap.recycle()
            outputFile
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate thumbnail", e)
            null
        }
    }

    private fun decodeImageThumbnail(context: Context, uri: Uri): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (original == null) return null

        val scale = THUMBNAIL_MAX_SIZE.toFloat() / maxOf(original.width, original.height)
        val width = (original.width * scale).toInt()
        val height = (original.height * scale).toInt()
        val thumbnail = Bitmap.createScaledBitmap(original, width, height, true)
        original.recycle()
        return thumbnail
    }

    private fun extractVideoFrame(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (frame != null) {
                val scale = THUMBNAIL_MAX_SIZE.toFloat() / maxOf(frame.width, frame.height)
                val width = (frame.width * scale).toInt()
                val height = (frame.height * scale).toInt()
                val thumbnail = Bitmap.createScaledBitmap(frame, width, height, true)
                frame.recycle()
                thumbnail
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract video frame", e)
            null
        } finally {
            retriever.release()
        }
    }
}
