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

// 🔹 Playback Position (Remember Last Position)
@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey val contentId: String,
    val contentType: String, // "movie", "episode", "live_stream", "vod"
    val position: Long, // Position in milliseconds
    val duration: Long, // Total duration in milliseconds
    val lastUpdated: Long, // Timestamp of last update
    val isCompleted: Boolean = false, // Whether content was completed
    val watchPercentage: Float = 0f // Percentage watched (0.0 to 1.0)
)

// 🔹 Watch History (Track Recently Watched Content)
@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val id: String, // Unique ID for history entry
    val contentId: String, // ID of the content (movie/series/episode)
    val contentType: String, // "movie", "series", "episode"
    val title: String, // Display title
    val cover: String?, // Cover image URL
    val streamUrl: String?, // Direct stream URL
    val categoryId: String?, // Category ID for organization
    val categoryName: String?, // Category name for display
    val seriesId: String?, // For episodes, the parent series ID
    val seasonNumber: Int?, // For episodes, the season number
    val episodeNumber: Int?, // For episodes, the episode number
    val lastWatched: Long, // Timestamp of last watch
    val watchDuration: Long, // How long was watched (in milliseconds)
    val totalDuration: Long, // Total content duration (in milliseconds)
    val watchPercentage: Float, // Percentage watched (0.0 to 1.0)
    val isCompleted: Boolean = false, // Whether content was completed
    val resumePosition: Long = 0L // Position to resume from
)

// 🔹 Favorite Playlist Entity
@Entity(tableName = "favorite_playlists")
data class FavoritePlaylistEntity(
    @PrimaryKey val id: String, // Unique playlist ID
    val name: String, // Playlist name
    val description: String?, // Optional description
    val createdDate: Long, // When playlist was created
    val itemCount: Int = 0, // Number of items in playlist
    val isDefault: Boolean = false // Whether this is a default playlist
)

// 🔹 Favorite Playlist Item Entity
@Entity(tableName = "favorite_playlist_items")
data class FavoritePlaylistItemEntity(
    @PrimaryKey val id: String, // Unique item ID
    val playlistId: String, // ID of the playlist this item belongs to
    val contentId: String, // ID of the content (movie/series/episode)
    val contentType: String, // "movie", "series", "episode"
    val title: String, // Display title
    val cover: String?, // Cover image URL
    val streamUrl: String?, // Direct stream URL
    val seriesId: String?, // For episodes, the parent series ID
    val seasonNumber: Int?, // For episodes, the season number
    val episodeNumber: Int?, // For episodes, the episode number
    val addedDate: Long, // When this item was added to playlist
    val sortOrder: Int = 0 // For custom ordering within playlist
)
