package com.example.memly.ui.theme

import androidx.compose.ui.graphics.Color

// --- Light Theme Colors ---
val LightPrimary = Color(0xFFFF6B6B)           // Soft Coral
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFE0E0)  // Light Coral
val LightOnPrimaryContainer = Color(0xFF5C2A2A)

val LightSecondary = Color(0xFF4BC0C8)          // Soft Teal
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE0F7FA) // Light Teal
val LightOnSecondaryContainer = Color(0xFF1A3A3D)

val LightTertiary = Color(0xFFFFB347)           // Soft Amber (nostalgic accent)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFE8C8)
val LightOnTertiaryContainer = Color(0xFF5C3D1A)

val LightBackground = Color(0xFFFAFAFA)         // Off-White
val LightOnBackground = Color(0xFF333333)        // Charcoal

val LightSurface = Color(0xFFFFF3E6)            // Warm Beige
val LightOnSurface = Color(0xFF444444)           // Dark Gray
val LightSurfaceVariant = Color(0xFFF5EDE3)     // Light Beige
val LightOnSurfaceVariant = Color(0xFF777777)    // Medium Gray

val LightOutline = Color(0xFFE0E0E0)
val LightOutlineVariant = Color(0xFFD0C8BE)

val LightError = Color(0xFFE53935)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightSurfaceContainer = Color(0xFFFFF8F0)
val LightSurfaceContainerHigh = Color(0xFFFFF3E6)
val LightSurfaceContainerHighest = Color(0xFFFFEDD9)

// --- Dark Theme Colors ---
val DarkPrimary = Color(0xFFFF8A8A)             // Lighter Coral for dark bg
val DarkOnPrimary = Color(0xFF5C1A1A)
val DarkPrimaryContainer = Color(0xFF5C2A2A)
val DarkOnPrimaryContainer = Color(0xFFFFE0E0)

val DarkSecondary = Color(0xFF80DEEA)           // Lighter Teal
val DarkOnSecondary = Color(0xFF1A3A3D)
val DarkSecondaryContainer = Color(0xFF1A3A3D)
val DarkOnSecondaryContainer = Color(0xFFE0F7FA)

val DarkTertiary = Color(0xFFFFCC80)
val DarkOnTertiary = Color(0xFF5C3D1A)
val DarkTertiaryContainer = Color(0xFF5C3D1A)
val DarkOnTertiaryContainer = Color(0xFFFFE8C8)

val DarkBackground = Color(0xFF121212)           // Deep Charcoal
val DarkOnBackground = Color(0xFFF0F0F0)

val DarkSurface = Color(0xFF1E1E1E)             // Dark Surface
val DarkOnSurface = Color(0xFFE0E0E0)
val DarkSurfaceVariant = Color(0xFF2A2A2A)      // Elevated Surface
val DarkOnSurfaceVariant = Color(0xFF999999)

val DarkOutline = Color(0xFF3A3A3A)
val DarkOutlineVariant = Color(0xFF4A4A4A)

val DarkError = Color(0xFFFF6B6B)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkSurfaceContainer = Color(0xFF252525)
val DarkSurfaceContainerHigh = Color(0xFF2E2E2E)
val DarkSurfaceContainerHighest = Color(0xFF383838)

// --- Mood Colors (shared across themes) ---
object MoodColors {
    val Happy = Color(0xFFFFD93D)        // Warm Yellow
    val Nostalgic = Color(0xFFFFB347)    // Soft Amber
    val Adventurous = Color(0xFF4BC0C8)  // Teal
    val Calm = Color(0xFF74B9FF)         // Soft Blue
    val Excited = Color(0xFFFF6348)      // Coral Orange
    val Grateful = Color(0xFF7ED6A8)     // Soft Green
    val Romantic = Color(0xFFFF85A1)     // Rose Pink
    val Reflective = Color(0xFFA29BFE)   // Lavender
    val Sad = Color(0xFF778CA3)          // Muted Blue
    val Funny = Color(0xFFBADC58)        // Bright Lime
}
