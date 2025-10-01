package com.example.newiptv

import android.app.Application
import android.util.Log
import com.example.newiptv.sync.SyncManager

/**
 * Application class for NewIPTV
 * Initializes sync manager and other global components
 */
class NewIPTVApplication : Application() {
    
    companion object {
        private const val TAG = "NewIPTVApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 NewIPTV Application starting...")
        
        // Initialize sync manager and schedule daily sync
        val syncManager = SyncManager(this)
        val syncStatus = syncManager.getSyncStatus()
        
        if (syncStatus.isEnabled) {
            Log.d(TAG, "📅 Scheduling daily sync (${syncStatus.intervalHours} hours)")
            syncManager.scheduleDailySync(syncStatus.intervalHours)
            
            // Trigger initial sync if never synced before
            if (syncStatus.lastSyncTime == 0L) {
                Log.d(TAG, "🚀 Triggering initial sync...")
                syncManager.triggerManualSync()
            }
        } else {
            Log.d(TAG, "⏸️ Daily sync is disabled")
        }
        
        Log.d(TAG, "✅ NewIPTV Application initialized")
    }
}
