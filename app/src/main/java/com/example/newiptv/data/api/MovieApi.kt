package com.example.newiptv.data.api

import com.example.newiptv.data.api.models.*
import retrofit2.http.GET
import retrofit2.http.Query

interface MovieApi {
    
    // 🔹 Get Movie Categories
    @GET("player_api.php")
    suspend fun getMovieCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<ApiMovieCategory>

    // 🔹 Get Movies List (All movies)
    @GET("player_api.php")
    suspend fun getMovies(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams"
    ): List<ApiMovieItem>

    // 🔹 Get Movies by Category
    @GET("player_api.php")
    suspend fun getMoviesByCategory(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String
    ): List<ApiMovieItem>

    // 🔹 Get Movie Details
    @GET("player_api.php")
    suspend fun getMovieInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_info",
        @Query("vod_id") movieId: String
    ): ApiMovieInfoResponse
}

// 🔹 TMDB API Interface for Enhanced Movie Data
interface TmdbApi {
    
    // 🔹 Get Movie Details
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @retrofit2.http.Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String
    ): ApiTmdbMovie

    // 🔹 Get Movie Cast & Crew
    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @retrofit2.http.Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String
    ): ApiTmdbCredits

    // 🔹 Get Movie Trailers
    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @retrofit2.http.Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String
    ): ApiTmdbVideos
}
