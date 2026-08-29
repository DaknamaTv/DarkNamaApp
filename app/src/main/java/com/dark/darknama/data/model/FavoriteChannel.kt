package com.dark.darknama.data.model

import kotlinx.serialization.Serializable

/**
 * A live TV channel saved by the user as a favorite.
 *
 * Stored on disk (favorite_channels.json) so the channel can be played
 * again from the "Favorites" chip in the Live TV screen even if the
 * original playlist is not currently loaded.
 */
@Serializable
data class FavoriteChannel(
    val id: String,
    val name: String,
    val logo: String = "",
    val group: String = "",
    val url: String,
    val userAgent: String? = null,
    val referer: String? = null
) {
    /** Stable key used to compare channels (playlist ids are not always unique). */
    val key: String get() = "$id|$url"

    fun toTvChannel(): TvChannel = TvChannel(
        id = id,
        name = name,
        logo = logo,
        group = group,
        url = url,
        userAgent = userAgent,
        referer = referer
    )

    companion object {
        fun from(channel: TvChannel): FavoriteChannel = FavoriteChannel(
            id = channel.id,
            name = channel.name,
            logo = channel.logo,
            group = channel.group,
            url = channel.url,
            userAgent = channel.userAgent,
            referer = channel.referer
        )
    }
}

/** Stable key for a [TvChannel] matching [FavoriteChannel.key]. */
val TvChannel.favoriteKey: String get() = "$id|$url"
