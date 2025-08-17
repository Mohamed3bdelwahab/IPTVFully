package com.example.iptvtv.data.remote

import retrofit2.http.GET
import retrofit2.http.Url

interface EPGService {
    @GET
    suspend fun getEPGData(@Url url: String): String
}
