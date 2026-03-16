package com.example.memly.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MemoryDetailScreen(
    onNavigateBack: () -> Unit
) {
    // Placeholder — full detail view will be built out
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Memory Detail",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
