package com.example.newiptv.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.repository.FavoritePlaylistRepository
import com.example.newiptv.databinding.ActivityPlaylistDetailsBinding
import com.example.newiptv.ui.movieinfo.MovieInfoScreen
import com.example.newiptv.ui.seriesinfo.SeriesInfoScreen
import kotlinx.coroutines.launch

/**
 * Playlist Details Activity - Show contents of a specific playlist
 */
class PlaylistDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPlaylistDetailsBinding
    private lateinit var favoritePlaylistRepository: FavoritePlaylistRepository
    private lateinit var playlistItemsAdapter: PlaylistItemsAdapter
    
    private var playlistId: String? = null
    private var playlistName: String? = null
    private var currentFocusedPosition = 0
    
    companion object {
        private const val TAG = "PlaylistDetailsActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get playlist ID from intent
        playlistId = intent.getStringExtra("playlist_id")
        if (playlistId == null) {
            Log.e(TAG, "❌ No playlist ID provided")
            finish()
            return
        }
        
        // Initialize repository
        val database = DatabaseProvider.getDatabase(this)
        favoritePlaylistRepository = FavoritePlaylistRepository(database.favoritePlaylistDao())
        
        setupUI()
        setupRecyclerView()
        loadPlaylistInfo()
        loadPlaylistItems()
    }
    
    private fun setupUI() {
        // Set up back button
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // Set up empty state
        binding.emptyStateLayout.visibility = View.GONE
        binding.emptyStateText.text = "This playlist is empty. Add some content!"
        
        // Set up menu button
        binding.menuButton.setOnClickListener {
            showPlaylistMenu()
        }
    }
    
    private fun setupRecyclerView() {
        playlistItemsAdapter = PlaylistItemsAdapter(
            onItemClick = { item ->
                openContent(item)
            },
            onItemLongClick = { item ->
                showRemoveItemDialog(item)
            }
        )
        
        binding.itemsRecyclerView.apply {
            adapter = playlistItemsAdapter
            layoutManager = LinearLayoutManager(this@PlaylistDetailsActivity)
        }
    }
    
    private fun loadPlaylistInfo() {
        lifecycleScope.launch {
            try {
                val playlist = favoritePlaylistRepository.getPlaylistById(playlistId!!)
                if (playlist != null) {
                    playlistName = playlist.name
                    binding.titleText.text = playlist.name
                    binding.subtitleText.text = "${playlist.itemCount} items"
                } else {
                    Log.e(TAG, "❌ Playlist not found: $playlistId")
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load playlist info", e)
                finish()
            }
        }
    }
    
    private fun loadPlaylistItems() {
        lifecycleScope.launch {
            try {
                favoritePlaylistRepository.getPlaylistItemsFlow(playlistId!!).collect { items ->
                    if (items.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        playlistItemsAdapter.submitList(items)
                        Log.d(TAG, "✅ Loaded ${items.size} playlist items")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load playlist items", e)
                showErrorState("Failed to load playlist items")
            }
        }
    }
    
    private fun openContent(item: com.example.newiptv.data.db.entities.FavoritePlaylistItemEntity) {
        when (item.contentType) {
            "movie" -> {
                val intent = Intent(this, MovieInfoScreen::class.java)
                intent.putExtra("movie_id", item.contentId)
                startActivity(intent)
            }
            "series" -> {
                val intent = Intent(this, SeriesInfoScreen::class.java)
                intent.putExtra("series_id", item.contentId)
                startActivity(intent)
            }
            "episode" -> {
                val intent = Intent(this, SeriesInfoScreen::class.java)
                intent.putExtra("series_id", item.seriesId ?: item.contentId)
                startActivity(intent)
            }
        }
    }
    
    private fun showRemoveItemDialog(item: com.example.newiptv.data.db.entities.FavoritePlaylistItemEntity) {
        AlertDialog.Builder(this)
            .setTitle("Remove Item")
            .setMessage("Remove '${item.title}' from this playlist?")
            .setPositiveButton("Remove") { _, _ ->
                removeItemFromPlaylist(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun removeItemFromPlaylist(item: com.example.newiptv.data.db.entities.FavoritePlaylistItemEntity) {
        lifecycleScope.launch {
            try {
                favoritePlaylistRepository.removeItemFromPlaylist(
                    playlistId!!,
                    item.contentId,
                    item.contentType
                )
                Log.d(TAG, "✅ Removed item from playlist: ${item.title}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to remove item from playlist", e)
            }
        }
    }
    
    private fun showPlaylistMenu() {
        val options = arrayOf("Rename Playlist", "Delete Playlist", "Clear All Items")
        
        AlertDialog.Builder(this)
            .setTitle("Playlist Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenamePlaylistDialog()
                    1 -> showDeletePlaylistDialog()
                    2 -> showClearPlaylistDialog()
                }
            }
            .show()
    }
    
    private fun showRenamePlaylistDialog() {
        val input = android.widget.EditText(this).apply {
            setText(playlistName)
            setPadding(32, 16, 32, 16)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Rename Playlist")
            .setMessage("Enter new name for '${playlistName}':")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != playlistName) {
                    renamePlaylist(newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun renamePlaylist(newName: String) {
        lifecycleScope.launch {
            try {
                val playlist = favoritePlaylistRepository.getPlaylistById(playlistId!!)
                if (playlist != null) {
                    val updatedPlaylist = playlist.copy(name = newName)
                    favoritePlaylistRepository.updatePlaylist(updatedPlaylist)
                    playlistName = newName
                    binding.titleText.text = newName
                    Log.d(TAG, "✅ Renamed playlist: ${playlist.name} -> $newName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to rename playlist", e)
            }
        }
    }
    
    private fun showDeletePlaylistDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Playlist")
            .setMessage("Are you sure you want to delete '${playlistName}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePlaylist()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deletePlaylist() {
        lifecycleScope.launch {
            try {
                favoritePlaylistRepository.deletePlaylist(playlistId!!)
                Log.d(TAG, "✅ Deleted playlist: $playlistName")
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to delete playlist", e)
            }
        }
    }
    
    private fun showClearPlaylistDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Playlist")
            .setMessage("Remove all items from '${playlistName}'?")
            .setPositiveButton("Clear") { _, _ ->
                clearPlaylist()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearPlaylist() {
        lifecycleScope.launch {
            try {
                val items = favoritePlaylistRepository.getPlaylistItems(playlistId!!)
                items.forEach { item ->
                    favoritePlaylistRepository.removeItemFromPlaylist(
                        playlistId!!,
                        item.contentId,
                        item.contentType
                    )
                }
                Log.d(TAG, "✅ Cleared playlist: $playlistName")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to clear playlist", e)
            }
        }
    }
    
    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.itemsRecyclerView.visibility = View.GONE
    }
    
    private fun hideEmptyState() {
        binding.emptyStateLayout.visibility = View.GONE
        binding.itemsRecyclerView.visibility = View.VISIBLE
    }
    
    private fun showErrorState(message: String) {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.itemsRecyclerView.visibility = View.GONE
        binding.emptyStateText.text = message
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
        if (currentFocusedPosition > 0) {
            currentFocusedPosition--
            updateFocus()
        }
    }
    
    private fun navigateDown() {
        val itemCount = playlistItemsAdapter.itemCount
        if (currentFocusedPosition < itemCount - 1) {
            currentFocusedPosition++
            updateFocus()
        }
    }
    
    private fun updateFocus() {
        binding.itemsRecyclerView.scrollToPosition(currentFocusedPosition)
        // Focus will be handled by the adapter
    }
    
    private fun selectCurrentItem() {
        if (currentFocusedPosition >= 0 && currentFocusedPosition < playlistItemsAdapter.itemCount) {
            val item = playlistItemsAdapter.getItemAt(currentFocusedPosition)
            if (item != null) {
                openContent(item)
            }
        }
    }
}
