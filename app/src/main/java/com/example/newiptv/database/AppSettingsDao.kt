package com.example.newiptv.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for AppSettings
 * 
 * Provides methods to interact with app settings in the database
 */
@Dao
interface AppSettingsDao {
    
    /**
     * Get current app settings
     * Returns a Flow for reactive updates
     */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>
    
    /**
     * Get current app settings synchronously
     */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsSync(): AppSettings?
    
    /**
     * Insert or update app settings
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: AppSettings)
    
    /**
     * Update specific login settings
     */
    @Query("UPDATE app_settings SET username = :username, password = :password, rememberCredentials = :rememberCredentials, autoLogin = :autoLogin, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateLoginSettings(
        username: String?,
        password: String?,
        rememberCredentials: Boolean,
        autoLogin: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Update player settings
     */
    @Query("UPDATE app_settings SET defaultPlaybackSpeed = :speed, rememberPlaybackSpeed = :rememberSpeed, defaultVideoQuality = :quality, autoPlayNext = :autoPlay, rememberPosition = :rememberPos, bufferSize = :buffer, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updatePlayerSettings(
        speed: Float,
        rememberSpeed: Boolean,
        quality: String,
        autoPlay: Boolean,
        rememberPos: Boolean,
        buffer: Int,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Update appearance settings
     */
    @Query("UPDATE app_settings SET theme = :theme, primaryColor = :primaryColor, accentColor = :accentColor, fontSize = :fontSize, showSubtitles = :showSubtitles, subtitleSize = :subtitleSize, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateAppearanceSettings(
        theme: String,
        primaryColor: String,
        accentColor: String,
        fontSize: String,
        showSubtitles: Boolean,
        subtitleSize: String,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Update general settings
     */
    @Query("UPDATE app_settings SET language = :language, notifications = :notifications, analytics = :analytics, crashReporting = :crashReporting, autoUpdate = :autoUpdate, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateGeneralSettings(
        language: String,
        notifications: Boolean,
        analytics: Boolean,
        crashReporting: Boolean,
        autoUpdate: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Update advanced settings
     */
    @Query("UPDATE app_settings SET debugMode = :debugMode, logLevel = :logLevel, cacheSize = :cacheSize, networkTimeout = :networkTimeout, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateAdvancedSettings(
        debugMode: Boolean,
        logLevel: String,
        cacheSize: Int,
        networkTimeout: Int,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Clear all settings (reset to default)
     */
    @Query("DELETE FROM app_settings")
    suspend fun clearAllSettings()
    
    /**
     * Check if settings exist
     */
    @Query("SELECT COUNT(*) FROM app_settings WHERE id = 1")
    suspend fun settingsExist(): Int
}
