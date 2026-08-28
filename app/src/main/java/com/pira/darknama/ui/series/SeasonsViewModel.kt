package com.pira.darknama.ui.series

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pira.darknama.data.model.Season
import com.pira.darknama.data.repository.SeasonsRepository
import kotlinx.coroutines.launch

class SeasonsViewModel : ViewModel() {
    private val repository = SeasonsRepository()
    
    var seasons by mutableStateOf<List<Season>>(emptyList())
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    fun loadSeasons(seriesId: Int) {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                seasons = repository.getSeasons(seriesId)
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }
}