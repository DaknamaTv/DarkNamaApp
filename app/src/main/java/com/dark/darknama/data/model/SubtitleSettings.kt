package com.dark.darknama.data.model

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color as ComposeColor
import com.dark.darknama.utils.DeviceUtils
import kotlinx.serialization.Serializable

@Serializable
data class SubtitleSettings(
    val textColor: Int = Color.YELLOW,
    val borderColor: Int = Color.argb(128, 0, 0, 0), // Glass background (50% transparent black)
    val textSize: Float = 17f
) {
    companion object {
        // Default subtitle settings with transparent background
        val DEFAULT = SubtitleSettings()
        
        // Get default settings based on device type
        fun getDefaultSettings(context: Context): SubtitleSettings {
            val textSize = if (DeviceUtils.isTv(context)) 25f else 17f
            return SubtitleSettings(textSize = textSize)
        }
        
        // Convert Android Color int to Compose Color
        fun Int.toComposeColor(): ComposeColor = ComposeColor(this)
        
        // Convert Compose Color to Android Color int
        fun ComposeColor.toInt(): Int = this.toArgb()
    }
}