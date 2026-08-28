package com.dark.darknama.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.darknama.data.model.Country
import com.dark.darknama.data.model.Poster
import com.dark.darknama.data.repository.CountryRepository
import com.dark.darknama.data.repository.SearchRepository
import com.dark.darknama.utils.LanguageUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repository = SearchRepository()
    private val countryRepository = CountryRepository()
    
    var searchResults by mutableStateOf<List<Poster>>(emptyList())
        private set
    
    var countries by mutableStateOf<List<Country>>(emptyList())
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var isCountriesLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var searchQuery by mutableStateOf("")
        private set
    
    // New state to track if a search has been performed
    var hasSearched by mutableStateOf(false)
        private set
    
    private var searchJob: Job? = null
    
    init {
        loadCountries()
    }
    
    fun updateSearchQuery(query: String) {
        searchQuery = query
        // Don't auto-search anymore - only search when explicitly triggered
        // Reset search state when query changes
        if (!hasSearched) {
            searchResults = emptyList()
        }
    }
    
    // New function to explicitly trigger search
    fun triggerSearch() {
        if (searchQuery.isNotEmpty()) {
            hasSearched = true
            search(searchQuery)
        }
    }
    
    private fun search(query: String) {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                val result = repository.search(query)
                
                // Filter out posters with Farsi titles
                val filteredPosters = result.posters.filter { poster ->
                    LanguageUtils.shouldDisplayTitle(poster.title)
                }
                
                searchResults = filteredPosters
            } catch (e: Exception) {
                errorMessage = e.message
                searchResults = emptyList()
            } finally {
                isLoading = false
            }
        }
    }
    
    private fun loadCountries() {
        viewModelScope.launch {
            try {
                isCountriesLoading = true
                countries = countryRepository.getAllCountries()
            } catch (e: Exception) {
                // Handle error silently for countries
                countries = emptyList()
            } finally {
                isCountriesLoading = false
            }
        }
    }
    
    fun clearSearch() {
        searchQuery = ""
        searchResults = emptyList()
        errorMessage = null
        hasSearched = false
    }
}