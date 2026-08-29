package com.dark.darknama.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dark.darknama.R
import com.dark.darknama.components.SettingsColorSwatch
import com.dark.darknama.components.SettingsSectionTitle
import com.dark.darknama.components.SettingsStepperRow
import com.dark.darknama.components.SettingsSubPage
import com.dark.darknama.data.model.SubtitleSettings
import com.dark.darknama.data.model.VideoPlayerSettings
import com.dark.darknama.utils.StorageUtils

/** Subtitle text size bounds shared with the in-player settings dialog. */
const val SUBTITLE_TEXT_SIZE_MIN = 10f
const val SUBTITLE_TEXT_SIZE_MAX = 60f
const val SUBTITLE_TEXT_SIZE_STEP = 1f

/** Seek-time bounds for the double-tap / D-pad seek. */
private const val SEEK_MIN = 5
private const val SEEK_MAX = 60
private const val SEEK_STEP = 5

/**
 * Full-page Video player & subtitle settings.
 *
 * Uses +/- steppers instead of sliders so it can be adjusted precisely
 * with an Android TV remote, and shows a live subtitle preview so the
 * chosen size/colour is immediately visible.
 */
@Composable
fun PlayerSettingsScreen(navController: NavController?) {
    val context = LocalContext.current
    var subtitleSettings by remember { mutableStateOf(StorageUtils.loadSubtitleSettings(context)) }
    var playerSettings by remember { mutableStateOf(StorageUtils.loadVideoPlayerSettings(context)) }

    fun updateSubtitle(newSettings: SubtitleSettings) {
        subtitleSettings = newSettings
        StorageUtils.saveSubtitleSettings(context, newSettings)
    }

    fun updatePlayer(newSettings: VideoPlayerSettings) {
        playerSettings = newSettings
        StorageUtils.saveVideoPlayerSettings(context, newSettings)
    }

    SettingsSubPage(
        title = stringResource(R.string.video_player_settings),
        navController = navController,
        icon = Icons.Default.Subtitles
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ---------- Seek time ----------
            SettingsSectionTitle(stringResource(R.string.player_seek_section))
            SettingsStepperRow(
                label = stringResource(R.string.seek_time, playerSettings.seekTimeSeconds),
                valueText = "${playerSettings.seekTimeSeconds}s",
                canDecrease = playerSettings.seekTimeSeconds > SEEK_MIN,
                canIncrease = playerSettings.seekTimeSeconds < SEEK_MAX,
                onDecrease = {
                    updatePlayer(
                        playerSettings.copy(
                            seekTimeSeconds = (playerSettings.seekTimeSeconds - SEEK_STEP)
                                .coerceIn(SEEK_MIN, SEEK_MAX)
                        )
                    )
                },
                onIncrease = {
                    updatePlayer(
                        playerSettings.copy(
                            seekTimeSeconds = (playerSettings.seekTimeSeconds + SEEK_STEP)
                                .coerceIn(SEEK_MIN, SEEK_MAX)
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ---------- Subtitle size ----------
            SettingsSectionTitle(stringResource(R.string.subtitle_settings))
            SettingsStepperRow(
                label = stringResource(R.string.subtitle_text_size, subtitleSettings.textSize.toInt()),
                valueText = "${subtitleSettings.textSize.toInt()} sp",
                canDecrease = subtitleSettings.textSize > SUBTITLE_TEXT_SIZE_MIN,
                canIncrease = subtitleSettings.textSize < SUBTITLE_TEXT_SIZE_MAX,
                onDecrease = {
                    updateSubtitle(
                        subtitleSettings.copy(
                            textSize = (subtitleSettings.textSize - SUBTITLE_TEXT_SIZE_STEP)
                                .coerceIn(SUBTITLE_TEXT_SIZE_MIN, SUBTITLE_TEXT_SIZE_MAX)
                        )
                    )
                },
                onIncrease = {
                    updateSubtitle(
                        subtitleSettings.copy(
                            textSize = (subtitleSettings.textSize + SUBTITLE_TEXT_SIZE_STEP)
                                .coerceIn(SUBTITLE_TEXT_SIZE_MIN, SUBTITLE_TEXT_SIZE_MAX)
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ---------- Subtitle text colour ----------
            Text(
                text = stringResource(R.string.subtitle_text_color),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Color.Yellow, Color.White, Color.Black,
                    Color.Red, Color.Blue, Color.Green, Color.Cyan
                ).forEach { color ->
                    SettingsColorSwatch(
                        color = color,
                        isSelected = subtitleSettings.textColor == color.toArgb(),
                        onSelect = { updateSubtitle(subtitleSettings.copy(textColor = color.toArgb())) },
                        swatchSize = 40
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---------- Subtitle outline / background colour ----------
            Text(
                text = stringResource(R.string.subtitle_background_color),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Transparent (no outline)
                SettingsColorSwatch(
                    color = Color.Transparent,
                    isSelected = subtitleSettings.borderColor == Color.Transparent.toArgb(),
                    onSelect = { updateSubtitle(subtitleSettings.copy(borderColor = Color.Transparent.toArgb())) },
                    label = stringResource(R.string.color_empty),
                    swatchSize = 40,
                    showEmptyIndicator = true
                )
                // Glass (semi-transparent black)
                val glass = Color.Black.copy(alpha = 0.5f)
                SettingsColorSwatch(
                    color = glass,
                    isSelected = subtitleSettings.borderColor == glass.toArgb(),
                    onSelect = { updateSubtitle(subtitleSettings.copy(borderColor = glass.toArgb())) },
                    label = stringResource(R.string.color_glass),
                    swatchSize = 40
                )
                listOf(Color.Black, Color.White, Color.Red, Color.Blue).forEach { color ->
                    SettingsColorSwatch(
                        color = color,
                        isSelected = subtitleSettings.borderColor == color.toArgb(),
                        onSelect = { updateSubtitle(subtitleSettings.copy(borderColor = color.toArgb())) },
                        swatchSize = 40
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---------- Live preview ----------
            SettingsSectionTitle(stringResource(R.string.subtitle_preview))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF101010))
                    .padding(vertical = 24.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.subtitle_preview_text),
                    color = Color(subtitleSettings.textColor),
                    fontSize = subtitleSettings.textSize.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(subtitleSettings.borderColor))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.subtitle_size_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
