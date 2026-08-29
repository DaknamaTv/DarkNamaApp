package com.dark.darknama.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.utils.StorageUtils

// Set of Material typography styles to start with
fun appTypography(fontFamily: FontFamily?): androidx.compose.material3.Typography {
    return androidx.compose.material3.Typography(
        bodyLarge = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

/**
 * Picks black or white for text/icons drawn on top of [color] so the
 * result always stays readable (important for the light/yellow swatches).
 */
private fun contrastingOn(color: Color): Color =
    if (color.luminance() > 0.5f) Color.Black else Color.White

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2D2D2D),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFE7E0EC),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun DarkNamaTheme(
    themeSettings: ThemeSettings = ThemeSettings(),
    fontSettings: FontSettings = FontSettings(), // Add font settings parameter
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (themeSettings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    // Load font family based on font settings
    val fontFamily = FontManager.loadFontFamily(context, fontSettings.fontType)
    
    // The colour scheme is always derived from the user's selected primary
    // colour (including the built-in "Default" purple).
    //
    // NOTE: previously, picking the default colour fell through to Android 12+
    // dynamic (wallpaper-based) colours, which made the "Default theme" option
    // look random / broken. The dynamic branch has been removed so the default
    // theme is now deterministic and matches the app's brand colour.
    val primary = themeSettings.primaryColor
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = contrastingOn(primary),
            primaryContainer = primary.copy(alpha = 0.30f),
            onPrimaryContainer = Color.White,
            secondary = primary,
            onSecondary = contrastingOn(primary),
            tertiary = Pink80,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF2D2D2D),
            onTertiary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFE0E0E0)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = contrastingOn(primary),
            primaryContainer = primary.copy(alpha = 0.20f),
            onPrimaryContainer = Color(0xFF1C1B1F),
            secondary = primary,
            onSecondary = contrastingOn(primary),
            tertiary = Pink40,
            background = Color(0xFFFFFBFE),
            surface = Color(0xFFFFFBFE),
            surfaceVariant = Color(0xFFE7E0EC),
            onTertiary = Color.White,
            onBackground = Color(0xFF1C1B1F),
            onSurface = Color(0xFF1C1B1F),
            onSurfaceVariant = Color(0xFF49454F)
        )
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // Light status-bar icons must be used on a LIGHT status bar, so the
            // flag depends on the brightness of the primary colour, not on the
            // theme mode (this was inverted before).
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                colorScheme.primary.luminance() > 0.5f
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(fontFamily),
        content = content
    )
}