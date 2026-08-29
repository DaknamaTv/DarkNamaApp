package com.dark.darknama.ui.tv

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dark.darknama.data.model.FavoriteChannel
import com.dark.darknama.data.model.TvBrowseMode
import com.dark.darknama.data.model.TvChannel
import com.dark.darknama.data.model.favoriteKey
import com.dark.darknama.data.repository.IptvRepository
import com.dark.darknama.utils.StorageUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ViewModel for the Live TV section.
 *
 * Browse modes:
 *  - PERSIAN  (default): Persian language channels (languages/fas.m3u)
 *  - COUNTRY : channels of a selected country (index.country.m3u) — with flags
 *  - CATEGORY: channels of a selected category (index.m3u)
 *  - FAVORITES: channels the user starred (stored on device)
 */
class LiveTvViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IptvRepository()
    private var loadJob: Job? = null

    var browseMode by mutableStateOf(TvBrowseMode.PERSIAN)
        private set

    /** All channels of the currently loaded playlist */
    var allChannels by mutableStateOf<List<TvChannel>>(emptyList())
        private set

    /** Channels currently shown (after group + search filters) */
    var channels by mutableStateOf<List<TvChannel>>(emptyList())
        private set

    /** Selected country (COUNTRY mode) or category (CATEGORY mode); null = none */
    var selectedGroup by mutableStateOf<String?>(null)
        private set

    /** Category chips inside the Persian (default) playlist */
    var persianCategories by mutableStateOf<List<String>>(emptyList())
        private set

    /** Selected Persian category chip; null = All */
    var selectedPersianCategory by mutableStateOf<String?>(null)
        private set

    /** List of all countries for the country picker dialog */
    var countryList by mutableStateOf<List<String>>(emptyList())
        private set

    /** List of all categories for the category picker dialog */
    var categoryList by mutableStateOf<List<String>>(emptyList())
        private set

    var isCountryListLoading by mutableStateOf(false)
        private set

    var isCategoryListLoading by mutableStateOf(false)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Keys (id|url) of all favorite channels - used to render the star state */
    var favoriteKeys by mutableStateOf<Set<String>>(emptySet())
        private set

    /** The user's favorite channels (newest first) */
    var favoriteChannels by mutableStateOf<List<FavoriteChannel>>(emptyList())
        private set

    init {
        reloadFavorites()
        loadChannels()
    }

    // ------------------------------------------------------------------
    // Favorites
    // ------------------------------------------------------------------

    /** Re-reads favorites from disk into memory */
    fun reloadFavorites() {
        val stored = StorageUtils.loadFavoriteChannels(getApplication())
        favoriteChannels = stored
        favoriteKeys = stored.map { it.key }.toSet()
    }

    fun isFavorite(channel: TvChannel): Boolean = favoriteKeys.contains(channel.favoriteKey)

    /** Adds/removes a channel from the favorites list */
    fun toggleFavorite(channel: TvChannel) {
        StorageUtils.toggleFavoriteChannel(getApplication(), FavoriteChannel.from(channel))
        reloadFavorites()
        // Keep the favorites view in sync while the user is browsing it
        if (browseMode == TvBrowseMode.FAVORITES) {
            allChannels = favoriteChannels.map { it.toTvChannel() }
            applyFilters()
        }
    }

    /** Show only the favorite channels */
    fun selectFavorites() {
        loadJob?.cancel()
        browseMode = TvBrowseMode.FAVORITES
        selectedGroup = null
        selectedPersianCategory = null
        errorMessage = null
        isLoading = false
        reloadFavorites()
        allChannels = favoriteChannels.map { it.toTvChannel() }
        applyFilters()
    }

    // ------------------------------------------------------------------
    // Browse modes
    // ------------------------------------------------------------------

    /** Reset to the default Persian channel list */
    fun selectPersianDefault() {
        if (browseMode == TvBrowseMode.PERSIAN && allChannels.isNotEmpty()) {
            selectedPersianCategory = null
            applyFilters()
            return
        }
        browseMode = TvBrowseMode.PERSIAN
        selectedGroup = null
        selectedPersianCategory = null
        searchQuery = ""
        loadChannels()
    }

    /** Select a country from the country picker */
    fun selectCountry(country: String) {
        browseMode = TvBrowseMode.COUNTRY
        selectedGroup = country
        selectedPersianCategory = null
        loadChannels()
    }

    /** Select a category from the category picker */
    fun selectCategory(category: String) {
        browseMode = TvBrowseMode.CATEGORY
        selectedGroup = category
        selectedPersianCategory = null
        loadChannels()
    }

    /** Select a category chip within the Persian playlist (null = All) */
    fun selectPersianCategory(category: String?) {
        selectedPersianCategory = category
        applyFilters()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        applyFilters()
    }

    fun retry() {
        if (browseMode == TvBrowseMode.FAVORITES) {
            selectFavorites()
        } else {
            loadChannels(forceRefresh = true)
        }
    }

    /** Loads country names for the picker (cached playlist reused for channels) */
    fun ensureCountryListLoaded() {
        if (countryList.isNotEmpty() || isCountryListLoading) return
        viewModelScope.launch {
            isCountryListLoading = true
            try {
                val list = repository.getChannels(IptvRepository.COUNTRY_PLAYLIST_URL)
                countryList = list.asSequence()
                    .map { it.group.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()
                    .toList()
            } catch (_: Exception) {
                // Picker will show an error/retry state via empty list
            } finally {
                isCountryListLoading = false
            }
        }
    }

    /** Loads category names for the picker (cached playlist reused for channels) */
    fun ensureCategoryListLoaded() {
        if (categoryList.isNotEmpty() || isCategoryListLoading) return
        viewModelScope.launch {
            isCategoryListLoading = true
            try {
                val list = repository.getChannels(IptvRepository.CATEGORY_PLAYLIST_URL)
                categoryList = list.asSequence()
                    .flatMap { it.group.split(';').asSequence() }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()
                    .toList()
            } catch (_: Exception) {
                // Picker will show an error/retry state via empty list
            } finally {
                isCategoryListLoading = false
            }
        }
    }

    fun loadChannels(forceRefresh: Boolean = false) {
        if (browseMode == TvBrowseMode.FAVORITES) {
            selectFavorites()
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val url = when (browseMode) {
                    TvBrowseMode.PERSIAN -> IptvRepository.PERSIAN_PLAYLIST_URL
                    TvBrowseMode.COUNTRY -> IptvRepository.COUNTRY_PLAYLIST_URL
                    TvBrowseMode.CATEGORY -> IptvRepository.CATEGORY_PLAYLIST_URL
                    TvBrowseMode.FAVORITES -> IptvRepository.PERSIAN_PLAYLIST_URL // unreachable
                }
                val result = repository.getChannels(url, forceRefresh)
                allChannels = result
                if (browseMode == TvBrowseMode.PERSIAN) {
                    persianCategories = result.asSequence()
                        .flatMap { it.group.split(';').asSequence() }
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .sorted()
                        .toList()
                }
                applyFilters()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load channels"
                allChannels = emptyList()
                channels = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    private fun applyFilters() {
        val query = searchQuery.trim()
        var filtered = allChannels

        when (browseMode) {
            TvBrowseMode.PERSIAN -> {
                val category = selectedPersianCategory
                if (category != null) {
                    filtered = filtered.filter { channel ->
                        channel.group.split(';').any { it.trim() == category }
                    }
                }
            }
            TvBrowseMode.COUNTRY -> {
                val country = selectedGroup
                if (country != null) {
                    filtered = filtered.filter { it.group == country }
                }
            }
            TvBrowseMode.CATEGORY -> {
                val category = selectedGroup
                if (category != null) {
                    filtered = filtered.filter { channel ->
                        channel.group.split(';').any { it.trim() == category }
                    }
                }
            }
            TvBrowseMode.FAVORITES -> {
                // Favorites are already the full working set
            }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }
        channels = filtered
    }
}
