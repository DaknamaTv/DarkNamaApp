package com.dark.darknama.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dark.darknama.R
import com.dark.darknama.components.SettingsOptionRow
import com.dark.darknama.components.SettingsSectionTitle
import com.dark.darknama.components.SettingsSubPage
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.data.model.FontType
import com.dark.darknama.ui.theme.FontManager
import com.dark.darknama.utils.StorageUtils

/**
 * Full-page Font settings with a live preview of the selected font.
 */
@Composable
fun FontSettingsScreen(
    navController: NavController?,
    onFontSettingsChanged: (FontSettings) -> Unit = {}
) {
    val context = LocalContext.current
    var fontSettings by remember { mutableStateOf(StorageUtils.loadFontSettings(context)) }

    fun update(newSettings: FontSettings) {
        fontSettings = newSettings
        StorageUtils.saveFontSettings(context, newSettings)
        onFontSettingsChanged(newSettings)
    }

    SettingsSubPage(
        title = stringResource(R.string.font_settings),
        navController = navController,
        icon = Icons.Default.TextFields
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SettingsSectionTitle(stringResource(R.string.select_font))

            SettingsOptionRow(
                label = stringResource(R.string.font_system_default),
                isSelected = fontSettings.fontType == FontType.DEFAULT,
                onSelect = { update(fontSettings.copy(fontType = FontType.DEFAULT)) }
            )

            SettingsOptionRow(
                label = stringResource(R.string.font_vazirmatn),
                description = stringResource(R.string.font_vazirmatn_desc),
                isSelected = fontSettings.fontType == FontType.VAZIRMATN,
                onSelect = { update(fontSettings.copy(fontType = FontType.VAZIRMATN)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ---------- Live preview ----------
            SettingsSectionTitle(stringResource(R.string.font_preview))

            val previewFamily = FontManager.loadFontFamily(context, fontSettings.fontType)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.font_preview_fa),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = previewFamily
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.font_preview_en),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = previewFamily
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "0 1 2 3 4 5 6 7 8 9  ۰ ۱ ۲ ۳ ۴ ۵ ۶ ۷ ۸ ۹",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = previewFamily
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
