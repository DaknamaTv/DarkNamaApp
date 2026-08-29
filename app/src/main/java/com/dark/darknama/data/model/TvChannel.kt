package com.dark.darknama.data.model

/**
 * Represents a live TV channel parsed from an M3U playlist (iptv-org).
 *
 * @param id        Unique id of the channel (tvg-id or generated).
 * @param name      Display name of the channel.
 * @param logo      Channel logo URL (may be empty).
 * @param group     Group title from the playlist (country name or category list).
 * @param url       Stream URL (usually HLS .m3u8).
 * @param userAgent Optional custom HTTP User-Agent required by the stream.
 * @param referer   Optional HTTP Referer required by the stream.
 */
data class TvChannel(
    val id: String,
    val name: String,
    val logo: String,
    val group: String,
    val url: String,
    val userAgent: String? = null,
    val referer: String? = null
)

/**
 * Browsing modes for the Live TV section.
 */
enum class TvBrowseMode {
    PERSIAN,   // Default: Persian channels (languages/fas.m3u)
    COUNTRY,   // Browse by country (index.country.m3u)
    CATEGORY,  // Browse by category (index.m3u)
    FAVORITES  // User's starred channels (stored locally)
}
