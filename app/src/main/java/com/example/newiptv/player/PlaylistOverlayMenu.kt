package com.example.newiptv.player

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.EpisodeEntity
import com.example.newiptv.ui.seriesinfo.EpisodesAdapter

/**
 * Playlist Overlay Menu for displaying current season episodes
 * Shows when Menu button is pressed on TV remote
 */
class PlaylistOverlayMenu(
    private val context: Context,
    private val episodes: List<EpisodeEntity>,
    private val currentEpisodeIndex: Int,
    private var onEpisodeSelected: ((EpisodeEntity, Int) -> Unit)?,
    private var onClose: (() -> Unit)?
) {
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var episodesAdapter: EpisodesAdapter? = null
    private var isVisible = false
    
    private val handler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable { hide() }
    
    companion object {
        private const val AUTO_HIDE_DELAY = 10000L // 10 seconds
    }
    
    fun show() {
        if (isVisible) return
        
        android.util.Log.d("PlaylistOverlayMenu", "=== SHOWING PLAYLIST OVERLAY ===")
        android.util.Log.d("PlaylistOverlayMenu", "Total episodes: ${episodes.size}")
        android.util.Log.d("PlaylistOverlayMenu", "Current episode index: $currentEpisodeIndex")
        
        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            // Inflate the overlay layout
            overlayView = LayoutInflater.from(context).inflate(R.layout.playlist_overlay_menu, null)
            
            // Setup UI components
            setupUI()
            
            // Setup window parameters
            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.CENTER
            }
            
            // Add the overlay to window
            windowManager?.addView(overlayView, params)
            isVisible = true
            
            // Schedule auto-hide
            scheduleAutoHide()
            
            android.util.Log.d("PlaylistOverlayMenu", "✅ Playlist overlay shown successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("PlaylistOverlayMenu", "❌ Error showing playlist overlay", e)
            e.printStackTrace()
        }
    }
    
    fun hide() {
        if (!isVisible) return
        
        android.util.Log.d("PlaylistOverlayMenu", "=== HIDING PLAYLIST OVERLAY ===")
        
        try {
            cancelAutoHide()
            
            overlayView?.let { view ->
                windowManager?.removeView(view)
            }
            
            overlayView = null
            windowManager = null
            isVisible = false
            
            onClose?.invoke()
            
            android.util.Log.d("PlaylistOverlayMenu", "✅ Playlist overlay hidden successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("PlaylistOverlayMenu", "❌ Error hiding playlist overlay", e)
            e.printStackTrace()
        }
    }
    
    fun destroy() {
        android.util.Log.d("PlaylistOverlayMenu", "=== DESTROYING PLAYLIST OVERLAY ===")
        
        hide()
        cancelAutoHide()
        
        episodesAdapter = null
        onEpisodeSelected = null
        onClose = null
    }
    
    private fun setupUI() {
        overlayView?.let { view ->
            // Setup header
            val headerText = view.findViewById<TextView>(R.id.tvPlaylistHeader)
            headerText.text = "Playlist (${episodes.size} episodes)"
            
            // Setup close button
            val closeButton = view.findViewById<View>(R.id.btnClosePlaylist)
            closeButton.setOnClickListener {
                hide()
            }
            
            // Setup episodes list
            val episodesRecyclerView = view.findViewById<RecyclerView>(R.id.rvEpisodesList)
            episodesRecyclerView.layoutManager = LinearLayoutManager(context)
            
            // Create and set adapter
            episodesAdapter = EpisodesAdapter { episode ->
                val index = episodes.indexOf(episode)
                if (index != -1) {
                    android.util.Log.d("PlaylistOverlayMenu", "Episode selected: ${episode.title} at index $index")
                    onEpisodeSelected?.invoke(episode, index)
                    hide()
                }
            }
            
            episodesRecyclerView.adapter = episodesAdapter
            episodesAdapter?.updateEpisodes(episodes)
            
            // Highlight current episode
            highlightCurrentEpisode()
            
            // Setup key event handling
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            
            view.setOnKeyListener { _, keyCode, event ->
                handleKeyEvent(event)
            }
        }
    }
    
    private fun highlightCurrentEpisode() {
        if (currentEpisodeIndex >= 0 && currentEpisodeIndex < episodes.size) {
            android.util.Log.d("PlaylistOverlayMenu", "Highlighting current episode at index: $currentEpisodeIndex")
            // The adapter will handle highlighting the current episode
        }
    }
    
    private fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        
        android.util.Log.d("PlaylistOverlayMenu", "Handling key event: ${event.keyCode}")
        
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE -> {
                hide()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                // Reset auto-hide timer on any navigation
                scheduleAutoHide()
                return false // Let the RecyclerView handle these events
            }
            else -> {
                // Reset auto-hide timer on any key press
                scheduleAutoHide()
                return false
            }
        }
    }
    
    private fun scheduleAutoHide() {
        cancelAutoHide()
        handler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY)
    }
    
    private fun cancelAutoHide() {
        handler.removeCallbacks(autoHideRunnable)
    }
}
