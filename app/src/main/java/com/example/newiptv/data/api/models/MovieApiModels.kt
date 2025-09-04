package com.example.newiptv.data.api.models

// 🔹 Movie Category API Response
data class ApiMovieCategory(
    val category_id: String,
    val category_name: String,
    val parent_id: Int
)

// 🔹 Movie Item API Response (from get_vod_streams)
data class ApiMovieItem(
    val num: Int?,
    val name: String,
    val stream_type: String,
    val stream_id: Int,
    val stream_icon: String?,
    val rating: String?,
    val rating_5based: Double?,
    val added: String?,
    val is_adult: String?,
    val category_id: String,
    val container_extension: String?,
    val custom_sid: String?,
    val direct_source: String?
)

// 🔹 Movie Info API Response (from get_vod_info)
data class ApiMovieInfoResponse(
    val info: ApiMovieInfoDetail?,
    val movie_data: ApiMovieStreamData?
)

// 🔹 Movie Info Details
data class ApiMovieInfoDetail(
    val kinopoisk_url: String?,
    val name: String?,
    val o_name: String?,
    val cover_big: String?,
    val movie_image: String?,
    val releasedate: String?,
    val episode_run_time: Int?,
    val youtube_trailer: String?,
    val director: String?,
    val actors: String?,
    val cast: String?,
    val description: String?,
    val plot: String?,
    val age: String?,
    val rating_mpaa: String?,
    val rating_kinopoisk: Double?,
    val rating_count_kinopoisk: Int?,
    val country: String?,
    val genre: String?,
    val backdrop_path: List<String>?,
    val tmdb_id: String?,
    val duration_secs: Int?,
    val duration: String?,
    val video: Map<String, Any>?,
    val audio: Map<String, Any>?,
    val bitrate: Int?,
    val backdrop: String?,
    val rating: String?
)

// 🔹 Movie Stream Data
data class ApiMovieStreamData(
    val stream_id: Int?,
    val name: String?,
    val added: String?,
    val category_id: String?,
    val container_extension: String?,
    val custom_sid: String?,
    val direct_source: String?
)

// 🔹 TMDB Movie Details (Enhanced metadata)
data class ApiTmdbMovie(
    val id: Int,
    val title: String?,
    val overview: String?,
    val backdrop_path: String?,
    val poster_path: String?,
    val release_date: String?,
    val runtime: Int?,
    val vote_average: Double?,
    val vote_count: Int?,
    val genres: List<ApiTmdbGenre>?,
    val production_countries: List<ApiTmdbCountry>?,
    val spoken_languages: List<ApiTmdbLanguage>?
)

// 🔹 TMDB Genre
data class ApiTmdbGenre(
    val id: Int,
    val name: String
)

// 🔹 TMDB Country
data class ApiTmdbCountry(
    val iso_3166_1: String,
    val name: String
)

// 🔹 TMDB Language
data class ApiTmdbLanguage(
    val english_name: String,
    val iso_639_1: String,
    val name: String
)

// 🔹 TMDB Cast & Crew
data class ApiTmdbCredits(
    val cast: List<ApiTmdbCast>?,
    val crew: List<ApiTmdbCrew>?
)

// 🔹 TMDB Cast Member
data class ApiTmdbCast(
    val id: Int,
    val name: String,
    val character: String?,
    val profile_path: String?
)

// 🔹 TMDB Crew Member
data class ApiTmdbCrew(
    val id: Int,
    val name: String,
    val job: String?,
    val profile_path: String?
)

// 🔹 TMDB Videos (Trailers)
data class ApiTmdbVideos(
    val results: List<ApiTmdbVideo>?
)

// 🔹 TMDB Video
data class ApiTmdbVideo(
    val id: String,
    val key: String?,
    val name: String?,
    val site: String?,
    val type: String?
)
