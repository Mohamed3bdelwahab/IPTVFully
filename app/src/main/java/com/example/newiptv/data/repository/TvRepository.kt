package com.example.newiptv.data.repository

import android.util.Log
import com.example.newiptv.data.api.TvApiClient
import com.example.newiptv.data.api.models.*
import com.example.newiptv.data.db.AppDatabase
import com.example.newiptv.data.db.entities.*
import com.example.newiptv.data.mapping.ApiTVMapping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TvRepository(
    private val database: AppDatabase,
    private val username: String = "moh7amed819",
    private val password: String = "150730"
) {
    private val categoryDao = database.categoryDao()
    private val itemDao = database.itemDao()
    private val infoDao = database.infoDao()
    private val episodeDao = database.episodeDao()

    // ------------------------------------------------
    // 🔹 Categories
    // ------------------------------------------------
    suspend fun syncCategories(type: String) {
        val action = when (type) {
            "series" -> "get_series_categories"
            "movie" -> "get_vod_categories"
            "live" -> "get_live_categories"
            else -> "get_series_categories"
        }
        try {
            val apiCategories = TvApiClient.api.getCategories(username, password, action)
            val entities = apiCategories.map { ApiTVMapping.mapCategory(it, type) }
            categoryDao.deleteCategoriesByType(type)
            categoryDao.insertAll(entities)
            Log.d("TvRepository", "Synced ${entities.size} categories for type=$type")
        } catch (e: Exception) {
            Log.e("TvRepository", "Error syncing categories ($type)", e)
            throw e
        }
    }

    fun getCategories(type: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategories(type)

    // ------------------------------------------------
    // 🔹 Items (Series, Movies, Live TV)
    // ------------------------------------------------
    // ------------------------------------------------
    // 🔹 Items (Series, Movies, Live TV)
    // ------------------------------------------------
    suspend fun syncItems(type: String, categoryId: String) {
        val action = when (type) {
            "series" -> "get_series"
            "movie" -> "get_vod_streams"
            "live" -> "get_live_streams"
            else -> "get_series"
        }

        try {
            Log.d("TvRepository", "Fetching items for category=$categoryId type=$type")

            // ✅ Call appropriate API based on content type
            val entities = when (type) {
                "movie" -> {
                    val apiMovieItems = TvApiClient.api.getMovieItems(username, password, action, categoryId)
                    apiMovieItems.map { ApiTVMapping.mapMovieItem(it, categoryId) }
                }
                else -> {
                    val apiItems = TvApiClient.api.getItems(username, password, action, categoryId)
                    apiItems.map { ApiTVMapping.mapItem(it, type, categoryId) }
                }
            }

            // ✅ Clear + insert to DB
            itemDao.deleteItemsByCategory(categoryId, type)
            itemDao.insertAll(entities)

            Log.d("TvRepository", "Synced ${entities.size} items for category=$categoryId type=$type")
        } catch (e: Exception) {
            Log.e("TvRepository", "Error syncing items (cat=$categoryId, type=$type)", e)
            throw e
        }
    }


    fun getItemsByCategory(categoryId: String, type: String): Flow<List<ItemEntity>> =
        itemDao.getItemsByCategory(categoryId, type)

    // ------------------------------------------------
    // 🔹 Info + Episodes
    // ------------------------------------------------
    suspend fun syncInfo(type: String, itemId: String) {
    val action = when (type) {
        "series" -> "get_series_info"
        "movie" -> "get_vod_info"
        "live" -> "get_live_info"
        else -> "get_series_info"
    }

    try {
        when (type) {
            "movie" -> {
                // ✅ Handle movie info separately
                val apiMovieInfo = TvApiClient.api.getMovieInfo(username, password, action, itemId)
                
                // ✅ Get category ID from movie stream data
                val categoryId = apiMovieInfo.movie_data?.category_id ?: ""
                
                // ✅ Save movie info metadata
                apiMovieInfo.info?.let { info ->
                    val entity = ApiTVMapping.mapMovieInfo(info, itemId, categoryId)
                    infoDao.deleteInfo(itemId, type)
                    infoDao.insert(entity)
                    Log.d("TvRepository", "Saved movie info for item=$itemId category=$categoryId")
                }
            }
            else -> {
                // ✅ Handle series/live info
                val apiInfo = TvApiClient.api.getInfo(
                    username = username,
                    password = password,
                    action = action,
                    seriesId = if (type == "series") itemId else null,
                    movieId = if (type == "movie") itemId else null,
                    liveId = if (type == "live") itemId else null
                )

                // ✅ Save info metadata
                apiInfo.info?.let { info ->
                    val entity = ApiTVMapping.mapInfo(info, itemId, type)
                    infoDao.deleteInfo(itemId, type)
                    infoDao.insert(entity)
                    Log.d("TvRepository", "Saved info for item=$itemId type=$type")
                }

                // ✅ Save seasons + episodes (series only)
                if (type == "series" && apiInfo.episodes != null) {
                    val seasonWithEpisodes = ApiTVMapping.mapSeasonsAndEpisodes(apiInfo.episodes, itemId)

                    // Clear & insert seasons
                    database.seasonDao().deleteSeasonsBySeries(itemId)
                    database.seasonDao().insertAll(seasonWithEpisodes.seasons)

                    // Clear & insert episodes
                    episodeDao.deleteEpisodesByItemId(itemId)
                    episodeDao.insertAll(seasonWithEpisodes.episodes)

                    Log.d(
                        "TvRepository",
                        "Saved ${seasonWithEpisodes.seasons.size} seasons & ${seasonWithEpisodes.episodes.size} episodes for series=$itemId"
                    )
                    
                    // Log first few episodes to check directSource
                    seasonWithEpisodes.episodes.take(3).forEach { episode ->
                        Log.d("TvRepository", "Episode: ${episode.title}, DirectSource: ${episode.directSource}")
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("TvRepository", "Error syncing info for item=$itemId type=$type", e)
        throw e
    }
}


    fun getEpisodes(itemId: String): Flow<List<EpisodeEntity>> =
        episodeDao.getEpisodes(itemId)

    suspend fun getInfo(itemId: String, type: String): InfoEntity? =
        infoDao.getInfo(itemId, type)

    // ------------------------------------------------
    // 🔹 Combined API + DB loaders
    // ------------------------------------------------
    suspend fun loadCategoriesWithSync(type: String): Flow<Result<List<CategoryEntity>>> = flow {
        try {
            syncCategories(type)
            emit(Result.success(categoryDao.getCategoriesSync(type)))
        } catch (e: Exception) {
            val cached = categoryDao.getCategoriesSync(type)
            if (cached.isNotEmpty()) emit(Result.success(cached))
            else emit(Result.failure(e))
        }
    }

    suspend fun loadItemsWithSync(type: String, categoryId: String): Flow<Result<List<ItemEntity>>> = flow {
        try {
            syncItems(type, categoryId)
            emit(Result.success(itemDao.getItemsByCategorySync(categoryId, type)))
        } catch (e: Exception) {
            val cached = itemDao.getItemsByCategorySync(categoryId, type)
            if (cached.isNotEmpty()) emit(Result.success(cached))
            else emit(Result.failure(e))
        }
    }

    suspend fun loadEpisodesWithSync(type: String, itemId: String): Flow<Result<List<EpisodeEntity>>> = flow {
        try {
            syncInfo(type, itemId)
            emit(Result.success(episodeDao.getEpisodesSync(itemId)))
        } catch (e: Exception) {
            val cached = episodeDao.getEpisodesSync(itemId)
            if (cached.isNotEmpty()) emit(Result.success(cached))
            else emit(Result.failure(e))
        }
    }
}
