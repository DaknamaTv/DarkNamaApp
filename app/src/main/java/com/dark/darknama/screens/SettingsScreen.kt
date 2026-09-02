@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dark.darknama.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dark.darknama.BuildConfig
import com.dark.darknama.R
import com.dark.darknama.components.SettingsNavigationCard
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.data.model.FontType
import com.dark.darknama.data.model.SubtitleSettings
import com.dark.darknama.data.model.VideoPlayerSettings
import com.dark.darknama.navigation.AppScreens
import com.dark.darknama.ui.theme.ThemeManager
import com.dark.darknama.ui.theme.ThemeMode
import com.dark.darknama.ui.theme.ThemeSettings
import com.dark.darknama.utils.LanguageUtils
import com.dark.darknama.utils.StorageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val html_url: String
)

/**
 * Settings hub.
 *
 * Every category (Theme / Player+Subtitles / Font) opens as its own
 * full page instead of an inline drop-down, which is far easier to use
 * with an Android TV remote (same pattern as the "About" page).
 */
@Composable
fun SettingsScreen(
    onThemeSettingsChanged: (ThemeSettings) -> Unit = {},
    onFontSettingsChanged: (FontSettings) -> Unit = {},
    navController: NavController? = null
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }

    // Re-read the persisted settings on every recomposition of this screen so
    // the summary lines stay correct after returning from a sub-page.
    var refreshKey by remember { mutableStateOf(0) }
    val themeSettings = remember(refreshKey) { themeManager.loadThemeSettings() }
    val subtitleSettings = remember(refreshKey) { StorageUtils.loadSubtitleSettings(context) }
    val videoPlayerSettings = remember(refreshKey) { StorageUtils.loadVideoPlayerSettings(context) }
    val fontSettings = remember(refreshKey) { StorageUtils.loadFontSettings(context) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showClearWatchedEpisodesDialog by remember { mutableStateOf(false) }
    var latestVersionUrl by remember { mutableStateOf("") }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var watchedEpisodesCacheSize by remember { mutableStateOf(0L) }

    val json = Json { ignoreUnknownKeys = true }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        watchedEpisodesCacheSize = try {
            val file = java.io.File(context.filesDir, "watched_episodes.json")
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error calculating cache size", e)
            0L
        }
    }

    // Reset all settings to defaults
    fun resetToDefaults() {
        val defaultTheme = ThemeSettings()
        themeManager.saveThemeSettings(defaultTheme)
        onThemeSettingsChanged(defaultTheme)

        StorageUtils.saveSubtitleSettings(context, SubtitleSettings.getDefaultSettings(context))
        StorageUtils.saveVideoPlayerSettings(context, VideoPlayerSettings.DEFAULT)

        val defaultFont = FontSettings.DEFAULT
        StorageUtils.saveFontSettings(context, defaultFont)
        onFontSettingsChanged(defaultFont)

        // Force the summaries to reload
        refreshKey++
    }

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

    fun isVersionNewer(currentVersion: String, latestVersion: String): Boolean {
        return try {
            val currentParts = currentVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latestVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
                val c = currentParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error comparing versions", e)
            false
        }
    }

    fun checkForUpdates() {
        isCheckingUpdate = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.github.com/repos/DarknamaTv/DarkNamaApp/releases")
                val connection = url.openConnection()
                connection.setRequestProperty("User-Agent", "DarkNama-App")
                val response = connection.getInputStream().bufferedReader().use { it.readText() }

                val releases = json.decodeFromString<List<GitHubRelease>>(response)
                if (releases.isNotEmpty()) {
                    val latestRelease = releases.first()
                    val currentVersion = "v${BuildConfig.VERSION_NAME}"
                    withContext(Dispatchers.Main) {
                        isCheckingUpdate = false
                        if (isVersionNewer(currentVersion, latestRelease.tag_name)) {
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

    // ---- Summary lines shown under each category ----
    val themeModeLabel = when (themeSettings.themeMode) {
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    }
    val fontLabel = when (fontSettings.fontType) {
        FontType.DEFAULT -> stringResource(R.string.font_system_default)
        FontType.VAZIRMATN -> stringResource(R.string.font_vazirmatn)
    }
    val playerSummary = stringResource(
        R.string.player_settings_summary,
        videoPlayerSettings.seekTimeSeconds,
        subtitleSettings.textSize.toInt()
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ---------- Header ----------
        item {
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

                // Language toggle (English <-> Farsi)
                val langInteraction = remember { MutableInteractionSource() }
                val isLangFocused by langInteraction.collectIsFocusedAsState()
                IconButton(
                    onClick = {
                        LanguageUtils.toggleLanguage(context)
                        (context as? android.app.Activity)?.recreate()
                    },
                    interactionSource = langInteraction,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = stringResource(R.string.change_language),
                        tint = if (isLangFocused)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                navController?.let {
                    val favInteraction = remember { MutableInteractionSource() }
                    val isFavFocused by favInteraction.collectIsFocusedAsState()
                    IconButton(
                        onClick = { navController.navigate(AppScreens.Favorites.route) },
                        interactionSource = favInteraction,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = stringResource(R.string.favorites),
                            tint = if (isFavFocused)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ---------- Theme settings -> own page ----------
        item {
            SettingsNavigationCard(
                icon = Icons.Default.FormatColorFill,
                title = stringResource(R.string.theme_settings),
                summary = themeModeLabel,
                onClick = { navController?.navigate(AppScreens.ThemeSettings.route) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Player & subtitle settings -> own page ----------
        item {
            SettingsNavigationCard(
                icon = Icons.Default.Subtitles,
                title = stringResource(R.string.video_player_settings),
                summary = playerSummary,
                onClick = { navController?.navigate(AppScreens.PlayerSettings.route) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Font settings -> own page ----------
        item {
            SettingsNavigationCard(
                icon = Icons.Default.TextFields,
                title = stringResource(R.string.font_settings),
                summary = fontLabel,
                onClick = { navController?.navigate(AppScreens.FontSettings.route) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Episode marks cache ----------
        item {
            SettingsNavigationCard(
                icon = Icons.Default.Check,
                title = stringResource(R.string.episodes_cache),
                summary = stringResource(
                    R.string.cache_size,
                    if (watchedEpisodesCacheSize > 0)
                        stringResource(R.string.cache_bytes, watchedEpisodesCacheSize.toInt())
                    else
                        stringResource(R.string.cache_empty)
                ),
                onClick = { showClearWatchedEpisodesDialog = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- About ----------
        item {
            SettingsNavigationCard(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about),
                summary = stringResource(R.string.about_description),
                onClick = { navController?.navigate(AppScreens.About.route) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Check for updates ----------
        item {
            SettingsNavigationCard(
                icon = Icons.Default.Download,
                title = stringResource(R.string.check_updates),
                summary = if (isCheckingUpdate)
                    stringResource(R.string.checking_updates)
                else
                    stringResource(R.string.current_version, BuildConfig.VERSION_NAME),
                onClick = { if (!isCheckingUpdate) checkForUpdates() }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Reset to defaults ----------
        item {
            SettingsNavigationCard(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.reset_defaults),
                summary = stringResource(R.string.reset_defaults_hint),
                onClick = { showResetDialog = true }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // ---------- Dialogs ----------
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = stringResource(R.string.reset_settings_title)) },
            text = { Text(stringResource(R.string.reset_settings_message)) },
            confirmButton = {
                TextButton(onClick = {
                    resetToDefaults()
                    showResetDialog = false
                }) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showClearWatchedEpisodesDialog) {
        AlertDialog(
            onDismissRequest = { showClearWatchedEpisodesDialog = false },
            title = { Text(text = stringResource(R.string.clear_watched_title)) },
            text = { Text(stringResource(R.string.clear_watched_message)) },
            confirmButton = {
                TextButton(onClick = {
                    clearWatchedEpisodes()
                    showClearWatchedEpisodesDialog = false
                }) {
                    Text(stringResource(R.string.clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearWatchedEpisodesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text(text = stringResource(R.string.update_available_title)) },
            text = { Text(stringResource(R.string.update_available_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latestVersionUrl))
                    context.startActivity(intent)
                    showUpdateDialog = false
                }) {
                    Text(stringResource(R.string.download))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }
}
