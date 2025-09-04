package com.example.newiptv.data.mapping

import com.example.newiptv.data.api.models.*
import com.example.newiptv.data.db.entities.*

object MovieApiMapping {

    // 🔹 Map Movie Category API → Database Entity
    fun mapMovieCategory(api: ApiMovieCategory): MovieCategoryEntity {
        return MovieCategoryEntity(
            categoryId = api.category_id,
            categoryName = api.category_name,
            parentId = api.parent_id,
            type = "movies"
        )
    }

    // 🔹 Map Movie Item API → Database Entity
    fun mapMovieItem(api: ApiMovieItem): MovieItemEntity {
        return MovieItemEntity(
            itemId = api.stream_id.toString(),
            name = api.name,
            streamType = api.stream_type,
            streamId = api.stream_id.toString(),
            streamIcon = api.stream_icon,
            rating = api.rating,
            rating5Based = api.rating_5based,
            added = api.added,
            isAdult = api.is_adult ?: "0",
            categoryId = api.category_id,
            containerExtension = api.container_extension,
            customSid = api.custom_sid,
            directSource = api.direct_source,
            type = "movies"
        )
    }

    // 🔹 Map Movie Info API → Database Entity
    fun mapMovieInfo(api: ApiMovieInfoDetail, streamData: ApiMovieStreamData?): MovieInfoEntity {
        return MovieInfoEntity(
            itemId = streamData?.stream_id?.toString() ?: "",
            name = api.name,
            originalName = api.o_name,
            coverBig = api.cover_big,
            movieImage = api.movie_image,
            releaseDate = api.releasedate,
            episodeRunTime = api.episode_run_time ?: 0,
            youtubeTrailer = api.youtube_trailer,
            director = api.director,
            actors = api.actors,
            cast = api.cast,
            description = api.description,
            plot = api.plot,
            age = api.age,
            ratingMpaa = api.rating_mpaa,
            ratingKinopoisk = api.rating_kinopoisk,
            ratingCountKinopoisk = api.rating_count_kinopoisk ?: 0,
            country = api.country,
            genre = api.genre,
            backdropPath = api.backdrop_path?.joinToString(","),
            tmdbId = api.tmdb_id,
            durationSecs = api.duration_secs ?: 0,
            duration = api.duration,
            bitrate = api.bitrate ?: 0,
            backdrop = api.backdrop,
            rating = api.rating,
            categoryId = streamData?.category_id ?: "",
            type = "movies"
        )
    }

    // 🔹 Map Movie Stream Data API → Database Entity
    fun mapMovieStreamData(api: ApiMovieStreamData): MovieStreamDataEntity {
        return MovieStreamDataEntity(
            itemId = api.stream_id?.toString() ?: "",
            name = api.name,
            added = api.added,
            categoryId = api.category_id ?: "",
            containerExtension = api.container_extension,
            customSid = api.custom_sid,
            directSource = api.direct_source,
            type = "movies"
        )
    }

    // 🔹 Map TMDB Movie → Enhanced Movie Info
    fun mapTmdbMovie(tmdb: ApiTmdbMovie, existingInfo: MovieInfoEntity?): MovieInfoEntity {
        return existingInfo?.copy(
            // Enhanced fields from TMDB
            plot = tmdb.overview ?: existingInfo.plot,
            ratingKinopoisk = tmdb.vote_average,
            ratingCountKinopoisk = tmdb.vote_count ?: 0,
            durationSecs = tmdb.runtime ?: existingInfo.durationSecs,
            duration = tmdb.runtime?.let { "${it / 60}:${String.format("%02d", it % 60)}" } ?: existingInfo.duration,
            // Additional TMDB fields can be added here
        ) ?: MovieInfoEntity(
            itemId = tmdb.id.toString(),
            name = tmdb.title,
            originalName = tmdb.title,
            coverBig = null,
            movieImage = null,
            releaseDate = tmdb.release_date,
            episodeRunTime = 0,
            youtubeTrailer = null,
            director = null,
            actors = null,
            cast = null,
            description = tmdb.overview,
            plot = tmdb.overview,
            age = null,
            ratingMpaa = null,
            ratingKinopoisk = tmdb.vote_average,
            ratingCountKinopoisk = tmdb.vote_count ?: 0,
            country = null,
            genre = tmdb.genres?.joinToString(", ") { it.name },
            backdropPath = tmdb.backdrop_path,
            tmdbId = tmdb.id.toString(),
            durationSecs = tmdb.runtime ?: 0,
            duration = tmdb.runtime?.let { "${it / 60}:${String.format("%02d", it % 60)}" },
            bitrate = 0,
            backdrop = tmdb.backdrop_path,
            rating = tmdb.vote_average?.toString(),
            categoryId = "",
            type = "movies"
        )
    }

    // 🔹 Construct Movie Playback URL
    fun constructMoviePlaybackUrl(streamId: String, containerExtension: String?): String {
        val extension = containerExtension ?: "mkv"
        return "http://aws85485.amazonedge.net//movie/moh7amed819/150730/$streamId.$extension"
    }

    // 🔹 Get TMDB Image URL
    fun getTmdbImageUrl(path: String?, size: String = "w500"): String? {
        return path?.let { "https://image.tmdb.org/t/p/$size$it" }
    }

    // 🔹 Get YouTube Trailer URL
    fun getYouTubeTrailerUrl(key: String?): String? {
        return key?.let { "https://www.youtube.com/watch?v=$it" }
    }
}
