package com.example.memly.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ThumbnailUtil {

    private const val THUMBNAIL_MAX_SIZE = 300

    fun generateThumbnail(context: Context, sourceUri: Uri, outputDir: File, fileName: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val scale = THUMBNAIL_MAX_SIZE.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)
            val width = (originalBitmap.width * scale).toInt()
            val height = (originalBitmap.height * scale).toInt()
            val thumbnail = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, "${fileName}_thumb.jpg")
            FileOutputStream(outputFile).use { out ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            originalBitmap.recycle()
            thumbnail.recycle()
            outputFile
        } catch (e: Exception) {
            null
        }
    }
}
