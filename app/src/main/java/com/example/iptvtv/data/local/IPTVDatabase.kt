package com.example.iptvtv.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.iptvtv.data.model.Channel
import com.example.iptvtv.data.model.EPGProgram
import com.example.iptvtv.data.model.PlaylistConfig

@Database(entities = [Channel::class, EPGProgram::class, PlaylistConfig::class], version = 1, exportSchema = false)
abstract class IPTVDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EPGDao
    abstract fun playlistConfigDao(): PlaylistConfigDao
    
    companion object {
        @Volatile
        private var INSTANCE: IPTVDatabase? = null
        
        fun getDatabase(context: Context): IPTVDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IPTVDatabase::class.java,
                    "iptv_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
