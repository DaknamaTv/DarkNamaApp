package com.pira.darknama.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val primaryColor: Color = defaultPrimaryColor
    // Removed secondaryColor as it's no longer used
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

// Default colors
val defaultPrimaryColor = Color(0xFF6650a4) // Purple40
// Removed defaultSecondaryColor as it's no longer used

// Predefined color options
val colorOptions = listOf(
    Color(0xFF6650a4), // Purple
    Color(0xFF006A6A), // Teal
    Color(0xFFBA1A1A), // Red
    Color(0xFF7D5260), // Pink
    Color(0xFF625B71), // PurpleGrey
    Color(0xFF006D32), // Green
    Color(0xFF3B5BA9), // Blue
    Color(0xFFFFB700)  // Yellow
)

@Composable
fun rememberThemeSettings(): ThemeSettings {
    return remember { ThemeSettings() }
}