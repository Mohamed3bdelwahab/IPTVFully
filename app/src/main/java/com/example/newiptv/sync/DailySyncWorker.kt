package com.example.newiptv.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.repository.TvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Daily sync worker that loads all series and movies data
 * Runs once per day or when manually triggered
 */
class DailySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DailySyncWorker"
        const val WORK_NAME = "daily_sync_work"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Starting daily sync...")
            
            val database = DatabaseProvider.getDatabase(applicationContext)
            val repository = TvRepository(database)
            
            // Load ALL series and movies at once using repository sync methods
            Log.d(TAG, "🔄 Starting bulk data loading...")
            
            var totalSeries = 0
            var totalMovies = 0
            
            try {
                // Sync all series categories and their content
                Log.d(TAG, "📺 Syncing all series categories and content...")
                repository.syncCategories("series")
                
                // Get all series categories and load their content
                val seriesCategories = database.categoryDao().getCategoriesSync("series")
                Log.d(TAG, "📺 Found ${seriesCategories.size} series categories")
                
                seriesCategories.forEach { category ->
                    try {
                        Log.d(TAG, "📺 Loading series for category: ${category.categoryName}")
                        repository.syncItems("series", category.categoryId)
                        val series = database.itemDao().getItemsByCategorySync(category.categoryId, "series")
                        totalSeries += series.size
                        Log.d(TAG, "✅ Loaded ${series.size} series for category: ${category.categoryName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to sync series category: ${category.categoryName}", e)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to sync series", e)
            }
            
            try {
                // Sync all movie categories and their content
                Log.d(TAG, "🎬 Syncing all movie categories and content...")
                repository.syncCategories("movie")
                
                // Get all movie categories and load their content
                val movieCategories = database.categoryDao().getCategoriesSync("movie")
                Log.d(TAG, "🎬 Found ${movieCategories.size} movie categories")
                
                movieCategories.forEach { category ->
                    try {
                        Log.d(TAG, "🎬 Loading movies for category: ${category.categoryName}")
                        repository.syncItems("movie", category.categoryId)
                        val movies = database.itemDao().getItemsByCategorySync(category.categoryId, "movie")
                        totalMovies += movies.size
                        Log.d(TAG, "✅ Loaded ${movies.size} movies for category: ${category.categoryName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to sync movie category: ${category.categoryName}", e)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to sync movies", e)
            }
            
            Log.d(TAG, "🎉 Daily sync completed successfully!")
            Log.d(TAG, "📊 Total series loaded: $totalSeries")
            Log.d(TAG, "📊 Total movies loaded: $totalMovies")
            
            // Update last sync time
            updateLastSyncTime()
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Daily sync failed", e)
            Result.retry()
        }
    }
    
    private fun updateLastSyncTime() {
        val prefs = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
        Log.d(TAG, "⏰ Updated last sync time")
    }
}
