package com.example.iptvtv.di

import android.content.Context
import androidx.room.Room
import com.example.iptvtv.data.local.IPTVDatabase
import com.example.iptvtv.data.local.ChannelDao
import com.example.iptvtv.data.local.EPGDao
import com.example.iptvtv.data.local.PlaylistConfigDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IPTVDatabase {
        return Room.databaseBuilder(
            context,
            IPTVDatabase::class.java,
            "iptv_database"
        ).build()
    }
    
    @Provides
    fun provideChannelDao(database: IPTVDatabase): ChannelDao {
        return database.channelDao()
    }
    
    @Provides
    fun provideEPGDao(database: IPTVDatabase): EPGDao {
        return database.epgDao()
    }
    
    @Provides
    fun providePlaylistConfigDao(database: IPTVDatabase): PlaylistConfigDao {
        return database.playlistConfigDao()
    }
}
