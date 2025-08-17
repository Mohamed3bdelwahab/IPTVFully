package com.example.iptvtv.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface IPTVService {
    @GET
    suspend fun getM3UPlaylist(@Url url: String): String
    
    @GET
    suspend fun getEPGData(@Url url: String): String
    
    @GET
    suspend fun getM3UPlaylistRaw(@Url url: String): ResponseBody
}
