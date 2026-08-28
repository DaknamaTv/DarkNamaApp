package com.dark.darknama.data.repository

import com.dark.darknama.data.model.FilterType
import com.dark.darknama.data.model.Poster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class CountryPostersRepository : BaseRepository() {
    private val BASE_URL = "https://server-hi-speed-iran.info/api/poster/by/filtres"
    
    suspend fun getPostersByCountry(countryId: Int, page: Int = 0, filterType: FilterType = FilterType.DEFAULT): List<Poster> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl(countryId, filterType, page)
                
                val jsonData = executeRequest(url) { Request.Builder().url(it).build() }
                
                parsePosters(jsonData)
            } catch (e: Exception) {
                throw Exception("Error fetching posters for country $countryId: ${e.message}")
            }
        }
    }
    
    private fun buildUrl(countryId: Int, filterType: FilterType, page: Int): String {
        return when (filterType) {
            FilterType.DEFAULT -> "$BASE_URL/0/$countryId/created/$page/$API_KEY"
            FilterType.BY_YEAR -> "$BASE_URL/0/$countryId/year/$page/$API_KEY"
            FilterType.BY_IMDB -> "$BASE_URL/0/$countryId/imdb/$page/$API_KEY"
        }
    }
    
    private fun parsePosters(jsonData: String): List<Poster> {
        val posters = mutableListOf<Poster>()
        val jsonArray = JSONArray(jsonData)
        
        for (i in 0 until jsonArray.length()) {
            try {
                val posterObj = jsonArray.getJSONObject(i)
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
                val genresArray = posterObj.getJSONArray("genres")
                val genres = mutableListOf<com.dark.darknama.data.model.Genre>()
                for (j in 0 until genresArray.length()) {
                    val genreObj = genresArray.getJSONObject(j)
                    genres.add(
                        com.dark.darknama.data.model.Genre(
                            id = genreObj.optInt("id", 0),
                            title = genreObj.optString("title", "Unknown")
                        )
                    )
                }
                genres
            } catch (e: Exception) {
                emptyList()
            },
            sources = try {
                val sourcesArray = posterObj.getJSONArray("sources")
                val sources = mutableListOf<com.dark.darknama.data.model.Source>()
                for (j in 0 until sourcesArray.length()) {
                    val sourceObj = sourcesArray.getJSONObject(j)
                    sources.add(
                        com.dark.darknama.data.model.Source(
                            id = sourceObj.optInt("id", 0),
                            quality = sourceObj.optString("quality", "Unknown"),
                            type = sourceObj.optString("type", "Unknown"),
                            url = sourceObj.optString("url", "")
                        )
                    )
                }
                sources
            } catch (e: Exception) {
                emptyList()
            },
            country = try {
                val countriesArray = posterObj.getJSONArray("country")
                val countries = mutableListOf<com.dark.darknama.data.model.Country>()
                for (j in 0 until countriesArray.length()) {
                    val countryObj = countriesArray.getJSONObject(j)
                    countries.add(
                        com.dark.darknama.data.model.Country(
                            id = countryObj.optInt("id", 0),
                            title = countryObj.optString("title", "Unknown"),
                            image = countryObj.optString("image", "")
                        )
                    )
                }
                countries
            } catch (e: Exception) {
                emptyList()
            }
        )
    }
}
