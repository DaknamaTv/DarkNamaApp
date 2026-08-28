package com.dark.darknama.data.repository

import com.dark.darknama.data.model.Country
import com.dark.darknama.data.model.FilterType
import com.dark.darknama.data.model.Genre
import com.dark.darknama.data.model.Movie
import com.dark.darknama.data.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MovieRepository : BaseRepository() {
    private val BASE_URL = "https://server-hi-speed-iran.info/api/movie/by/filtres"
    
    suspend fun getMovies(page: Int = 0, genreId: Int = 0, filterType: FilterType = FilterType.DEFAULT): List<Movie> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl(BASE_URL, genreId, filterType, page)
                
                val jsonData = executeRequest(url) { Request.Builder().url(it).build() }
                
                parseMovies(jsonData)
            } catch (e: Exception) {
                throw Exception("Error fetching movies: ${e.message}")
            }
        }
    }
    
    private fun buildUrl(baseUrl: String, genreId: Int, filterType: FilterType, page: Int): String {
        return when (filterType) {
            FilterType.DEFAULT -> "$baseUrl/$genreId/created/$page/$API_KEY"
            FilterType.BY_YEAR -> "$baseUrl/$genreId/year/$page/$API_KEY"
            FilterType.BY_IMDB -> "$baseUrl/$genreId/imdb/$page/$API_KEY"
        }
    }
    
    private fun parseMovies(jsonData: String): List<Movie> {
        val movies = mutableListOf<Movie>()
        val jsonArray = JSONArray(jsonData)
        
        for (i in 0 until jsonArray.length()) {
            try {
                val movieObj = jsonArray.getJSONObject(i)
                val movie = parseMovie(movieObj)
                movies.add(movie)
            } catch (e: Exception) {
                // Skip items that fail to parse
                continue
            }
        }
        
        return movies
    }
    
    private fun parseMovie(movieObj: JSONObject): Movie {
        return Movie(
            id = movieObj.optInt("id", 0),
            type = movieObj.optString("type", ""),
            title = movieObj.optString("title", "Unknown Title"),
            description = movieObj.optString("description", "No description available"),
            year = movieObj.optInt("year", 0),
            imdb = movieObj.optDouble("imdb", 0.0),
            rating = movieObj.optDouble("rating", 0.0),
            duration = movieObj.optString("duration", null).takeIf { it != "null" && it != "N/A" },
            image = movieObj.optString("image", ""),
            cover = movieObj.optString("cover", ""),
            genres = try {
                parseGenres(movieObj.getJSONArray("genres"))
            } catch (e: Exception) {
                emptyList()
            },
            sources = try {
                parseSources(movieObj.getJSONArray("sources"))
            } catch (e: Exception) {
                emptyList()
            },
            country = try {
                parseCountries(movieObj.getJSONArray("country"))
            } catch (e: Exception) {
                emptyList()
            }
        )
    }
    
    private fun parseGenres(genresArray: JSONArray): List<Genre> {
        val genres = mutableListOf<Genre>()
        for (i in 0 until genresArray.length()) {
            try {
                val genreObj = genresArray.getJSONObject(i)
                genres.add(
                    Genre(
                        id = genreObj.optInt("id", 0),
                        title = genreObj.optString("title", "Unknown")
                    )
                )
            } catch (e: Exception) {
                // Skip genres that fail to parse
                continue
            }
        }
        return genres
    }
    
    private fun parseSources(sourcesArray: JSONArray): List<Source> {
        val sources = mutableListOf<Source>()
        for (i in 0 until sourcesArray.length()) {
            try {
                val sourceObj = sourcesArray.getJSONObject(i)
                sources.add(
                    Source(
                        id = sourceObj.optInt("id", 0),
                        quality = sourceObj.optString("quality", "Unknown"),
                        type = sourceObj.optString("type", "Unknown"),
                        url = sourceObj.optString("url", "")
                    )
                )
            } catch (e: Exception) {
                // Skip sources that fail to parse
                continue
            }
        }
        return sources
    }
    
    private fun parseCountries(countriesArray: JSONArray): List<Country> {
        val countries = mutableListOf<Country>()
        for (i in 0 until countriesArray.length()) {
            try {
                val countryObj = countriesArray.getJSONObject(i)
                countries.add(
                    Country(
                        id = countryObj.optInt("id", 0),
                        title = countryObj.optString("title", "Unknown"),
                        image = countryObj.optString("image", "")
                    )
                )
            } catch (e: Exception) {
                // Skip countries that fail to parse
                continue
            }
        }
        return countries
    }
}
