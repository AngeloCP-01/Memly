package com.example.memly.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.memly.data.local.entity.Mood

fun Mood.color(): Color = when (this) {
    Mood.HAPPY -> MoodColors.Happy
    Mood.NOSTALGIC -> MoodColors.Nostalgic
    Mood.ADVENTUROUS -> MoodColors.Adventurous
    Mood.CALM -> MoodColors.Calm
    Mood.EXCITED -> MoodColors.Excited
    Mood.GRATEFUL -> MoodColors.Grateful
    Mood.ROMANTIC -> MoodColors.Romantic
    Mood.REFLECTIVE -> MoodColors.Reflective
    Mood.SAD -> MoodColors.Sad
    Mood.FUNNY -> MoodColors.Funny
}
