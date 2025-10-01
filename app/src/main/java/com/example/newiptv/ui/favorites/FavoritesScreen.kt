package com.example.newiptv.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.repository.FavoritePlaylistRepository
import com.example.newiptv.databinding.ActivityFavoritesScreenBinding
import com.example.newiptv.ui.movieinfo.MovieInfoScreen
import com.example.newiptv.ui.seriesinfo.SeriesInfoScreen
import kotlinx.coroutines.launch

/**
 * Favorites Screen - Manage favorite playlists
 * Allows users to create, view, and manage their favorite playlists
 */
class FavoritesScreen : AppCompatActivity() {
    
    private lateinit var binding: ActivityFavoritesScreenBinding
    private lateinit var favoritePlaylistRepository: FavoritePlaylistRepository
    private lateinit var playlistAdapter: PlaylistAdapter
    
    private var currentFocusedPosition = 0
    private var isCreatingPlaylist = false
    private var isCreateButtonFocused = false
    
    companion object {
        private const val TAG = "FavoritesScreen"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize repository
        val database = DatabaseProvider.getDatabase(this)
        favoritePlaylistRepository = FavoritePlaylistRepository(database.favoritePlaylistDao())
        
        // Initialize default playlists if needed
        lifecycleScope.launch {
            favoritePlaylistRepository.createDefaultPlaylistsIfNeeded()
        }
        
        setupUI()
        setupRecyclerView()
        loadPlaylists()
    }
    
    private fun setupUI() {
        // Set up title
        binding.titleText.text = "My Favorites"
        
        // Set up create playlist button
        binding.createPlaylistButton.setOnClickListener {
            showCreatePlaylistDialog()
        }
        
        // Make create playlist button focusable for TV remote
        binding.createPlaylistButton.isFocusable = true
        binding.createPlaylistButton.isFocusableInTouchMode = true
        
        // Add focus change listener for visual feedback
        binding.createPlaylistButton.setOnFocusChangeListener { _, hasFocus ->
            isCreateButtonFocused = hasFocus
            if (hasFocus) {
                Log.d(TAG, "🎯 Create Playlist button focused")
                binding.createPlaylistButton.background = getDrawable(R.drawable.button_focused_background)
            } else {
                binding.createPlaylistButton.background = getDrawable(R.drawable.button_background)
            }
        }
        
        // Set up back button
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // Set up empty state
        binding.emptyStateLayout.visibility = View.GONE
        binding.emptyStateText.text = "No playlists yet. Create your first playlist!"
        binding.createFirstPlaylistButton.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }
    
    private fun setupRecyclerView() {
        playlistAdapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                openPlaylistDetails(playlist.id)
            },
            onPlaylistLongClick = { playlist ->
                showPlaylistOptionsDialog(playlist)
            }
        )
        
        binding.playlistsRecyclerView.apply {
            adapter = playlistAdapter
            layoutManager = GridLayoutManager(this@FavoritesScreen, 2)
            
            // Add item decoration for spacing
            addItemDecoration(PlaylistItemDecoration(16))
            
            // Enable focus for TV remote navigation
            isFocusable = true
            isFocusableInTouchMode = true
        }
        
        // Set initial focus on create button
        binding.createPlaylistButton.post {
            binding.createPlaylistButton.requestFocus()
            isCreateButtonFocused = true
        }
    }
    
    private fun loadPlaylists() {
        lifecycleScope.launch {
            try {
                favoritePlaylistRepository.getAllPlaylists().collect { playlists ->
                    if (playlists.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        playlistAdapter.submitList(playlists)
                        Log.d(TAG, "✅ Loaded ${playlists.size} playlists")
                        
                        // Don't override focus if create button is focused
                        if (!isCreateButtonFocused) {
                            binding.playlistsRecyclerView.post {
                                if (playlists.isNotEmpty()) {
                                    currentFocusedPosition = 0
                                    playlistAdapter.setFocusedPosition(0)
                                    binding.playlistsRecyclerView.requestFocus()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load playlists", e)
                showErrorState("Failed to load playlists")
            }
        }
    }
    
    private fun createDefaultPlaylistsIfNeeded() {
        lifecycleScope.launch {
            try {
                favoritePlaylistRepository.createDefaultPlaylistsIfNeeded()
                Log.d(TAG, "✅ Default playlists created if needed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create default playlists", e)
            }
        }
    }
    
    private fun showCreatePlaylistDialog() {
        if (isCreatingPlaylist) return
        isCreatingPlaylist = true
        
        val input = android.widget.EditText(this).apply {
            hint = "Enter playlist name"
            setPadding(32, 16, 32, 16)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Create New Playlist")
            .setMessage("Enter a name for your new playlist:")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createPlaylist(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .setOnDismissListener {
                isCreatingPlaylist = false
            }
            .show()
    }
    
    private fun createPlaylist(name: String) {
        lifecycleScope.launch {
            try {
                // Check if playlist with this name already exists
                val existingPlaylist = favoritePlaylistRepository.getPlaylistByName(name)
                if (existingPlaylist != null) {
                    showErrorDialog("Playlist with this name already exists")
                    return@launch
                }
                
                val playlist = favoritePlaylistRepository.createPlaylist(name)
                Log.d(TAG, "✅ Created playlist: ${playlist.name}")
                
                // Refresh the list
                loadPlaylists()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create playlist", e)
                showErrorDialog("Failed to create playlist")
            }
        }
    }
    
    private fun showPlaylistOptionsDialog(playlist: com.example.newiptv.data.db.entities.FavoritePlaylistEntity) {
        val options = arrayOf("View Contents", "Rename", "Delete")
        
        AlertDialog.Builder(this)
            .setTitle("Playlist Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openPlaylistDetails(playlist.id)
                    1 -> showRenamePlaylistDialog(playlist)
                    2 -> showDeletePlaylistDialog(playlist)
                }
            }
            .show()
    }
    
    private fun showRenamePlaylistDialog(playlist: com.example.newiptv.data.db.entities.FavoritePlaylistEntity) {
        val input = android.widget.EditText(this).apply {
            setText(playlist.name)
            setPadding(32, 16, 32, 16)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Rename Playlist")
            .setMessage("Enter new name for '${playlist.name}':")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != playlist.name) {
                    renamePlaylist(playlist, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun renamePlaylist(playlist: com.example.newiptv.data.db.entities.FavoritePlaylistEntity, newName: String) {
        lifecycleScope.launch {
            try {
                // Check if playlist with this name already exists
                val existingPlaylist = favoritePlaylistRepository.getPlaylistByName(newName)
                if (existingPlaylist != null && existingPlaylist.id != playlist.id) {
                    showErrorDialog("Playlist with this name already exists")
                    return@launch
                }
                
                val updatedPlaylist = playlist.copy(name = newName)
                favoritePlaylistRepository.updatePlaylist(updatedPlaylist)
                Log.d(TAG, "✅ Renamed playlist: ${playlist.name} -> $newName")
                
                // Refresh the list
                loadPlaylists()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to rename playlist", e)
                showErrorDialog("Failed to rename playlist")
            }
        }
    }
    
    private fun showDeletePlaylistDialog(playlist: com.example.newiptv.data.db.entities.FavoritePlaylistEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Playlist")
            .setMessage("Are you sure you want to delete '${playlist.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePlaylist(playlist)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deletePlaylist(playlist: com.example.newiptv.data.db.entities.FavoritePlaylistEntity) {
        lifecycleScope.launch {
            try {
                favoritePlaylistRepository.deletePlaylist(playlist.id)
                Log.d(TAG, "✅ Deleted playlist: ${playlist.name}")
                
                // Refresh the list
                loadPlaylists()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to delete playlist", e)
                showErrorDialog("Failed to delete playlist")
            }
        }
    }
    
    private fun openPlaylistDetails(playlistId: String) {
        val intent = Intent(this, PlaylistDetailsActivity::class.java)
        intent.putExtra("playlist_id", playlistId)
        startActivity(intent)
    }
    
    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.playlistsRecyclerView.visibility = View.GONE
    }
    
    private fun hideEmptyState() {
        binding.emptyStateLayout.visibility = View.GONE
        binding.playlistsRecyclerView.visibility = View.VISIBLE
    }
    
    private fun showErrorState(message: String) {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.playlistsRecyclerView.visibility = View.GONE
        binding.emptyStateText.text = message
    }
    
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    // 🔹 TV Remote Navigation Support
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    navigateUp()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    navigateDown()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    navigateLeft()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    navigateRight()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    selectCurrentItem()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    finish()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
    
    private fun navigateUp() {
        if (isCreateButtonFocused) {
            // Already at top, stay on create button
            return
        }
        
        val layoutManager = binding.playlistsRecyclerView.layoutManager as? GridLayoutManager
        if (layoutManager != null) {
            val spanCount = layoutManager.spanCount
            val newPosition = (currentFocusedPosition - spanCount).coerceAtLeast(0)
            if (newPosition != currentFocusedPosition) {
                Log.d(TAG, "🔼 Navigating UP: $currentFocusedPosition -> $newPosition")
                currentFocusedPosition = newPosition
                updateFocus()
            } else {
                // Move to create button if at top of playlist grid
                Log.d(TAG, "🔼 Moving to Create Playlist button")
                moveToCreateButton()
            }
        }
    }
    
    private fun navigateDown() {
        if (isCreateButtonFocused) {
            // Move to first playlist
            Log.d(TAG, "🔽 Moving from Create button to first playlist")
            moveToFirstPlaylist()
        } else {
            val layoutManager = binding.playlistsRecyclerView.layoutManager as? GridLayoutManager
            if (layoutManager != null) {
                val spanCount = layoutManager.spanCount
                val itemCount = playlistAdapter.itemCount
                val newPosition = (currentFocusedPosition + spanCount).coerceAtMost(itemCount - 1)
                if (newPosition != currentFocusedPosition) {
                    Log.d(TAG, "🔽 Navigating DOWN: $currentFocusedPosition -> $newPosition")
                    currentFocusedPosition = newPosition
                    updateFocus()
                }
            }
        }
    }
    
    private fun navigateLeft() {
        if (isCreateButtonFocused) {
            // Stay on create button
            return
        }
        
        if (currentFocusedPosition > 0) {
            Log.d(TAG, "⬅️ Navigating LEFT: $currentFocusedPosition -> ${currentFocusedPosition - 1}")
            currentFocusedPosition--
            updateFocus()
        }
    }
    
    private fun navigateRight() {
        if (isCreateButtonFocused) {
            // Stay on create button
            return
        }
        
        val itemCount = playlistAdapter.itemCount
        if (currentFocusedPosition < itemCount - 1) {
            Log.d(TAG, "➡️ Navigating RIGHT: $currentFocusedPosition -> ${currentFocusedPosition + 1}")
            currentFocusedPosition++
            updateFocus()
        }
    }
    
    private fun updateFocus() {
        val layoutManager = binding.playlistsRecyclerView.layoutManager as? GridLayoutManager
        if (layoutManager != null) {
            Log.d(TAG, "🎯 Updating focus to position: $currentFocusedPosition")
            layoutManager.scrollToPosition(currentFocusedPosition)
            // Update adapter focus
            playlistAdapter.setFocusedPosition(currentFocusedPosition)
        }
    }
    
    private fun moveToCreateButton() {
        isCreateButtonFocused = true
        binding.createPlaylistButton.requestFocus()
        Log.d(TAG, "🎯 Moved focus to Create Playlist button")
    }
    
    private fun moveToFirstPlaylist() {
        if (playlistAdapter.itemCount > 0) {
            isCreateButtonFocused = false
            currentFocusedPosition = 0
            playlistAdapter.setFocusedPosition(0)
            binding.playlistsRecyclerView.requestFocus()
            Log.d(TAG, "🎯 Moved focus to first playlist")
        }
    }
    
    private fun selectCurrentItem() {
        if (isCreateButtonFocused) {
            Log.d(TAG, "✅ Selecting Create Playlist button")
            showCreatePlaylistDialog()
        } else if (currentFocusedPosition >= 0 && currentFocusedPosition < playlistAdapter.itemCount) {
            val playlist = playlistAdapter.getItemAt(currentFocusedPosition)
            if (playlist != null) {
                Log.d(TAG, "✅ Selecting playlist: ${playlist.name} at position $currentFocusedPosition")
                openPlaylistDetails(playlist.id)
            }
        } else {
            Log.d(TAG, "❌ Invalid position for selection: $currentFocusedPosition (itemCount: ${playlistAdapter.itemCount})")
        }
    }
}
