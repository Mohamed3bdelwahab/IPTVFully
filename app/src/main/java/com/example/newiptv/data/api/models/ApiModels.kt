package com.example.newiptv.data.api.models

// 🔹 Category JSON from API
data class ApiCategory(
    val category_id: String,
    val category_name: String,
    val parent_id: Int
)

// 🔹 Item JSON (series, movies, live tv entry) from API
data class ApiItem(
    val num: Int?,
    val series_id: String?, // For series
    val stream_id: String?, // For live TV
    val movie_id: String?,  // For movies
    val name: String,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val last_modified: String?,
    val rating: String?,
    val rating_5based: Double?,
    val backdrop_path: Any?, // Can be List<String>, String, or empty
    val youtube_trailer: String?,
    val episode_run_time: String?,
    val category_id: String
)

// 🔹 Season object from API
data class ApiSeason(
    val season_number: Int?,
    val air_date: String? = null
)

// 🔹 Info Response (Series/Movies)
data class ApiInfoResponse(
    val seasons: List<ApiSeason>?,
    val info: ApiInfoDetail?,
    val episodes: Map<String, List<ApiEpisode>>? // key = season number
)

// 🔹 Info Details (metadata)
data class ApiInfoDetail(
    val name: String?,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val last_modified: String?,
    val rating: String?,
    val rating_5based: Double?,
    val backdrop_path: Any?, // Can be List<String>, String, or null
    val youtube_trailer: String?,
    val episode_run_time: String?,
    val category_id: String?
)

// 🔹 Episode JSON
data class ApiEpisode(
    val id: String,
    val episode_num: Int,
    val title: String?,
    val container_extension: String?,
    val info: ApiEpisodeInfo?,
    val added: String?,
    val season: Int,
    val direct_source: String?
)

data class ApiEpisodeInfo(
    val movie_image: String?,
    val plot: String?,
    val rating: String?,
    val releasedate: String?
)
