package com.example.newiptv.player

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.example.newiptv.R
import android.util.Log

/**
 * Speed Overlay Menu for TV Remote Control
 * Provides visual speed control interface that only closes on back button
 * Based on comprehensive playback speed documentation
 */
class SpeedOverlayMenu(
    private val context: Context,
    private val exoPlayer: ExoPlayer? = null,
    private val onSpeedChanged: ((Float) -> Unit)? = null,
    private val onClose: (() -> Unit)? = null
) {
    
    companion object {
        private const val TAG = "SpeedOverlayMenu"
        private const val MIN_SPEED = 0.25f
        private const val MAX_SPEED = 3.0f
        private const val SPEED_INCREMENT = 0.25f
        
        // Speed presets mapping
        private val SPEED_PRESETS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    }
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var currentSpeed: Float = 1.0f
    private var isVisible = false
    
    // UI Components
    private var tvCurrentSpeed: TextView? = null
    private var btnSpeedDown: Button? = null
    private var btnSpeedUp: Button? = null
    
    /**
     * Show the speed overlay menu
     */
    fun show() {
        if (isVisible) return
        
        try {
            // Get current speed
            currentSpeed = exoPlayer?.playbackParameters?.speed ?: 1.0f
            
            // Create overlay view
            createOverlayView()
            
            // Add to window manager
            addToWindow()
            
            isVisible = true
            Log.d(TAG, "Speed overlay menu shown")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing speed overlay menu", e)
        }
    }
    
    /**
     * Hide the speed overlay menu
     */
    fun hide() {
        if (!isVisible) return
        
        try {
            removeFromWindow()
            isVisible = false
            Log.d(TAG, "Speed overlay menu hidden")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding speed overlay menu", e)
        }
    }
    
    /**
     * Check if menu is visible
     */
    fun isMenuVisible(): Boolean = isVisible
    
    /**
     * Handle key events for the overlay
     */
    fun handleKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!isVisible) return false
        
        if (keyEvent.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        
        Log.d(TAG, "Handling key event in speed menu: ${keyEvent.keyCode}")
        
        return when (keyEvent.keyCode) {
            // Speed adjustment
            KeyEvent.KEYCODE_DPAD_UP -> {
                adjustSpeed(SPEED_INCREMENT)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                adjustSpeed(-SPEED_INCREMENT)
                true
            }
            KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_NUMPAD_ADD -> {
                adjustSpeed(SPEED_INCREMENT)
                true
            }
            KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> {
                adjustSpeed(-SPEED_INCREMENT)
                true
            }
            
            // Speed presets
            KeyEvent.KEYCODE_0 -> {
                setSpeedPreset(0)
                true
            }
            KeyEvent.KEYCODE_1 -> {
                setSpeedPreset(1)
                true
            }
            KeyEvent.KEYCODE_2 -> {
                setSpeedPreset(2)
                true
            }
            KeyEvent.KEYCODE_3 -> {
                setSpeedPreset(3)
                true
            }
            KeyEvent.KEYCODE_4 -> {
                setSpeedPreset(4)
                true
            }
            KeyEvent.KEYCODE_5 -> {
                setSpeedPreset(5)
                true
            }
            KeyEvent.KEYCODE_6 -> {
                setSpeedPreset(6)
                true
            }
            
            // Navigation - only back button closes the menu
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                hide()
                onClose?.invoke()
                true
            }
            
            // Other keys don't close the menu
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                // Just consume the event, don't close menu
                true
            }
            
            else -> false
        }
    }
    
    /**
     * Create the overlay view
     */
    private fun createOverlayView() {
        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.speed_overlay_menu, null)
        
        // Initialize UI components
        initializeUI()
        
        // Set up click listeners
        setupClickListeners()
        
        // Set initial speed display
        updateSpeedDisplay()
    }
    
    /**
     * Initialize UI components
     */
    private fun initializeUI() {
        overlayView?.let { view ->
            tvCurrentSpeed = view.findViewById(R.id.tvCurrentSpeed)
            btnSpeedDown = view.findViewById(R.id.btnSpeedDown)
            btnSpeedUp = view.findViewById(R.id.btnSpeedUp)
        }
    }
    
    /**
     * Set up click listeners
     */
    private fun setupClickListeners() {
        // Speed adjustment buttons
        btnSpeedDown?.setOnClickListener {
            adjustSpeed(-SPEED_INCREMENT)
        }
        
        btnSpeedUp?.setOnClickListener {
            adjustSpeed(SPEED_INCREMENT)
        }
        
        // Add touch support for the entire overlay
        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // Handle touch events for speed control
                    val x = event.x
                    val viewWidth = overlayView?.width ?: 0
                    
                    when {
                        x < viewWidth * 0.33 -> adjustSpeed(-SPEED_INCREMENT)  // Left third: decrease
                        x > viewWidth * 0.66 -> adjustSpeed(SPEED_INCREMENT)   // Right third: increase
                        // Middle third: no action (current speed display)
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Add overlay to window
     */
    private fun addToWindow() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val params = WindowManager.LayoutParams().apply {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.BOTTOM or Gravity.START
        }
        
        windowManager?.addView(overlayView, params)
    }
    
    /**
     * Remove overlay from window
     */
    private fun removeFromWindow() {
        overlayView?.let { view ->
            windowManager?.removeView(view)
            overlayView = null
        }
    }
    
    /**
     * Adjust speed by delta
     */
    private fun adjustSpeed(delta: Float) {
        val newSpeed = (currentSpeed + delta).coerceIn(MIN_SPEED, MAX_SPEED)
        setSpeed(newSpeed)
    }
    
    /**
     * Set speed preset
     */
    private fun setSpeedPreset(presetIndex: Int) {
        if (presetIndex in SPEED_PRESETS.indices) {
            val speed = SPEED_PRESETS[presetIndex]
            setSpeed(speed)
        }
    }
    
    /**
     * Set speed directly
     */
    private fun setSpeed(speed: Float) {
        currentSpeed = speed
        
        // Apply to ExoPlayer
        exoPlayer?.setPlaybackParameters(PlaybackParameters(speed))
        
        // Update UI
        updateSpeedDisplay()
        
        // Notify callback
        onSpeedChanged?.invoke(speed)
        
        Log.d(TAG, "Speed changed to: ${speed}x")
    }
    
    /**
     * Update speed display
     */
    private fun updateSpeedDisplay() {
        tvCurrentSpeed?.text = "${currentSpeed}x"
    }
    

    
    /**
     * Get current speed
     */
    fun getCurrentSpeed(): Float = currentSpeed
    
    /**
     * Get speed presets
     */
    fun getSpeedPresets(): List<Float> = SPEED_PRESETS
    
    /**
     * Clean up resources
     */
    fun destroy() {
        hide()
        windowManager = null
    }
}
