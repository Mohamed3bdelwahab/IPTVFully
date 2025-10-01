package com.example.newiptv.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AppSettings Entity for storing application preferences
 * 
 * This entity stores various app settings including:
 * - Login credentials and preferences
 * - Player settings (speed, quality, etc.)
 * - Appearance settings (theme, colors, etc.)
 * - General app preferences
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1, // Single settings record
    
    // Login Settings
    val username: String? = null,
    val password: String? = null,
    val rememberCredentials: Boolean = false,
    val autoLogin: Boolean = false,
    
    // Player Settings
    val defaultPlaybackSpeed: Float = 1.0f,
    val rememberPlaybackSpeed: Boolean = true,
    val defaultVideoQuality: String = "auto",
    val autoPlayNext: Boolean = true,
    val rememberPosition: Boolean = true,
    val bufferSize: Int = 100, // MB
    
    // Appearance Settings
    val theme: String = "dark", // dark, light, auto
    val primaryColor: String = "#2196F3",
    val accentColor: String = "#FF4081",
    val fontSize: String = "medium", // small, medium, large
    val showSubtitles: Boolean = false,
    val subtitleSize: String = "medium",
    
    // General Settings
    val language: String = "en",
    val notifications: Boolean = true,
    val analytics: Boolean = false,
    val crashReporting: Boolean = true,
    val autoUpdate: Boolean = true,
    
    // Advanced Settings
    val debugMode: Boolean = false,
    val logLevel: String = "info", // debug, info, warn, error
    val cacheSize: Int = 500, // MB
    val networkTimeout: Int = 30, // seconds
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
