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
    private var currentFocusIndex = 0
    
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
            
            // Setup window parameters - Small size in top right corner
            val params = WindowManager.LayoutParams().apply {
                width = 350 // Smaller width for compact menu
                height = 400 // Smaller height for compact menu
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.END // Top right corner
                x = 30 // Smaller margin from right edge
                y = 80 // Smaller margin from top
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
            
            // Set initial focus to current episode
            currentFocusIndex = currentEpisodeIndex.coerceIn(0, episodes.size - 1)
            android.util.Log.d("PlaylistOverlayMenu", "Initial focus index: $currentFocusIndex")
            
            // Set focus to the current episode
            episodesRecyclerView.post {
                val viewHolder = episodesRecyclerView.findViewHolderForAdapterPosition(currentFocusIndex)
                viewHolder?.itemView?.requestFocus()
                android.util.Log.d("PlaylistOverlayMenu", "Set focus to episode at index: $currentFocusIndex")
            }
            
            // Highlight current episode
            highlightCurrentEpisode()
            
            // Setup key event handling - similar to SpeedOverlayMenu
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
        }
    }
    
    /**
     * Check if menu is visible
     */
    fun isMenuVisible(): Boolean = isVisible
    
    /**
     * Handle key events for the overlay - called from VideoPlayerActivity
     */
    fun handleKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!isVisible) return false
        
        if (keyEvent.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        
        android.util.Log.d("PlaylistOverlayMenu", "Handling key event in playlist menu: ${keyEvent.keyCode}")
        
        return when (keyEvent.keyCode) {
            // Navigation within playlist
            KeyEvent.KEYCODE_DPAD_UP -> {
                android.util.Log.d("PlaylistOverlayMenu", "D-pad UP pressed - navigating up")
                navigateUp()
                scheduleAutoHide()
                true // Consume the event
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                android.util.Log.d("PlaylistOverlayMenu", "D-pad DOWN pressed - navigating down")
                navigateDown()
                scheduleAutoHide()
                true // Consume the event
            }
            
            // Episode selection
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                android.util.Log.d("PlaylistOverlayMenu", "Enter/Center pressed - episode selection")
                selectCurrentEpisode()
                scheduleAutoHide()
                true // Consume the event
            }
            
            // Close menu only on back button
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE -> {
                android.util.Log.d("PlaylistOverlayMenu", "Back/Escape pressed - hiding playlist")
                hide()
                onClose?.invoke()
                true
            }
            
            // Other keys reset auto-hide timer but don't close menu
            else -> {
                android.util.Log.d("PlaylistOverlayMenu", "Other key pressed: ${keyEvent.keyCode} - resetting timer")
                scheduleAutoHide()
                true // Consume other keys to prevent player interference
            }
        }
    }
    
    private fun highlightCurrentEpisode() {
        if (currentEpisodeIndex >= 0 && currentEpisodeIndex < episodes.size) {
            android.util.Log.d("PlaylistOverlayMenu", "Highlighting current episode at index: $currentEpisodeIndex")
            // The adapter will handle highlighting the current episode
        }
    }
    
    private fun navigateUp() {
        if (currentFocusIndex > 0) {
            currentFocusIndex--
            updateFocus()
            android.util.Log.d("PlaylistOverlayMenu", "Navigated UP to index: $currentFocusIndex")
        }
    }
    
    private fun navigateDown() {
        if (currentFocusIndex < episodes.size - 1) {
            currentFocusIndex++
            updateFocus()
            android.util.Log.d("PlaylistOverlayMenu", "Navigated DOWN to index: $currentFocusIndex")
        }
    }
    
    private fun updateFocus() {
        overlayView?.let { view ->
            val episodesRecyclerView = view.findViewById<RecyclerView>(R.id.rvEpisodesList)
            episodesRecyclerView.post {
                val viewHolder = episodesRecyclerView.findViewHolderForAdapterPosition(currentFocusIndex)
                viewHolder?.itemView?.requestFocus()
                android.util.Log.d("PlaylistOverlayMenu", "Updated focus to episode at index: $currentFocusIndex")
            }
        }
    }
    
    private fun selectCurrentEpisode() {
        if (currentFocusIndex in episodes.indices) {
            val episode = episodes[currentFocusIndex]
            android.util.Log.d("PlaylistOverlayMenu", "Selecting episode: ${episode.title} at index: $currentFocusIndex")
            onEpisodeSelected?.invoke(episode, currentFocusIndex)
            hide()
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
