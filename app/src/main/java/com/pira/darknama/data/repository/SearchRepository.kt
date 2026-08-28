package com.pira.darknama.data.repository

import com.pira.darknama.data.model.Country
import com.pira.darknama.data.model.Genre
import com.pira.darknama.data.model.Poster
import com.pira.darknama.data.model.SearchResult
import com.pira.darknama.data.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class SearchRepository : BaseRepository() {
    private val BASE_URL = "https://server-hi-speed-iran.info/api/search"
    
    suspend fun search(query: String): SearchResult {
        return withContext(Dispatchers.IO) {
            try {
                // Properly encode the query for URL paths
                val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString()).replace("+", "%20")
                val url = "$BASE_URL/$encodedQuery/$API_KEY/"
                
                val jsonData = executeRequest(url) { Request.Builder().url(it).build() }
                
                parseSearchResult(jsonData)
            } catch (e: Exception) {
                throw Exception("Error searching: ${e.message}")
            }
        }
    }
    
    private fun parseSearchResult(jsonData: String): SearchResult {
        val jsonObject = JSONObject(jsonData)
        val postersArray = jsonObject.getJSONArray("posters")
        val posters = parsePosters(postersArray)
        
        return SearchResult(
            posters = posters
        )
    }
    
    private fun parsePosters(postersArray: JSONArray): List<Poster> {
        val posters = mutableListOf<Poster>()
        for (i in 0 until postersArray.length()) {
            try {
                val posterObj = postersArray.getJSONObject(i)
                posters.add(parsePoster(posterObj))
            } catch (e: Exception) {
                // Skip posters that fail to parse
                continue
            }
        }
        return posters
    }
    
    private fun parsePoster(posterObj: JSONObject): Poster {
        return Poster(
            id = posterObj.optInt("id", 0),
            title = posterObj.optString("title", "Unknown Title"),
            type = posterObj.optString("type", ""),
            description = posterObj.optString("description", "No description available"),
            year = posterObj.optInt("year", 0),
            imdb = posterObj.optDouble("imdb", 0.0),
            rating = posterObj.optDouble("rating", 0.0),
            duration = posterObj.optString("duration", null).takeIf { it != "null" && it != "N/A" },
            image = posterObj.optString("image", ""),
            cover = posterObj.optString("cover", ""),
            genres = try {
                parseGenres(posterObj.getJSONArray("genres"))
            } catch (e: Exception) {
                emptyList()
            },
            sources = try {
                parseSources(posterObj.getJSONArray("sources"))
            } catch (e: Exception) {
                emptyList()
            },
            country = try {
                parseCountries(posterObj.getJSONArray("country"))
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
