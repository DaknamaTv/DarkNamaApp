@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dark.darknama.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dark.darknama.BuildConfig
import com.dark.darknama.R
import com.dark.darknama.data.model.SubtitleSettings
import com.dark.darknama.data.model.VideoPlayerSettings
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.data.model.FontType
import com.dark.darknama.data.model.WatchedEpisode
import com.dark.darknama.ui.theme.ThemeMode
import com.dark.darknama.ui.theme.ThemeSettings
import com.dark.darknama.ui.theme.ThemeManager
import com.dark.darknama.ui.theme.colorOptions
import com.dark.darknama.ui.theme.defaultPrimaryColor
import com.dark.darknama.utils.LanguageUtils
import com.dark.darknama.utils.StorageUtils
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URL

/**
 * Modifier that draws a visible focus ring so D-pad users can see which
 * element is focused on Android TV.
 */
@Composable
private fun Modifier.tvFocusBorder(
    isFocused: Boolean,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
): Modifier = this.border(
    width = if (isFocused) 2.dp else 0.dp,
    color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
    shape = shape
)

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val html_url: String
)

@Composable
fun SettingsScreen(
    onThemeSettingsChanged: (ThemeSettings) -> Unit = {},
    onFontSettingsChanged: (FontSettings) -> Unit = {}, // Add this parameter
    navController: NavController? = null
) {
    val themeManager = ThemeManager(androidx.compose.ui.platform.LocalContext.current)
    var themeSettings by remember { mutableStateOf(themeManager.loadThemeSettings()) }
    val context = LocalContext.current
    var subtitleSettings by remember { mutableStateOf(StorageUtils.loadSubtitleSettings(context)) }
    var videoPlayerSettings by remember { mutableStateOf(StorageUtils.loadVideoPlayerSettings(context)) }
    var fontSettings by remember { mutableStateOf(StorageUtils.loadFontSettings(context)) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showClearWatchedEpisodesDialog by remember { mutableStateOf(false) }
    var latestVersionUrl by remember { mutableStateOf("") }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var watchedEpisodesCacheSize by remember { mutableStateOf(0L) }
    
    // Configure JSON to ignore unknown keys
    val json = Json { ignoreUnknownKeys = true }
    
    // Load watched episodes cache size when screen is shown
    androidx.compose.runtime.LaunchedEffect(Unit) {
        watchedEpisodesCacheSize = try {
            val file = java.io.File(context.filesDir, "watched_episodes.json")
            if (file.exists()) {
                file.length()
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error calculating cache size", e)
            0L
        }
    }
    
    // Update parent when settings change
    fun updateThemeSettings(newSettings: ThemeSettings) {
        themeSettings = newSettings
        onThemeSettingsChanged(newSettings)
        themeManager.saveThemeSettings(newSettings)
    }
    
    // Update subtitle settings
    fun updateSubtitleSettings(newSettings: SubtitleSettings) {
        subtitleSettings = newSettings
        StorageUtils.saveSubtitleSettings(context, newSettings)
    }
    
    // Update video player settings
    fun updateVideoPlayerSettings(newSettings: VideoPlayerSettings) {
        videoPlayerSettings = newSettings
        StorageUtils.saveVideoPlayerSettings(context, newSettings)
    }
    
    // Update font settings
    fun updateFontSettings(newSettings: FontSettings) {
        fontSettings = newSettings
        StorageUtils.saveFontSettings(context, newSettings)
        onFontSettingsChanged(newSettings) // Add this line to notify parent
    }
    
    // Reset all settings to defaults
    fun resetToDefaults() {
        val defaultSettings = ThemeSettings()
        updateThemeSettings(defaultSettings)
        // Reset subtitle settings to default as well
        val defaultSubtitleSettings = SubtitleSettings.getDefaultSettings(context)
        updateSubtitleSettings(defaultSubtitleSettings)
        // Reset video player settings to default as well
        val defaultVideoPlayerSettings = VideoPlayerSettings.DEFAULT
        updateVideoPlayerSettings(defaultVideoPlayerSettings)
        // Reset font settings to default as well
        val defaultFontSettings = FontSettings.DEFAULT
        updateFontSettings(defaultFontSettings)
    }
    
    // Calculate watched episodes cache size
    fun calculateWatchedEpisodesCacheSize(): Long {
        return try {
            val file = java.io.File(context.filesDir, "watched_episodes.json")
            if (file.exists()) {
                file.length()
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error calculating cache size", e)
            0L
        }
    }
    
    // Clear all watched episodes
    fun clearWatchedEpisodes() {
        try {
            StorageUtils.clearAllWatchedEpisodes(context)
            watchedEpisodesCacheSize = 0L
            Toast.makeText(context, context.getString(R.string.watched_cache_cleared), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error clearing watched episodes", e)
            Toast.makeText(context, context.getString(R.string.error_clearing_cache), Toast.LENGTH_SHORT).show()
        }
    }
    
    // Compare version strings
    fun isVersionNewer(currentVersion: String, latestVersion: String): Boolean {
        try {
            // Remove 'v' prefix if present
            val current = currentVersion.removePrefix("v")
            val latest = latestVersion.removePrefix("v")
            
            // Split version numbers
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            
            // Compare each part
            for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
                val currentPart = if (i < currentParts.size) currentParts[i] else 0
                val latestPart = if (i < latestParts.size) latestParts[i] else 0
                
                if (latestPart > currentPart) return true
                if (latestPart < currentPart) return false
            }
            
            return false // Versions are equal
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error comparing versions", e)
            return false
        }
    }
    
    // Check for updates
    fun checkForUpdates() {
        isCheckingUpdate = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.github.com/repos/DaknamaTv/DarkNamaApp/releases")
                val connection = url.openConnection()
                connection.setRequestProperty("User-Agent", "DarkNama-App")
                val response = connection.getInputStream().bufferedReader().use { it.readText() }
                
                val releases = json.decodeFromString<List<GitHubRelease>>(response)
                if (releases.isNotEmpty()) {
                    val latestRelease = releases.first()
                    val latestVersion = latestRelease.tag_name
                    val currentVersion = "v${BuildConfig.VERSION_NAME}"
                    
                    withContext(Dispatchers.Main) {
                        isCheckingUpdate = false
                        if (isVersionNewer(currentVersion, latestVersion)) {
                            latestVersionUrl = latestRelease.html_url
                            showUpdateDialog = true
                        } else {
                            Toast.makeText(context, context.getString(R.string.app_up_to_date), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isCheckingUpdate = false
                        Toast.makeText(context, context.getString(R.string.unable_check_updates), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsScreen", "Error checking for updates", e)
                withContext(Dispatchers.Main) {
                    isCheckingUpdate = false
                    Toast.makeText(context, context.getString(R.string.error_check_updates), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Language toggle button (English <-> Farsi)
                    IconButton(
                        onClick = {
                            LanguageUtils.toggleLanguage(context)
                            // Recreate the activity so the new locale is applied
                            (context as? android.app.Activity)?.recreate()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = stringResource(R.string.change_language),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Add like icon button that navigates to favorites
                    navController?.let {
                        IconButton(
                            onClick = { navController.navigate("favorites") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.favorites),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        // Theme Settings Card
        item {
            var isExpanded by remember { mutableStateOf(false) }
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400, delayMillis = 100)),
                exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(animationSpec = tween(400))
            ) {
                Card(
                    onClick = { isExpanded = !isExpanded },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusBorder(isFocused),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatColorFill,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.theme_settings),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Theme Mode Section
                                Text(
                                    text = stringResource(R.string.theme_mode),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                ThemeModeOption(
                                    mode = ThemeMode.LIGHT,
                                    label = stringResource(R.string.theme_light),
                                    isSelected = themeSettings.themeMode == ThemeMode.LIGHT,
                                    onSelect = { mode ->
                                        val newSettings = themeSettings.copy(themeMode = mode)
                                        updateThemeSettings(newSettings)
                                    }
                                )
                                
                                ThemeModeOption(
                                    mode = ThemeMode.DARK,
                                    label = stringResource(R.string.theme_dark),
                                    isSelected = themeSettings.themeMode == ThemeMode.DARK,
                                    onSelect = { mode ->
                                        val newSettings = themeSettings.copy(themeMode = mode)
                                        updateThemeSettings(newSettings)
                                    }
                                )
                                
                                ThemeModeOption(
                                    mode = ThemeMode.SYSTEM,
                                    label = stringResource(R.string.theme_system),
                                    isSelected = themeSettings.themeMode == ThemeMode.SYSTEM,
                                    onSelect = { mode ->
                                        val newSettings = themeSettings.copy(themeMode = mode)
                                        updateThemeSettings(newSettings)
                                    }
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Primary Color Section
                                Text(
                                    text = stringResource(R.string.primary_color),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                // Display color options in rows of 4
                                for (rowColors in colorOptions.chunked(4)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        rowColors.forEach { color ->
                                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                                ColorOption(
                                                    color = color,
                                                    isSelected = themeSettings.primaryColor == color,
                                                    onSelect = { selectedColor ->
                                                        val newSettings = themeSettings.copy(primaryColor = selectedColor)
                                                        updateThemeSettings(newSettings)
                                                    }
                                                )
                                            }
                                        }
                                        // Fill remaining spaces if less than 4 items
                                        repeat(4 - rowColors.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                
                                // Add default color option
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    val defaultColor = defaultPrimaryColor
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        ColorOption(
                                            color = defaultColor,
                                            isSelected = themeSettings.primaryColor == defaultColor,
                                            onSelect = { selectedColor ->
                                                val newSettings = themeSettings.copy(primaryColor = selectedColor)
                                                updateThemeSettings(newSettings)
                                            },
                                            label = stringResource(R.string.color_default)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500, delayMillis = 200)),
                exit = fadeOut(animationSpec = tween(500)) + slideOutVertically(animationSpec = tween(500))
            ) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Video Player Settings Card
        item {
            var isExpanded by remember { mutableStateOf(false) }
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(animationSpec = tween(600, delayMillis = 300)),
                exit = fadeOut(animationSpec = tween(600)) + slideOutVertically(animationSpec = tween(600))
            ) {
                Card(
                    onClick = { isExpanded = !isExpanded },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusBorder(isFocused),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.video_player_settings),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.seek_time, videoPlayerSettings.seekTimeSeconds),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Slider(
                                    value = videoPlayerSettings.seekTimeSeconds.toFloat(),
                                    onValueChange = { seconds ->
                                        updateVideoPlayerSettings(videoPlayerSettings.copy(seekTimeSeconds = seconds.toInt()))
                                    },
                                    valueRange = 5f..30f,
                                    steps = 24, // Allow values from 5 to 30 in 1-second increments,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                                            when (keyEvent.key) {
                                                Key.DirectionLeft -> {
                                                    val newValue = (videoPlayerSettings.seekTimeSeconds - 1).coerceIn(5, 30)
                                                    updateVideoPlayerSettings(videoPlayerSettings.copy(seekTimeSeconds = newValue))
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    val newValue = (videoPlayerSettings.seekTimeSeconds + 1).coerceIn(5, 30)
                                                    updateVideoPlayerSettings(videoPlayerSettings.copy(seekTimeSeconds = newValue))
                                                    true
                                                }
                                                else -> false
                                            }
                                        }
                                        .focusable()
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Subtitle Settings Section
                                Text(
                                    text = stringResource(R.string.subtitle_settings),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                // Text color setting
                                SubtitleColorSetting(
                                    title = stringResource(R.string.subtitle_text_color),
                                    currentColor = Color(subtitleSettings.textColor),
                                    onColorSelected = { color ->
                                        updateSubtitleSettings(subtitleSettings.copy(textColor = color.toArgb()))
                                    },
                                    defaultColor = Color.Yellow
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Border color setting (Background)
                                SubtitleColorSetting(
                                    title = stringResource(R.string.subtitle_background_color),
                                    currentColor = Color(subtitleSettings.borderColor),
                                    onColorSelected = { color ->
                                        updateSubtitleSettings(subtitleSettings.copy(borderColor = color.toArgb()))
                                    },
                                    noColorOption = true,
                                    glassBackgroundOption = true
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Text size setting
                                Text(
                                    text = stringResource(R.string.subtitle_text_size, subtitleSettings.textSize.toInt()),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Slider(
                                    value = subtitleSettings.textSize,
                                    onValueChange = { size ->
                                        updateSubtitleSettings(subtitleSettings.copy(textSize = size))
                                    },
                                    valueRange = 10f..50f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                                            when (keyEvent.key) {
                                                Key.DirectionLeft -> {
                                                    val newValue = (subtitleSettings.textSize - 1).coerceIn(10f, 50f)
                                                    updateSubtitleSettings(subtitleSettings.copy(textSize = newValue))
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    val newValue = (subtitleSettings.textSize + 1).coerceIn(10f, 50f)
                                                    updateSubtitleSettings(subtitleSettings.copy(textSize = newValue))
                                                    true
                                                }
                                                else -> false
                                            }
                                        }
                                        .focusable()
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(700)) + slideInVertically(animationSpec = tween(700, delayMillis = 400)),
                exit = fadeOut(animationSpec = tween(700)) + slideOutVertically(animationSpec = tween(700))
            ) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Font Settings Card
        item {
            var isExpanded by remember { mutableStateOf(false) }
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(800)) + slideInVertically(animationSpec = tween(800, delayMillis = 600)),
                exit = fadeOut(animationSpec = tween(800)) + slideOutVertically(animationSpec = tween(800))
            ) {
                Card(
                    onClick = { isExpanded = !isExpanded },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusBorder(isFocused),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.font_settings),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.select_font),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                FontOption(
                                    fontType = FontType.DEFAULT,
                                    label = stringResource(R.string.font_system_default),
                                    isSelected = fontSettings.fontType == FontType.DEFAULT,
                                    onSelect = { fontType ->
                                        val newSettings = fontSettings.copy(fontType = fontType)
                                        updateFontSettings(newSettings)
                                    }
                                )
                                
                                FontOption(
                                    fontType = FontType.VAZIRMATN,
                                    label = stringResource(R.string.font_vazirmatn),
                                    isSelected = fontSettings.fontType == FontType.VAZIRMATN,
                                    onSelect = { fontType ->
                                        val newSettings = fontSettings.copy(fontType = fontType)
                                        updateFontSettings(newSettings)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(900)) + slideInVertically(animationSpec = tween(900, delayMillis = 700)),
                exit = fadeOut(animationSpec = tween(900)) + slideOutVertically(animationSpec = tween(900))
            ) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Episode Marks Cache Card
        item {
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(900)) + slideInVertically(animationSpec = tween(900, delayMillis = 600)),
                exit = fadeOut(animationSpec = tween(900)) + slideOutVertically(animationSpec = tween(900))
            ) {
                Card(
                    onClick = { showClearWatchedEpisodesDialog = true },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusBorder(isFocused),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.episodes_cache),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f)
                            )
                        }
                        
                        Text(
                            text = stringResource(
                                R.string.cache_size,
                                if (watchedEpisodesCacheSize > 0) stringResource(R.string.cache_bytes, watchedEpisodesCacheSize.toInt()) else stringResource(R.string.cache_empty)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = stringResource(R.string.cache_clear_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(animationSpec = tween(1000, delayMillis = 700)),
                exit = fadeOut(animationSpec = tween(1000)) + slideOutVertically(animationSpec = tween(1000))
            ) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // About Card
        item {
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(1100)) + slideInVertically(animationSpec = tween(1100, delayMillis = 800)),
                exit = fadeOut(animationSpec = tween(1100)) + slideOutVertically(animationSpec = tween(1100))
            ) {
                Card(
                    onClick = { navController?.navigate("about") },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusBorder(isFocused),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.about),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f)
                            )
                        }
                        
                        Text(
                            text = stringResource(R.string.about_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(1200)) + slideInVertically(animationSpec = tween(1200, delayMillis = 900)),
                exit = fadeOut(animationSpec = tween(1200)) + slideOutVertically(animationSpec = tween(1200))
            ) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Check for Updates Card
        item {
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(1200)) + slideInVertically(animationSpec = tween(1200, delayMillis = 900)),
                exit = fadeOut(animationSpec = tween(1200)) + slideOutVertically(animationSpec = tween(1200))
            ) {
                Card(
                    onClick = {
                        if (!isCheckingUpdate) {
                            checkForUpdates()
                        }
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusBorder(isFocused),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.check_updates),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f)
                            )
                            if (isCheckingUpdate) {
                                // Show loading indicator
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Checking",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(2.dp)
                                )
                            }
                        }
                        
                        Text(
                            text = stringResource(R.string.check_updates_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(1300)) + slideInVertically(animationSpec = tween(1300, delayMillis = 1000)),
                exit = fadeOut(animationSpec = tween(1300)) + slideOutVertically(animationSpec = tween(1300))
            ) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Reset to Defaults Card
        item {
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(1400)) + slideInVertically(animationSpec = tween(1400, delayMillis = 1100)),
                exit = fadeOut(animationSpec = tween(1400)) + slideOutVertically(animationSpec = tween(1400))
            ) {
                Card(
                    onClick = { showResetDialog = true },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusBorder(isFocused),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.reset_defaults),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f)
                            )
                        }
                        
                        Text(
                            text = stringResource(R.string.reset_defaults_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(text = stringResource(R.string.reset_settings_title))
            },
            text = {
                Text(stringResource(R.string.reset_settings_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetToDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Clear watched episodes confirmation dialog
    if (showClearWatchedEpisodesDialog) {
        AlertDialog(
            onDismissRequest = { showClearWatchedEpisodesDialog = false },
            title = {
                Text(text = stringResource(R.string.clear_watched_title))
            },
            text = {
                Text(stringResource(R.string.clear_watched_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearWatchedEpisodes()
                        showClearWatchedEpisodesDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear_all))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearWatchedEpisodesDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Update available dialog
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Text(text = stringResource(R.string.update_available_title))
            },
            text = {
                Text(stringResource(R.string.update_available_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latestVersionUrl))
                        context.startActivity(intent)
                        showUpdateDialog = false
                    }
                ) {
                    Text(stringResource(R.string.download))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUpdateDialog = false }
                ) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }
}

@Composable
fun ThemeModeOption(
    mode: ThemeMode,
    label: String,
    isSelected: Boolean,
    onSelect: (ThemeMode) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .tvFocusBorder(isFocused, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onSelect(mode) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // onClick = null keeps the RadioButton purely visual so the whole row
        // is a single focus target for the TV remote.
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onSelect: (Color) -> Unit,
    label: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(
                    width = if (isFocused) 3.dp else 0.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(color)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onSelect(color) }
                .then(
                    if (isSelected) {
                        Modifier.padding(4.dp)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = if (color == Color.White || color == Color.Yellow) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SubtitleColorSetting(
    title: String,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    noColorOption: Boolean = false,
    glassBackgroundOption: Boolean = false,
    defaultColor: Color? = null
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Color options
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // No color option (transparent)
                if (noColorOption) {
                    ColorOptionButton(
                        color = Color.Transparent,
                        isSelected = currentColor == Color.Transparent,
                        onClick = { onColorSelected(Color.Transparent) },
                        showBorder = true,
                        label = stringResource(R.string.color_empty)
                    )
                }
                
                // Glass background option (semi-transparent)
                if (glassBackgroundOption) {
                    ColorOptionButton(
                        color = Color.Black.copy(alpha = 0.5f),
                        isSelected = currentColor == Color.Black.copy(alpha = 0.5f),
                        onClick = { onColorSelected(Color.Black.copy(alpha = 0.5f)) },
                        label = stringResource(R.string.color_glass)
                    )
                }
                
                // Default color option
                if (defaultColor != null) {
                    ColorOptionButton(
                        color = defaultColor,
                        isSelected = currentColor == defaultColor,
                        onClick = { onColorSelected(defaultColor) }
                    )
                }
                
                // Standard color options
                listOf(Color.White, Color.Black, Color.Red, Color.Blue, Color.Green).forEach { color ->
                    ColorOptionButton(
                        color = color,
                        isSelected = currentColor == color,
                        onClick = { onColorSelected(color) }
                    )
                }
            }
            
            // Current color preview
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(currentColor)
                    .then(
                        if (currentColor == Color.Transparent) {
                            Modifier.background(Color.Gray.copy(alpha = 0.3f))
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

@Composable
fun ColorOptionButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    showBorder: Boolean = false,
    label: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = if (color == Color.White || color == Color.Yellow) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else if (color == Color.Transparent && showBorder) {
                Icon(
                    imageVector = Icons.Default.Brightness1,
                    contentDescription = "No color",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun FontOption(
    fontType: FontType,
    label: String,
    isSelected: Boolean,
    onSelect: (FontType) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .tvFocusBorder(isFocused, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onSelect(fontType) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // onClick = null keeps the RadioButton purely visual so the whole row
        // is a single focus target for the TV remote.
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}