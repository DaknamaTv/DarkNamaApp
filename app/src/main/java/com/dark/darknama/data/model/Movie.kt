package com.dark.darknama.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    val id: Int,
    val type: String,
    val title: String,
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
)

@Serializable
data class Genre(
    val id: Int,
    val title: String
)

@Serializable
data class Source(
    val id: Int,
    val quality: String,
    val type: String,
    val url: String
)

@Serializable
data class Country(
    val id: Int,
    val title: String,
    val image: String
)