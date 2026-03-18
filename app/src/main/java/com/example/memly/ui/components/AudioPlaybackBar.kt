package com.example.memly.ui.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Audio playback bar with play/pause, progress, and elapsed/total time.
 * Uses prepareAsync to avoid blocking the main thread.
 */
@Composable
fun AudioPlaybackBar(
    audioUri: Uri,
    durationMs: Long? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentMs by remember { mutableStateOf(0L) }
    var totalMs by remember { mutableStateOf(durationMs ?: 0L) }

    val mediaPlayer = remember { MediaPlayer() }

    // Prepare asynchronously off the main thread
    LaunchedEffect(audioUri) {
        withContext(Dispatchers.IO) {
            try {
                mediaPlayer.setDataSource(context, audioUri)
                mediaPlayer.prepare()
                if (durationMs == null || durationMs == 0L) {
                    totalMs = mediaPlayer.duration.toLong()
                }
                isPrepared = true
            } catch (_: Exception) {
                hasError = true
            }
        }
    }

    // Update progress while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            try {
                currentMs = mediaPlayer.currentPosition.toLong()
                progress = if (totalMs > 0) currentMs.toFloat() / totalMs else 0f
                if (!mediaPlayer.isPlaying) {
                    isPlaying = false
                    progress = 0f
                    currentMs = 0
                    mediaPlayer.seekTo(0)
                }
            } catch (_: IllegalStateException) {
                isPlaying = false
            }
            delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.release()
            } catch (_: Exception) { }
        }
    }

    if (hasError) {
        // Fallback UI for unavailable audio
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Audio unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (!isPrepared) return@IconButton
                    if (isPlaying) {
                        mediaPlayer.pause()
                        isPlaying = false
                    } else {
                        mediaPlayer.start()
                        isPlaying = true
                    }
                },
                modifier = Modifier.size(36.dp),
                enabled = isPrepared
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPrepared) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(totalMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
