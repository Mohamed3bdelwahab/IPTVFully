package com.example.newiptv.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MovieApiClient {
    
    // 🔹 Hydra API Client
    private const val HYDRA_BASE_URL = "http://aws85485.amazonedge.net/"
    
    val hydraApi: MovieApi by lazy {
        Retrofit.Builder()
            .baseUrl(HYDRA_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MovieApi::class.java)
    }
    
    // 🔹 TMDB API Client
    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    private const val TMDB_API_KEY = "fed3065d290f59dee0b0433bc6ee9e39" // Replace with actual API key
    
    val tmdbApi: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }
    
    // 🔹 Get TMDB API Key (you can move this to BuildConfig or local.properties)
    fun getTmdbApiKey(): String = TMDB_API_KEY
}
