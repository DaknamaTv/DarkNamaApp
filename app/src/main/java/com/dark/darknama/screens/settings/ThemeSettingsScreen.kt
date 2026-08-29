package com.dark.darknama.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.dark.darknama.R
import com.dark.darknama.components.SettingsColorSwatch
import com.dark.darknama.components.SettingsOptionRow
import com.dark.darknama.components.SettingsSectionTitle
import com.dark.darknama.components.SettingsSubPage
import com.dark.darknama.ui.theme.ThemeManager
import com.dark.darknama.ui.theme.ThemeMode
import com.dark.darknama.ui.theme.ThemeSettings
import com.dark.darknama.ui.theme.colorOptions
import com.dark.darknama.ui.theme.defaultPrimaryColor

/**
 * Full-page Theme settings (theme mode + primary colour).
 *
 * Replaces the previous inline drop-down card so that it can be
 * navigated comfortably with an Android TV remote.
 */
@Composable
fun ThemeSettingsScreen(
    navController: NavController?,
    onThemeSettingsChanged: (ThemeSettings) -> Unit = {}
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    var themeSettings by remember { mutableStateOf(themeManager.loadThemeSettings()) }

    val colorRows = remember { colorOptions.chunked(4) }

    fun update(newSettings: ThemeSettings) {
        themeSettings = newSettings
        themeManager.saveThemeSettings(newSettings)
        onThemeSettingsChanged(newSettings)
    }

    SettingsSubPage(
        title = stringResource(R.string.theme_settings),
        navController = navController,
        icon = Icons.Default.FormatColorFill
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // ---------- Theme mode ----------
            item {
                SettingsSectionTitle(stringResource(R.string.theme_mode))
            }
            item {
                SettingsOptionRow(
                    label = stringResource(R.string.theme_light),
                    isSelected = themeSettings.themeMode == ThemeMode.LIGHT,
                    onSelect = { update(themeSettings.copy(themeMode = ThemeMode.LIGHT)) }
                )
            }
            item {
                SettingsOptionRow(
                    label = stringResource(R.string.theme_dark),
                    isSelected = themeSettings.themeMode == ThemeMode.DARK,
                    onSelect = { update(themeSettings.copy(themeMode = ThemeMode.DARK)) }
                )
            }
            item {
                SettingsOptionRow(
                    label = stringResource(R.string.theme_system),
                    isSelected = themeSettings.themeMode == ThemeMode.SYSTEM,
                    onSelect = { update(themeSettings.copy(themeMode = ThemeMode.SYSTEM)) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // ---------- Primary colour ----------
            item {
                SettingsSectionTitle(stringResource(R.string.primary_color))
            }

            // Default colour first so it is easy to get back to it.
            item {
                SettingsOptionRow(
                    label = stringResource(R.string.theme_use_default_color),
                    isSelected = themeSettings.primaryColor == defaultPrimaryColor,
                    onSelect = { update(themeSettings.copy(primaryColor = defaultPrimaryColor)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Colour swatches, 4 per row
            items(count = colorRows.size) { rowIndex ->
                val rowColors = colorRows[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowColors.forEach { color ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            SettingsColorSwatch(
                                color = color,
                                isSelected = themeSettings.primaryColor == color,
                                onSelect = { update(themeSettings.copy(primaryColor = color)) }
                            )
                        }
                    }
                    repeat(4 - rowColors.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.theme_color_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
