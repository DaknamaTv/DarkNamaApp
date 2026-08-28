package com.dark.darknama.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.data.model.FontType
import com.dark.darknama.utils.StorageUtils

object FontManager {
    private var vazirmatnFontFamily: FontFamily? = null
    
    fun loadFontFamily(context: Context, fontType: FontType): FontFamily? {
        return when (fontType) {
            FontType.DEFAULT -> null // Use system default
            FontType.VAZIRMATN -> {
                if (vazirmatnFontFamily == null) {
                    // Create a custom font family using resource identifiers
                    vazirmatnFontFamily = FontFamily(
                        androidx.compose.ui.text.font.Font(
                            com.dark.darknama.R.font.vazirmatn_regular,
                            FontWeight.Normal,
                            FontStyle.Normal
                        ),
                        androidx.compose.ui.text.font.Font(
                            com.dark.darknama.R.font.vazirmatn_bold,
                            FontWeight.Bold,
                            FontStyle.Normal
                        ),
                        androidx.compose.ui.text.font.Font(
                            com.dark.darknama.R.font.vazirmatn_light,
                            FontWeight.Light,
                            FontStyle.Normal
                        )
                    )
                }
                vazirmatnFontFamily
            }
        }
    }
}

@Composable
fun rememberFontSettings(): FontSettings {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember { StorageUtils.loadFontSettings(context) }
}