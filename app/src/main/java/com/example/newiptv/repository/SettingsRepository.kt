package com.example.newiptv.repository

import com.example.newiptv.database.AppSettings
import com.example.newiptv.database.AppSettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository for managing app settings
 * 
 * Provides a clean interface for accessing and updating app settings
 * Handles default values and settings initialization
 */
class SettingsRepository(
    private val settingsDao: AppSettingsDao
) {
    
    /**
     * Get current app settings as Flow
     */
    fun getSettings(): Flow<AppSettings?> = settingsDao.getSettings()
    
    /**
     * Get current app settings synchronously
     */
    suspend fun getSettingsSync(): AppSettings {
        return settingsDao.getSettingsSync() ?: getDefaultSettings()
    }
    
    /**
     * Update entire settings object
     */
    suspend fun updateSettings(settings: AppSettings) {
        settingsDao.insertOrUpdateSettings(settings)
    }
    
    /**
     * Update login settings
     */
    suspend fun updateLoginSettings(
        username: String?,
        password: String?,
        rememberCredentials: Boolean,
        autoLogin: Boolean
    ) {
        settingsDao.updateLoginSettings(username, password, rememberCredentials, autoLogin)
    }
    
    /**
     * Update player settings
     */
    suspend fun updatePlayerSettings(
        speed: Float,
        rememberSpeed: Boolean,
        quality: String,
        autoPlay: Boolean,
        rememberPos: Boolean,
        buffer: Int
    ) {
        settingsDao.updatePlayerSettings(speed, rememberSpeed, quality, autoPlay, rememberPos, buffer)
    }
    
    /**
     * Update appearance settings
     */
    suspend fun updateAppearanceSettings(
        theme: String,
        primaryColor: String,
        accentColor: String,
        fontSize: String,
        showSubtitles: Boolean,
        subtitleSize: String
    ) {
        settingsDao.updateAppearanceSettings(theme, primaryColor, accentColor, fontSize, showSubtitles, subtitleSize)
    }
    
    /**
     * Update general settings
     */
    suspend fun updateGeneralSettings(
        language: String,
        notifications: Boolean,
        analytics: Boolean,
        crashReporting: Boolean,
        autoUpdate: Boolean
    ) {
        settingsDao.updateGeneralSettings(language, notifications, analytics, crashReporting, autoUpdate)
    }
    
    /**
     * Update advanced settings
     */
    suspend fun updateAdvancedSettings(
        debugMode: Boolean,
        logLevel: String,
        cacheSize: Int,
        networkTimeout: Int
    ) {
        settingsDao.updateAdvancedSettings(debugMode, logLevel, cacheSize, networkTimeout)
    }
    
    /**
     * Get default playback speed
     */
    suspend fun getDefaultPlaybackSpeed(): Float {
        return getSettingsSync().defaultPlaybackSpeed
    }
    
    /**
     * Set default playback speed
     */
    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        val currentSettings = getSettingsSync()
        updatePlayerSettings(
            speed = speed,
            rememberSpeed = currentSettings.rememberPlaybackSpeed,
            quality = currentSettings.defaultVideoQuality,
            autoPlay = currentSettings.autoPlayNext,
            rememberPos = currentSettings.rememberPosition,
            buffer = currentSettings.bufferSize
        )
    }
    
    /**
     * Check if should remember playback speed
     */
    suspend fun shouldRememberPlaybackSpeed(): Boolean {
        return getSettingsSync().rememberPlaybackSpeed
    }
    
    /**
     * Set remember playback speed preference
     */
    suspend fun setRememberPlaybackSpeed(remember: Boolean) {
        val currentSettings = getSettingsSync()
        updatePlayerSettings(
            speed = currentSettings.defaultPlaybackSpeed,
            rememberSpeed = remember,
            quality = currentSettings.defaultVideoQuality,
            autoPlay = currentSettings.autoPlayNext,
            rememberPos = currentSettings.rememberPosition,
            buffer = currentSettings.bufferSize
        )
    }
    
    /**
     * Initialize default settings if none exist
     */
    suspend fun initializeDefaultSettings() {
        if (settingsDao.settingsExist() == 0) {
            settingsDao.insertOrUpdateSettings(getDefaultSettings())
        }
    }
    
    /**
     * Reset all settings to default
     */
    suspend fun resetToDefaults() {
        settingsDao.clearAllSettings()
        settingsDao.insertOrUpdateSettings(getDefaultSettings())
    }
    
    /**
     * Get default settings
     */
    private fun getDefaultSettings(): AppSettings {
        return AppSettings(
            id = 1,
            username = null,
            password = null,
            rememberCredentials = false,
            autoLogin = false,
            defaultPlaybackSpeed = 1.0f,
            rememberPlaybackSpeed = true,
            defaultVideoQuality = "auto",
            autoPlayNext = true,
            rememberPosition = true,
            bufferSize = 100,
            theme = "dark",
            primaryColor = "#2196F3",
            accentColor = "#FF4081",
            fontSize = "medium",
            showSubtitles = false,
            subtitleSize = "medium",
            language = "en",
            notifications = true,
            analytics = false,
            crashReporting = true,
            autoUpdate = true,
            debugMode = false,
            logLevel = "info",
            cacheSize = 500,
            networkTimeout = 30,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
