package com.example.newiptv.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TvApiClient {
    private const val BASE_URL = "http://aws85485.amazonedge.net/"

    val api: TvApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TvApi::class.java)
    }
}
