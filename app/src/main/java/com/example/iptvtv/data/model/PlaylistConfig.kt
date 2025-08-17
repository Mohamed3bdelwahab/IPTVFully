package com.example.iptvtv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_configs")
data class PlaylistConfig(
    @PrimaryKey val id: String,
    val name: String,
    val m3uUrl: String,
    val epgUrl: String? = null,
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)
