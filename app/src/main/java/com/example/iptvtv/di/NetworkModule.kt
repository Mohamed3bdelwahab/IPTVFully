package com.example.iptvtv.di

import com.example.iptvtv.data.remote.IPTVService
import com.example.iptvtv.data.remote.EPGService
import com.example.iptvtv.util.M3UParser
import com.example.iptvtv.util.EPGParser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://example.com/") // Base URL for your API
            .client(okHttpClient)
            .addConverterFactory(com.squareup.retrofit2.converter.scalars.ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideIPTVService(retrofit: Retrofit): IPTVService {
        return retrofit.create(IPTVService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideEPGService(retrofit: Retrofit): EPGService {
        return retrofit.create(EPGService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideM3UParser(): M3UParser {
        return M3UParser()
    }
    
    @Provides
    @Singleton
    fun provideEPGParser(): EPGParser {
        return EPGParser()
    }
}
