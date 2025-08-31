package com.example.newiptv.data.api

import com.example.newiptv.data.api.models.*
import retrofit2.http.GET
import retrofit2.http.Query

interface TvApi {

    // 🔹 Categories (Series, Movies, Live TV)
    @GET("player_api.php")
    suspend fun getCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String // e.g. "get_series_categories"
    ): List<ApiCategory>

    // 🔹 Items list (Series, Movies, Live TV entries)
    @GET("player_api.php")
    suspend fun getItems(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String, // e.g. "get_series"
        @Query("category_id") categoryId: String? = null
    ): List<ApiItem>

    // 🔹 Detailed Info for one Series/Movie/Live stream
    @GET("player_api.php")
    suspend fun getInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String, // e.g. "get_series_info"
        @Query("series_id") seriesId: String? = null,
        @Query("vod_id") movieId: String? = null,
        @Query("stream_id") liveId: String? = null
    ): ApiInfoResponse
}
