package com.pira.darknama.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteGroup(
    val id: String, // UUID or unique identifier
    val name: String,
    val isDefault: Boolean = false,
    val movieIds: MutableList<Int> = mutableListOf(),
    val seriesIds: MutableList<Int> = mutableListOf()
) {
    // Helper functions to check if a favorite is in this group
    fun containsMovie(movieId: Int): Boolean = movieIds.contains(movieId)
    fun containsSeries(seriesId: Int): Boolean = seriesIds.contains(seriesId)
    
    // Helper functions to add/remove favorites from this group
    fun addMovie(movieId: Int) {
        if (!movieIds.contains(movieId)) {
            movieIds.add(movieId)
        }
    }
    
    fun addSeries(seriesId: Int) {
        if (!seriesIds.contains(seriesId)) {
            seriesIds.add(seriesId)
        }
    }
    
    fun removeMovie(movieId: Int) {
        movieIds.remove(movieId)
    }
    
    fun removeSeries(seriesId: Int) {
        seriesIds.remove(seriesId)
    }
    
    // Copy function for renaming
    fun copy(name: String = this.name): FavoriteGroup {
        return FavoriteGroup(
            id = this.id,
            name = name,
            isDefault = this.isDefault,
            movieIds = this.movieIds.toMutableList(),
            seriesIds = this.seriesIds.toMutableList()
        )
    }
}