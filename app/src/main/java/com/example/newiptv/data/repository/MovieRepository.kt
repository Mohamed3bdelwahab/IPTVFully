package com.example.newiptv.data.repository

import com.example.newiptv.data.api.MovieApiClient
import com.example.newiptv.data.api.models.*
import com.example.newiptv.data.db.AppDatabase
import com.example.newiptv.data.db.entities.MovieCategoryEntity
import com.example.newiptv.data.db.entities.MovieItemEntity
import com.example.newiptv.data.db.entities.MovieInfoEntity
import com.example.newiptv.data.db.entities.MovieStreamDataEntity
import com.example.newiptv.data.mapping.MovieApiMapping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MovieRepository(
    private val database: AppDatabase
) {
    private val movieCategoryDao = database.movieCategoryDao()
    private val movieItemDao = database.movieItemDao()
    private val movieInfoDao = database.movieInfoDao()
    private val movieStreamDataDao = database.movieStreamDataDao()

    // 🔹 Constants
    companion object {
        private const val USERNAME = "moh7amed819"
        private const val PASSWORD = "150730"
    }

    // 🔹 Load Movie Categories with Sync
    fun loadMovieCategoriesWithSync(): Flow<Result<List<MovieCategoryEntity>>> = flow {
        try {
            // Fetch from API
            val apiCategories = MovieApiClient.hydraApi.getMovieCategories(USERNAME, PASSWORD)
            
            // Map to entities
            val categories = apiCategories.map { MovieApiMapping.mapMovieCategory(it) }
            
            // Store in database
            movieCategoryDao.insertAll(categories)
            
            // Return success
            emit(Result.success(categories))
            
        } catch (e: Exception) {
            // Return cached data if available, otherwise error
            val cachedCategories = movieCategoryDao.getAllCategoriesSync()
            if (cachedCategories.isNotEmpty()) {
                emit(Result.success(cachedCategories))
            } else {
                emit(Result.failure(e))
            }
        }
    }

    // 🔹 Load Movies by Category with Sync
    fun loadMoviesByCategoryWithSync(categoryId: String): Flow<Result<List<MovieItemEntity>>> = flow {
        try {
            // Fetch from API
            val apiMovies = MovieApiClient.hydraApi.getMoviesByCategory(USERNAME, PASSWORD, categoryId = categoryId)
            
            // Map to entities
            val movies = apiMovies.map { MovieApiMapping.mapMovieItem(it) }
            
            // Store in database
            movieItemDao.insertAll(movies)
            
            // Return success
            emit(Result.success(movies))
            
        } catch (e: Exception) {
            // Return cached data if available, otherwise error
            val cachedMovies = movieItemDao.getMoviesByCategorySync(categoryId)
            if (cachedMovies.isNotEmpty()) {
                emit(Result.success(cachedMovies))
            } else {
                emit(Result.failure(e))
            }
        }
    }

    // 🔹 Load All Movies with Sync
    fun loadAllMoviesWithSync(): Flow<Result<List<MovieItemEntity>>> = flow {
        try {
            // Fetch from API
            val apiMovies = MovieApiClient.hydraApi.getMovies(USERNAME, PASSWORD)
            
            // Map to entities
            val movies = apiMovies.map { MovieApiMapping.mapMovieItem(it) }
            
            // Store in database
            movieItemDao.insertAll(movies)
            
            // Return success
            emit(Result.success(movies))
            
        } catch (e: Exception) {
            // Return cached data if available, otherwise error
            val cachedMovies = movieItemDao.getAllMoviesSync()
            if (cachedMovies.isNotEmpty()) {
                emit(Result.success(cachedMovies))
            } else {
                emit(Result.failure(e))
            }
        }
    }

    // 🔹 Load Movie Info with Sync
    fun loadMovieInfoWithSync(movieId: String): Flow<Result<MovieInfoEntity>> = flow {
        try {
            // Fetch from API
            val apiResponse = MovieApiClient.hydraApi.getMovieInfo(USERNAME, PASSWORD, movieId = movieId)
            
            // Map to entities
            val movieInfo = apiResponse.info?.let { info ->
                MovieApiMapping.mapMovieInfo(info, apiResponse.movie_data)
            } ?: throw Exception("Movie info not available")
            val streamData = apiResponse.movie_data?.let { MovieApiMapping.mapMovieStreamData(it) }
            
            // Store in database
            movieInfoDao.insert(movieInfo)
            streamData?.let { movieStreamDataDao.insert(it) }
            
            // Try to enhance with TMDB data if available
            val enhancedInfo = enhanceWithTmdbData(movieInfo)
            
            // Return success
            emit(Result.success(enhancedInfo))
            
        } catch (e: Exception) {
            // Return cached data if available, otherwise error
            val cachedInfo = movieInfoDao.getMovieInfo(movieId)
            if (cachedInfo != null) {
                emit(Result.success(cachedInfo))
            } else {
                emit(Result.failure(e))
            }
        }
    }

    // 🔹 Enhance Movie Info with TMDB Data
    private suspend fun enhanceWithTmdbData(movieInfo: MovieInfoEntity): MovieInfoEntity {
        val tmdbId = movieInfo.tmdbId ?: return movieInfo
        
        return try {
            // Fetch TMDB data
            val tmdbMovie = MovieApiClient.tmdbApi.getMovieDetails(tmdbId, MovieApiClient.getTmdbApiKey())
            
            // Map and enhance
            val enhancedInfo = MovieApiMapping.mapTmdbMovie(tmdbMovie, movieInfo)
            
            // Update database
            movieInfoDao.insert(enhancedInfo)
            
            enhancedInfo
        } catch (e: Exception) {
            // Return original info if TMDB fails
            movieInfo
        }
    }

    // 🔹 Get Movie Categories (from database)
    fun getMovieCategories(): Flow<List<MovieCategoryEntity>> {
        return movieCategoryDao.getAllCategories()
    }

    // 🔹 Get Movies by Category (from database)
    fun getMoviesByCategory(categoryId: String): Flow<List<MovieItemEntity>> {
        return movieItemDao.getMoviesByCategory(categoryId)
    }

    // 🔹 Get Movie Info (from database)
    suspend fun getMovieInfo(movieId: String): MovieInfoEntity? {
        return movieInfoDao.getMovieInfo(movieId)
    }

    // 🔹 Get Movie by ID (from database)
    suspend fun getMovieById(movieId: String): MovieItemEntity? {
        return movieItemDao.getMovieById(movieId)
    }

    // 🔹 Clear All Data
    suspend fun clearAllData() {
        movieCategoryDao.deleteAll()
        movieItemDao.deleteAll()
        movieInfoDao.deleteAll()
        movieStreamDataDao.deleteAll()
    }
}
