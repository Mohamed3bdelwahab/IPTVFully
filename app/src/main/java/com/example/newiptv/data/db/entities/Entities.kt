package com.example.newiptv.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// 🔹 Categories (Series, Movies, Live TV)
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val categoryId: String,
    val categoryName: String,
    val parentId: Int,
    val type: String // "series", "movie", "live"
)

// 🔹 Items (Series, Movies, Live TV entries)
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val itemId: String,
    val name: String,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val lastModified: String?,
    val rating: String?,
    val rating5Based: Double?,
    val backdropPath: String?, // JSON array as string
    val youtubeTrailer: String?,
    val episodeRunTime: String?,
    val categoryId: String,
    val type: String
)

// 🔹 Info Screen (Detailed metadata for a single item)
@Entity(tableName = "info")
data class InfoEntity(
    @PrimaryKey val itemId: String,
    val name: String?,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val lastModified: String?,
    val rating: String?,
    val rating5Based: Double?,
    val backdropPath: String?, // JSON array as string
    val youtubeTrailer: String?,
    val episodeRunTime: String?,
    val categoryId: String,
    val type: String
)



// 🔹 Episodes (only for series, linked to season + series)
@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val itemId: String,       // link to Series
    val season: Int,          // link to Season
    val episodeNum: Int,
    val title: String?,
    val containerExtension: String?,
    val movieImage: String?,
    val plot: String?,
    val rating: String?,
    val releaseDate: String?,
    val directSource: String?
)
