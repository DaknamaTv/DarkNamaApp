package com.dark.darknama.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FontSettings(
    val fontType: FontType = FontType.DEFAULT
) {
    companion object {
        // Default font settings
        val DEFAULT = FontSettings()
    }
}

enum class FontType {
    DEFAULT, // System default font
    VAZIRMATN // Custom Vazirmatn font
}