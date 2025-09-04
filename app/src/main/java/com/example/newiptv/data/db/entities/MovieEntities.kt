package com.example.newiptv.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// 🔹 Movie Categories (VOD Categories)
@Entity(tableName = "movie_categories")
data class MovieCategoryEntity(
    @PrimaryKey val categoryId: String,      // "301"
    val categoryName: String,                // "2025 English | أجنبي"
    val parentId: Int,                       // 0
    val type: String = "movies"              // Always "movies"
)

// 🔹 Movie Items (VOD Streams)
@Entity(tableName = "movie_items")
data class MovieItemEntity(
    @PrimaryKey val itemId: String,          // stream_id as String
    val name: String,                        // Movie title
    val streamType: String,                  // "movie"
    val streamId: String,                    // stream_id for playback
    val streamIcon: String?,                 // Cover image URL
    val rating: String?,                     // Rating string
    val rating5Based: Double?,               // Rating on 5-point scale
    val added: String?,                      // Unix timestamp
    val isAdult: String,                     // "0" or "1"
    val categoryId: String,                  // category_id
    val containerExtension: String?,         // mkv, mp4, avi
    val customSid: String?,                  // Custom session ID
    val directSource: String?,               // Direct source URL
    val type: String = "movies"              // Always "movies"
)

// 🔹 Movie Info (Detailed metadata from get_vod_info)
@Entity(tableName = "movie_info")
data class MovieInfoEntity(
    @PrimaryKey val itemId: String,          // stream_id
    val name: String?,                       // Movie title
    val originalName: String?,               // o_name
    val coverBig: String?,                   // cover_big
    val movieImage: String?,                 // movie_image
    val releaseDate: String?,                // releasedate
    val episodeRunTime: Int,                 // episode_run_time
    val youtubeTrailer: String?,             // youtube_trailer
    val director: String?,                   // director
    val actors: String?,                     // actors
    val cast: String?,                       // cast
    val description: String?,                // description
    val plot: String?,                       // plot
    val age: String?,                        // age
    val ratingMpaa: String?,                 // rating_mpaa
    val ratingKinopoisk: Double?,            // rating_kinopoisk
    val ratingCountKinopoisk: Int,           // rating_count_kinopoisk
    val country: String?,                    // country
    val genre: String?,                      // genre
    val backdropPath: String?,               // backdrop_path
    val tmdbId: String?,                     // tmdb_id
    val durationSecs: Int,                   // duration_secs
    val duration: String?,                   // duration (HH:MM:SS)
    val bitrate: Int,                        // bitrate
    val backdrop: String?,                   // backdrop
    val rating: String?,                     // rating
    val categoryId: String,                  // category_id
    val type: String = "movies"              // Always "movies"
)

// 🔹 Movie Stream Data (Additional stream information)
@Entity(tableName = "movie_stream_data")
data class MovieStreamDataEntity(
    @PrimaryKey val itemId: String,          // stream_id
    val name: String?,                       // Movie name
    val added: String?,                      // Added timestamp
    val categoryId: String,                  // category_id
    val containerExtension: String?,         // mkv, mp4, avi
    val customSid: String?,                  // Custom session ID
    val directSource: String?,               // Direct source URL
    val type: String = "movies"              // Always "movies"
)
