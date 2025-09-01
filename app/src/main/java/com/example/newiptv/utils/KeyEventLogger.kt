package com.example.newiptv.utils

import android.util.Log
import android.view.KeyEvent

/**
 * Comprehensive Key Event Logger for monitoring all button presses
 * Tracks all key events, navigation, and user interactions
 */
object KeyEventLogger {
    
    private const val TAG = "KeyEventLogger"
    private const val LOG_PREFIX = "🎮"
    
    /**
     * Log all key events with detailed information
     */
    fun logKeyEvent(screenName: String, event: KeyEvent, additionalInfo: String = "") {
        val keyCodeName = getKeyCodeName(event.keyCode)
        val action = if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"
        val timestamp = System.currentTimeMillis()
        
        Log.d(TAG, "$LOG_PREFIX [$screenName] Key Event: $keyCodeName (${event.keyCode}) - $action")
        Log.d(TAG, "$LOG_PREFIX [$screenName] Timestamp: $timestamp")
        Log.d(TAG, "$LOG_PREFIX [$screenName] Additional Info: $additionalInfo")
        
        // Log special events with more detail
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                Log.i(TAG, "$LOG_PREFIX [$screenName] 🎯 OK/ENTER BUTTON PRESSED!")
                Log.i(TAG, "$LOG_PREFIX [$screenName] This should trigger item selection")
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                Log.d(TAG, "$LOG_PREFIX [$screenName] 📍 Navigation: ${if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) "UP" else "DOWN"}")
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                Log.d(TAG, "$LOG_PREFIX [$screenName] 📍 Navigation: ${if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) "LEFT" else "RIGHT"}")
            }
            KeyEvent.KEYCODE_BACK -> {
                Log.w(TAG, "$LOG_PREFIX [$screenName] ⬅️ BACK BUTTON PRESSED!")
            }
        }
    }
    
    /**
     * Log item selection events
     */
    fun logItemSelection(screenName: String, itemType: String, position: Int, itemName: String) {
        Log.i(TAG, "$LOG_PREFIX [$screenName] 🎯 ITEM SELECTED: $itemType at position $position")
        Log.i(TAG, "$LOG_PREFIX [$screenName] Item Name: $itemName")
        Log.i(TAG, "$LOG_PREFIX [$screenName] Selection triggered by OK/ENTER button")
    }
    
    /**
     * Log focus changes
     */
    fun logFocusChange(screenName: String, panelName: String, position: Int) {
        Log.d(TAG, "$LOG_PREFIX [$screenName] 📍 FOCUS CHANGED: $panelName at position $position")
    }
    
    /**
     * Log navigation events
     */
    fun logNavigation(screenName: String, direction: String, fromPanel: String, toPanel: String? = null) {
        Log.d(TAG, "$LOG_PREFIX [$screenName] 🧭 NAVIGATION: $direction")
        Log.d(TAG, "$LOG_PREFIX [$screenName] From Panel: $fromPanel")
        if (toPanel != null) {
            Log.d(TAG, "$LOG_PREFIX [$screenName] To Panel: $toPanel")
        }
    }
    
    /**
     * Log data loading events
     */
    fun logDataLoading(screenName: String, dataType: String, trigger: String) {
        Log.i(TAG, "$LOG_PREFIX [$screenName] 📥 DATA LOADING: $dataType")
        Log.i(TAG, "$LOG_PREFIX [$screenName] Trigger: $trigger")
    }
    
    /**
     * Log error events
     */
    fun logError(screenName: String, error: String, details: String = "") {
        Log.e(TAG, "$LOG_PREFIX [$screenName] ❌ ERROR: $error")
        if (details.isNotEmpty()) {
            Log.e(TAG, "$LOG_PREFIX [$screenName] Details: $details")
        }
    }
    
    /**
     * Get human-readable key code name
     */
    private fun getKeyCodeName(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> "DPAD_LEFT"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "DPAD_RIGHT"
            KeyEvent.KEYCODE_DPAD_UP -> "DPAD_UP"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DPAD_DOWN"
            KeyEvent.KEYCODE_DPAD_CENTER -> "DPAD_CENTER"
            KeyEvent.KEYCODE_ENTER -> "ENTER"
            KeyEvent.KEYCODE_BACK -> "BACK"
            KeyEvent.KEYCODE_MENU -> "MENU"
            KeyEvent.KEYCODE_HOME -> "HOME"
            KeyEvent.KEYCODE_INFO -> "INFO"
            KeyEvent.KEYCODE_MEDIA_PLAY -> "MEDIA_PLAY"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "MEDIA_PAUSE"
            KeyEvent.KEYCODE_MEDIA_NEXT -> "MEDIA_NEXT"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "MEDIA_PREVIOUS"
            KeyEvent.KEYCODE_VOLUME_UP -> "VOLUME_UP"
            KeyEvent.KEYCODE_VOLUME_DOWN -> "VOLUME_DOWN"
            KeyEvent.KEYCODE_MUTE -> "MUTE"
            else -> "UNKNOWN($keyCode)"
        }
    }
    
    /**
     * Log screen lifecycle events
     */
    fun logScreenEvent(screenName: String, event: String) {
        Log.i(TAG, "$LOG_PREFIX [$screenName] 🖥️ SCREEN EVENT: $event")
    }
    
    /**
     * Log user interaction summary
     */
    fun logInteractionSummary(screenName: String, summary: String) {
        Log.i(TAG, "$LOG_PREFIX [$screenName] 📊 INTERACTION SUMMARY: $summary")
    }
}
