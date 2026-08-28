package com.pira.darknama.ui.country

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pira.darknama.data.model.FilterType
import com.pira.darknama.data.model.Poster
import com.pira.darknama.data.repository.CountryPostersRepository
import com.pira.darknama.data.repository.CountryRepository
import com.pira.darknama.utils.LanguageUtils
import kotlinx.coroutines.launch

class CountryViewModel : ViewModel() {
    private val postersRepository = CountryPostersRepository()
    private val countryRepository = CountryRepository()
    
    var posters by mutableStateOf<List<Poster>>(emptyList())
        private set
    
    var countryName by mutableStateOf<String>("")
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var isLoadingMore by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var currentPage by mutableStateOf(0)
        private set
    
    var canLoadMore by mutableStateOf(true)
        private set
    
    var countryId by mutableStateOf<Int?>(null)
        private set
    
    var selectedFilterType by mutableStateOf(FilterType.DEFAULT)
        private set
    
    fun setCountryId(id: Int) {
        if (countryId != id) {
            countryId = id
            loadCountryName()
            refresh()
        }
    }
    
    fun selectFilterType(filterType: FilterType) {
        if (selectedFilterType != filterType) {
            selectedFilterType = filterType
            refresh()
        }
    }
    
    private fun loadCountryName() {
        viewModelScope.launch {
            try {
                val countries = countryRepository.getAllCountries()
                val country = countries.find { it.id == countryId }
                countryName = country?.title ?: "Country"
            } catch (e: Exception) {
                countryName = "Country"
            }
        }
    }
    
    fun loadPosters(page: Int = 0) {
        val currentCountryId = countryId ?: return
        
        viewModelScope.launch {
            try {
                if (page == 0) {
                    isLoading = true
                } else {
                    isLoadingMore = true
                }
                errorMessage = null
                
                val newPosters = postersRepository.getPostersByCountry(currentCountryId, page, selectedFilterType)
                
                // Filter out posters with Farsi titles
                val filteredPosters = newPosters.filter { poster ->
                    LanguageUtils.shouldDisplayTitle(poster.title)
                }
                
                // If we get fewer posters than expected, we've reached the end
                canLoadMore = filteredPosters.isNotEmpty()
                
                if (page == 0) {
                    posters = filteredPosters
                } else {
                    posters = posters + filteredPosters
                }
                
                currentPage = page
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }
    
    fun loadMorePosters() {
        if (!isLoading && !isLoadingMore && canLoadMore) {
            loadPosters(currentPage + 1)
        }
    }
    
    fun retry() {
        loadPosters(currentPage)
    }
    
    fun refresh() {
        loadPosters(0)
    }
}