package com.example.newiptv.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Manages daily sync scheduling and execution
 */
class SyncManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SyncManager"
        private const val SYNC_INTERVAL_HOURS = 24L // 24 hours
    }
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Schedule daily sync with user-configurable interval
     */
    fun scheduleDailySync(intervalHours: Long = SYNC_INTERVAL_HOURS) {
        Log.d(TAG, "📅 Scheduling daily sync every $intervalHours hours")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<DailySyncWorker>(
            intervalHours, TimeUnit.HOURS,
            intervalHours / 4, TimeUnit.HOURS // Flex interval
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("daily_sync")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            DailySyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )
        
        Log.d(TAG, "✅ Daily sync scheduled successfully")
    }
    
    /**
     * Cancel daily sync
     */
    fun cancelDailySync() {
        Log.d(TAG, "❌ Canceling daily sync")
        workManager.cancelUniqueWork(DailySyncWorker.WORK_NAME)
    }
    
    /**
     * Trigger manual sync immediately
     */
    fun triggerManualSync() {
        Log.d(TAG, "🔄 Triggering manual sync")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<DailySyncWorker>()
            .setConstraints(constraints)
            .addTag("manual_sync")
            .build()
        
        workManager.enqueue(syncRequest)
        
        Log.d(TAG, "✅ Manual sync triggered")
    }
    
    /**
     * Get sync status
     */
    fun getSyncStatus(): SyncStatus {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val lastSyncTime = prefs.getLong("last_sync_time", 0L)
        val isEnabled = prefs.getBoolean("sync_enabled", true)
        val intervalHours = prefs.getLong("sync_interval_hours", SYNC_INTERVAL_HOURS)
        
        return SyncStatus(
            isEnabled = isEnabled,
            lastSyncTime = lastSyncTime,
            intervalHours = intervalHours,
            isRunning = isWorkRunning()
        )
    }
    
    /**
     * Update sync settings
     */
    fun updateSyncSettings(enabled: Boolean, intervalHours: Long) {
        Log.d(TAG, "⚙️ Updating sync settings: enabled=$enabled, interval=${intervalHours}h")
        
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("sync_enabled", enabled)
            .putLong("sync_interval_hours", intervalHours)
            .apply()
        
        if (enabled) {
            scheduleDailySync(intervalHours)
        } else {
            cancelDailySync()
        }
    }
    
    /**
     * Check if sync work is currently running
     */
    private fun isWorkRunning(): Boolean {
        val workInfos = workManager.getWorkInfosByTag("daily_sync").get()
        return workInfos.any { it.state == WorkInfo.State.RUNNING }
    }
    
    /**
     * Get work status for monitoring
     */
    fun getWorkStatus() = workManager.getWorkInfosByTag("daily_sync")
}

/**
 * Data class for sync status
 */
data class SyncStatus(
    val isEnabled: Boolean,
    val lastSyncTime: Long,
    val intervalHours: Long,
    val isRunning: Boolean
) {
    fun getLastSyncText(): String {
        if (lastSyncTime == 0L) return "Never"
        
        val now = System.currentTimeMillis()
        val diff = now - lastSyncTime
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000} minutes ago"
            diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
            else -> "${diff / 86_400_000} days ago"
        }
    }
    
    fun getNextSyncText(): String {
        if (!isEnabled) return "Disabled"
        if (lastSyncTime == 0L) return "Next sync scheduled"
        
        val nextSync = lastSyncTime + (intervalHours * 3_600_000)
        val now = System.currentTimeMillis()
        val diff = nextSync - now
        
        return if (diff <= 0) {
            "Due now"
        } else {
            val hours = diff / 3_600_000
            if (hours < 24) {
                "In $hours hours"
            } else {
                "In ${hours / 24} days"
            }
        }
    }
}
