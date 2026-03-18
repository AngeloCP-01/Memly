package com.example.memly.util

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Wraps MediaRecorder lifecycle for voice memo recording.
 * Records to a temp file in cacheDir; the caller saves to MediaStore via MediaStoreManager.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var _isRecording = false
    val isRecording: Boolean get() = _isRecording

    companion object {
        private const val TAG = "AudioRecorder"
    }

    /**
     * Start recording audio to a temp file.
     * Returns the content URI of the temp file via FileProvider.
     */
    fun start(): Uri? {
        if (_isRecording) return null

        return try {
            val cacheDir = File(context.cacheDir, "audio")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mr
            _isRecording = true

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            null
        }
    }

    /**
     * Stop recording and return the URI of the recorded file, or null on failure.
     */
    fun stop(): Uri? {
        if (!_isRecording) return null

        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            _isRecording = false

            outputFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            cleanup()
            null
        }
    }

    /**
     * Cancel an in-progress recording, discarding the file.
     */
    fun cancel() {
        cleanup()
    }

    private fun cleanup() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) { }
        recorder = null
        _isRecording = false
        outputFile?.delete()
        outputFile = null
    }
}
