package com.example.newiptv.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seasons")
data class SeasonEntity(
    @PrimaryKey val seasonId: String,   // e.g. "seriesId_1"
    val seriesId: String,               // FK to ItemEntity.itemId
    val seasonNumber: Int               // Season number (1, 2, 3…)
)
