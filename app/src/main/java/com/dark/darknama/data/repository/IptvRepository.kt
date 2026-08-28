package com.dark.darknama.data.repository

import com.dark.darknama.data.model.TvChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Repository for fetching and parsing live TV channels from the
 * iptv-org public playlists (https://github.com/iptv-org/iptv).
 */
class IptvRepository {

    companion object {
        // Default playlist: Persian language channels
        const val PERSIAN_PLAYLIST_URL = "https://iptv-org.github.io/iptv/languages/fas.m3u"

        // Grouped by country
        const val COUNTRY_PLAYLIST_URL = "https://iptv-org.github.io/iptv/index.country.m3u"

        // Grouped by category
        const val CATEGORY_PLAYLIST_URL = "https://iptv-org.github.io/iptv/index.m3u"

        // Simple in-memory cache so switching tabs doesn't re-download playlists
        private val cache = mutableMapOf<String, List<TvChannel>>()

        /**
         * Emoji flag for a country name used by iptv-org group-titles.
         * Falls back to a generic globe if the country is unknown.
         */
        fun flagFor(country: String): String = COUNTRY_FLAGS[country] ?: "\uD83C\uDF10" // 🌐

        private val COUNTRY_FLAGS: Map<String, String> = mapOf(
            "Afghanistan" to "🇦🇫", "Albania" to "🇦🇱", "Algeria" to "🇩🇿", "Andorra" to "🇦🇩",
            "Angola" to "🇦🇴", "Antigua and Barbuda" to "🇦🇬", "Argentina" to "🇦🇷", "Armenia" to "🇦🇲",
            "Aruba" to "🇦🇼", "Australia" to "🇦🇺", "Austria" to "🇦🇹", "Azerbaijan" to "🇦🇿",
            "Bahamas" to "🇧🇸", "Bahrain" to "🇧🇭", "Bangladesh" to "🇧🇩", "Barbados" to "🇧🇧",
            "Belarus" to "🇧🇾", "Belgium" to "🇧🇪", "Belize" to "🇧🇿", "Benin" to "🇧🇯",
            "Bolivia" to "🇧🇴", "Bonaire" to "🇧🇶", "Bosnia and Herzegovina" to "🇧🇦", "Brazil" to "🇧🇷",
            "British Virgin Islands" to "🇻🇬", "Brunei" to "🇧🇳", "Bulgaria" to "🇧🇬", "Burkina Faso" to "🇧🇫",
            "Cambodia" to "🇰🇭", "Cameroon" to "🇨🇲", "Canada" to "🇨🇦", "Cape Verde" to "🇨🇻",
            "Chad" to "🇹🇩", "Chile" to "🇨🇱", "China" to "🇨🇳", "Colombia" to "🇨🇴",
            "Comoros" to "🇰🇲", "Costa Rica" to "🇨🇷", "Croatia" to "🇭🇷", "Cuba" to "🇨🇺",
            "Curacao" to "🇨🇼", "Cyprus" to "🇨🇾", "Czech Republic" to "🇨🇿",
            "Democratic Republic of the Congo" to "🇨🇩", "Denmark" to "🇩🇰", "Dominica" to "🇩🇲",
            "Dominican Republic" to "🇩🇴", "Ecuador" to "🇪🇨", "Egypt" to "🇪🇬", "El Salvador" to "🇸🇻",
            "Equatorial Guinea" to "🇬🇶", "Eritrea" to "🇪🇷", "Estonia" to "🇪🇪", "Ethiopia" to "🇪🇹",
            "Fiji" to "🇫🇯", "Finland" to "🇫🇮", "France" to "🇫🇷", "French Guiana" to "🇬🇫",
            "French Polynesia" to "🇵🇫", "Gabon" to "🇬🇦", "Gambia" to "🇬🇲", "Georgia" to "🇬🇪",
            "Germany" to "🇩🇪", "Ghana" to "🇬🇭", "Greece" to "🇬🇷", "Greenland" to "🇬🇱",
            "Grenada" to "🇬🇩", "Guadeloupe" to "🇬🇵", "Guam" to "🇬🇺", "Guatemala" to "🇬🇹",
            "Guernsey" to "🇬🇬", "Guinea" to "🇬🇳", "Guinea-Bissau" to "🇬🇼", "Guyana" to "🇬🇾",
            "Haiti" to "🇭🇹", "Honduras" to "🇭🇳", "Hong Kong" to "🇭🇰", "Hungary" to "🇭🇺",
            "Iceland" to "🇮🇸", "India" to "🇮🇳", "Indonesia" to "🇮🇩", "International" to "🌍",
            "Iran" to "🇮🇷", "Iraq" to "🇮🇶", "Ireland" to "🇮🇪", "Israel" to "🇮🇱",
            "Italy" to "🇮🇹", "Ivory Coast" to "🇨🇮", "Jamaica" to "🇯🇲", "Japan" to "🇯🇵",
            "Jersey" to "🇯🇪", "Jordan" to "🇯🇴", "Kazakhstan" to "🇰🇿", "Kenya" to "🇰🇪",
            "Kiribati" to "🇰🇮", "Kosovo" to "🇽🇰", "Kuwait" to "🇰🇼", "Kyrgyzstan" to "🇰🇬",
            "Laos" to "🇱🇦", "Latvia" to "🇱🇻", "Lebanon" to "🇱🇧", "Lesotho" to "🇱🇸",
            "Liberia" to "🇱🇷", "Libya" to "🇱🇾", "Liechtenstein" to "🇱🇮", "Lithuania" to "🇱🇹",
            "Luxembourg" to "🇱🇺", "Macao" to "🇲🇴", "Madagascar" to "🇲🇬", "Malawi" to "🇲🇼",
            "Malaysia" to "🇲🇾", "Maldives" to "🇲🇻", "Mali" to "🇲🇱", "Malta" to "🇲🇹",
            "Martinique" to "🇲🇶", "Mauritania" to "🇲🇷", "Mauritius" to "🇲🇺", "Mexico" to "🇲🇽",
            "Moldova" to "🇲🇩", "Monaco" to "🇲🇨", "Mongolia" to "🇲🇳", "Montenegro" to "🇲🇪",
            "Montserrat" to "🇲🇸", "Morocco" to "🇲🇦", "Mozambique" to "🇲🇿", "Myanmar" to "🇲🇲",
            "Namibia" to "🇳🇦", "Nepal" to "🇳🇵", "Netherlands" to "🇳🇱", "New Zealand" to "🇳🇿",
            "Nicaragua" to "🇳🇮", "Niger" to "🇳🇪", "Nigeria" to "🇳🇬", "North Korea" to "🇰🇵",
            "North Macedonia" to "🇲🇰", "Norway" to "🇳🇴", "Oman" to "🇴🇲", "Pakistan" to "🇵🇰",
            "Palestine" to "🇵🇸", "Panama" to "🇵🇦", "Papua New Guinea" to "🇵🇬", "Paraguay" to "🇵🇾",
            "Peru" to "🇵🇪", "Philippines" to "🇵🇭", "Poland" to "🇵🇱", "Portugal" to "🇵🇹",
            "Puerto Rico" to "🇵🇷", "Qatar" to "🇶🇦", "Republic of the Congo" to "🇨🇬",
            "Reunion" to "🇷🇪", "Romania" to "🇷🇴", "Russia" to "🇷🇺", "Rwanda" to "🇷🇼",
            "Saint Kitts and Nevis" to "🇰🇳", "Saint Lucia" to "🇱🇨",
            "Saint Vincent and the Grenadines" to "🇻🇨", "Samoa" to "🇼🇸", "San Marino" to "🇸🇲",
            "Saudi Arabia" to "🇸🇦", "Senegal" to "🇸🇳", "Serbia" to "🇷🇸", "Seychelles" to "🇸🇨",
            "Sierra Leone" to "🇸🇱", "Singapore" to "🇸🇬", "Sint Maarten" to "🇸🇽",
            "Slovakia" to "🇸🇰", "Slovenia" to "🇸🇮", "Somalia" to "🇸🇴", "South Africa" to "🇿🇦",
            "South Korea" to "🇰🇷", "South Sudan" to "🇸🇸", "Spain" to "🇪🇸", "Sri Lanka" to "🇱🇰",
            "Sudan" to "🇸🇩", "Suriname" to "🇸🇷", "Sweden" to "🇸🇪", "Switzerland" to "🇨🇭",
            "Syria" to "🇸🇾", "Taiwan" to "🇹🇼", "Tajikistan" to "🇹🇯", "Tanzania" to "🇹🇿",
            "Thailand" to "🇹🇭", "Togo" to "🇹🇬", "Tonga" to "🇹🇴", "Trinidad and Tobago" to "🇹🇹",
            "Tunisia" to "🇹🇳", "Turkiye" to "🇹🇷", "Turkey" to "🇹🇷", "Turkmenistan" to "🇹🇲",
            "Uganda" to "🇺🇬", "Ukraine" to "🇺🇦", "Undefined" to "🌐",
            "United Arab Emirates" to "🇦🇪", "United Kingdom" to "🇬🇧", "United States" to "🇺🇸",
            "Uruguay" to "🇺🇾", "Uzbekistan" to "🇺🇿", "Vanuatu" to "🇻🇺", "Vatican City" to "🇻🇦",
            "Venezuela" to "🇻🇪", "Vietnam" to "🇻🇳", "Western Sahara" to "🇪🇭", "Yemen" to "🇾🇪",
            "Zambia" to "🇿🇲", "Zimbabwe" to "🇿🇼"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Fetch and parse an M3U playlist into a list of [TvChannel].
     * Results are cached in memory for the lifetime of the process.
     */
    suspend fun getChannels(playlistUrl: String, forceRefresh: Boolean = false): List<TvChannel> {
        if (!forceRefresh) {
            cache[playlistUrl]?.let { return it }
        }
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(playlistUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) DarkNama/2.0")
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Server returned error: ${response.code}")
                }
                response.body?.string() ?: throw Exception("Empty playlist response")
            }
            val channels = parseM3u(body)
            if (channels.isNotEmpty()) {
                cache[playlistUrl] = channels
            }
            channels
        }
    }

    /**
     * Parses an extended M3U playlist. Supports:
     *  - #EXTINF attributes: tvg-id, tvg-logo, group-title, http-user-agent, http-referrer
     *  - #EXTVLCOPT:http-user-agent / http-referrer directives
     */
    fun parseM3u(content: String): List<TvChannel> {
        val channels = ArrayList<TvChannel>(4096)
        var name = ""
        var logo = ""
        var group = ""
        var tvgId = ""
        var userAgent: String? = null
        var referer: String? = null
        var pendingInfo = false
        var index = 0

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    tvgId = extractAttr(line, "tvg-id")
                    logo = extractAttr(line, "tvg-logo")
                    group = extractAttr(line, "group-title")
                    userAgent = extractAttr(line, "http-user-agent").ifEmpty { null }
                    referer = extractAttr(line, "http-referrer").ifEmpty { null }
                    name = line.substringAfterLast(',').trim()
                    pendingInfo = true
                }
                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val opt = line.removePrefix("#EXTVLCOPT:")
                    when {
                        opt.startsWith("http-user-agent=", ignoreCase = true) ->
                            userAgent = opt.substringAfter('=').trim().ifEmpty { null }
                        opt.startsWith("http-referrer=", ignoreCase = true) ->
                            referer = opt.substringAfter('=').trim().ifEmpty { null }
                    }
                }
                line.startsWith("#") -> {
                    // Other directives (#EXTM3U, #EXTGRP etc.) — ignore
                }
                else -> {
                    // Stream URL line
                    if (pendingInfo && (line.startsWith("http://") || line.startsWith("https://"))) {
                        channels.add(
                            TvChannel(
                                id = tvgId.ifEmpty { "ch_$index" },
                                name = name.ifEmpty { "Channel ${index + 1}" },
                                logo = logo,
                                group = group,
                                url = line,
                                userAgent = userAgent,
                                referer = referer
                            )
                        )
                        index++
                    }
                    // Reset state for the next entry
                    name = ""
                    logo = ""
                    group = ""
                    tvgId = ""
                    userAgent = null
                    referer = null
                    pendingInfo = false
                }
            }
        }
        return channels
    }

    private fun extractAttr(line: String, attr: String): String {
        val key = "$attr=\""
        val start = line.indexOf(key)
        if (start == -1) return ""
        val valueStart = start + key.length
        val end = line.indexOf('"', valueStart)
        if (end == -1) return ""
        return line.substring(valueStart, end).trim()
    }
}
