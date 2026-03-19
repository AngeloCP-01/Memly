package com.example.memly.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Custom centered toast that displays a message in the middle of the screen
 * with a fade+scale animation. Auto-dismisses after [durationMs].
 */
@Composable
fun MemlyToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = true,
    durationMs: Long = 3000L
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            visible = true
            delay(durationMs)
            visible = false
            delay(300) // wait for exit animation
            onDismiss()
        } else {
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible && message != null,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                shadowElevation = 8.dp,
                tonalElevation = 4.dp
            ) {
                Text(
                    text = message ?: "",
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .widthIn(max = 280.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
