package com.example.newiptv.data.mapping

import com.example.newiptv.data.api.models.*
import com.example.newiptv.data.db.entities.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Maps API responses (Series, Movies, Live TV) into Room DB entities.
 * Handles Categories, Items, Info, Seasons, and Episodes.
 */
object ApiTVMapping {

    private val gson = Gson()

    // 🔹 Map a single Category
    fun mapCategory(api: ApiCategory, type: String): CategoryEntity {
        return CategoryEntity(
            categoryId = api.category_id,
            categoryName = api.category_name,
            parentId = api.parent_id,
            type = type
        )
    }

    // 🔹 Map a single Item (Series, Movie, Live Stream)
    fun mapItem(api: ApiItem, type: String, categoryId: String? = null): ItemEntity {
        val itemId = api.series_id ?: api.movie_id ?: api.stream_id ?: api.num?.toString() ?: "0"

        val backdropPath = when (api.backdrop_path) {
            is List<*> -> gson.toJson(api.backdrop_path.filterIsInstance<String>())
            is String -> api.backdrop_path
            else -> null
        }

        return ItemEntity(
            itemId = itemId,
            name = api.name,
            cover = api.cover,
            plot = api.plot,
            cast = api.cast,
            director = api.director,
            genre = api.genre,
            releaseDate = api.releaseDate,
            lastModified = api.last_modified,
            rating = api.rating,
            rating5Based = api.rating_5based,
            backdropPath = backdropPath,
            youtubeTrailer = api.youtube_trailer,
            episodeRunTime = api.episode_run_time,
            categoryId = categoryId ?: api.category_id, // ✅ ensure correct category
            type = type
        )
    }

    // 🔹 Map detailed Info metadata
    fun mapInfo(api: ApiInfoDetail, itemId: String, type: String): InfoEntity {
        val backdropPath = when (api.backdrop_path) {
            is List<*> -> gson.toJson(api.backdrop_path.filterIsInstance<String>())
            is String -> api.backdrop_path
            else -> null
        }

        return InfoEntity(
            itemId = itemId,
            name = api.name ?: "",
            cover = api.cover,
            plot = api.plot,
            cast = api.cast,
            director = api.director,
            genre = api.genre,
            releaseDate = api.releaseDate,
            lastModified = api.last_modified,
            rating = api.rating,
            rating5Based = api.rating_5based,
            backdropPath = backdropPath,
            youtubeTrailer = api.youtube_trailer,
            episodeRunTime = api.episode_run_time,
            categoryId = api.category_id ?: "",
            type = type
        )
    }

    // 🔹 Map a single Episode
    fun mapEpisode(api: ApiEpisode, itemId: String, season: Int): EpisodeEntity {
        android.util.Log.d("ApiTVMapping", "=== MAPPING EPISODE ===")
        android.util.Log.d("ApiTVMapping", "Episode ID: ${api.id}")
        android.util.Log.d("ApiTVMapping", "Episode Title: ${api.title}")
        android.util.Log.d("ApiTVMapping", "Episode Season: $season")
        android.util.Log.d("ApiTVMapping", "Episode Number: ${api.episode_num}")
        android.util.Log.d("ApiTVMapping", "Direct Source (API): ${api.direct_source}")
        android.util.Log.d("ApiTVMapping", "Container Extension: ${api.container_extension}")
        android.util.Log.d("ApiTVMapping", "Raw API Episode Data: $api")
        
        // Try to construct video URL if direct_source is not available
        val videoUrl = when {
            !api.direct_source.isNullOrEmpty() -> {
                android.util.Log.d("ApiTVMapping", "Using direct_source: ${api.direct_source}")
                api.direct_source
            }
            !api.id.isNullOrEmpty() -> {
                // Construct URL using correct CDN pattern
                val extension = api.container_extension ?: "mkv"
                val constructedUrl = "http://aws85485.amazonedge.net/series/moh7amed819/150730/${api.id}.$extension"
                android.util.Log.d("ApiTVMapping", "Constructed URL from ID: $constructedUrl")
                constructedUrl
            }
            else -> {
                android.util.Log.w("ApiTVMapping", "No video URL available for episode ${api.id}")
                null
            }
        }
        
        android.util.Log.d("ApiTVMapping", "Final constructed videoUrl: $videoUrl")
        
        return EpisodeEntity(
            id = api.id,
            itemId = itemId,
            season = season,
            episodeNum = api.episode_num,
            title = api.title ?: "Episode ${api.episode_num}",
            containerExtension = api.container_extension,
            movieImage = api.info?.movie_image,
            plot = api.info?.plot,
            rating = api.info?.rating,
            releaseDate = api.info?.releasedate,
            directSource = videoUrl
        )
    }

    // 🔹 Bundle for Seasons + Episodes
    data class SeasonWithEpisodes(
        val seasons: List<SeasonEntity>,
        val episodes: List<EpisodeEntity>
    )

    // 🔹 Map all Seasons + Episodes for a given Series
    fun mapSeasonsAndEpisodes(
        episodesMap: Map<String, List<ApiEpisode>>,
        itemId: String
    ): SeasonWithEpisodes {
        val seasons = mutableListOf<SeasonEntity>()
        val episodes = mutableListOf<EpisodeEntity>()

        episodesMap.forEach { (seasonStr, episodeList) ->
            val seasonNumber = seasonStr.toIntOrNull() ?: 1

            // Add SeasonEntity
            seasons.add(
                SeasonEntity(
                    seasonId = "${itemId}_$seasonNumber", // unique key
                    seriesId = itemId,
                    seasonNumber = seasonNumber
                )
            )

            // Add Episodes
            episodeList.forEach { apiEpisode ->
                episodes.add(mapEpisode(apiEpisode, itemId, seasonNumber))
            }
        }

        return SeasonWithEpisodes(seasons, episodes)
    }

    // 🔹 Helper to parse JSON backdropPath into List<String>
    fun parseBackdropPath(backdropPathJson: String?): List<String> {
        return try {
            if (backdropPathJson.isNullOrEmpty()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(backdropPathJson, type) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
