package com.pira.darknama.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val posters: List<Poster>
)

@Serializable
data class Poster(
    val id: Int,
    val title: String,
    val type: String,
    val description: String,
    val year: Int,
    val imdb: Double,
    val rating: Double,
    val duration: String?,
    val image: String,
    val cover: String,
    val genres: List<Genre>,
    val sources: List<Source>,
    val country: List<Country>
) {
    // Convert Poster to Movie for compatibility with existing UI components
    fun toMovie(): Movie {
        return Movie(
            id = id,
            type = type,
            title = title,
            description = description,
            year = year,
            imdb = imdb,
            rating = rating,
            duration = duration,
            image = image,
            cover = cover,
            genres = genres,
            sources = sources,
            country = country
        )
    }
    
    // Convert Poster to Series for compatibility with existing UI components
    fun toSeries(): Series {
        return Series(
            id = id,
            type = type,
            title = title,
            description = description,
            year = year,
            imdb = imdb,
            rating = rating,
            duration = duration,
            image = image,
            cover = cover,
            genres = genres,
            country = country
        )
    }
    
    // Check if this is a movie
    fun isMovie(): Boolean {
        return type == "movie"
    }
    
    // Check if this is a series
    fun isSeries(): Boolean {
        return type == "serie"
    }
}