package com.example.newiptv.ui.movieinfo

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.InfoEntity
import com.example.newiptv.data.mapping.ApiTVMapping
import com.example.newiptv.data.repository.TvRepository
import com.example.newiptv.data.repository.FavoritePlaylistRepository
import com.example.newiptv.data.db.entities.FavoritePlaylistEntity
import com.example.newiptv.ui.favorites.PlaylistDialogAdapter
import com.example.newiptv.player.VideoPlayerActivity
import com.example.newiptv.utils.KeyEventLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MovieInfoScreen : AppCompatActivity() {

    private lateinit var movieCover: ImageView
    private lateinit var movieBackdrop: ImageView
    private lateinit var movieTitle: TextView
    private lateinit var moviePlot: TextView
    private lateinit var movieGenre: TextView
    private lateinit var movieReleaseDate: TextView
    private lateinit var movieRating: TextView
    private lateinit var movieDuration: TextView
    private lateinit var movieDirector: TextView
    private lateinit var movieCast: TextView
    private lateinit var movieCountry: TextView
    private lateinit var movieLanguage: TextView
    private lateinit var movieAgeRating: TextView
    private lateinit var loadingText: TextView
    private lateinit var errorText: TextView
    private lateinit var watchMovieButton: Button

    private lateinit var repository: TvRepository
    private lateinit var favoritePlaylistRepository: FavoritePlaylistRepository
    private var movieId: String = ""
    private var movieName: String = ""
    private var movieCategory: String = ""
    private var isFavoriteButtonFocused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_info_screen)

        // Get movie details from intent
        movieId = intent.getStringExtra("movie_id") ?: ""
        movieName = intent.getStringExtra("movie_name") ?: ""
        movieCategory = intent.getStringExtra("movie_category") ?: ""

        if (movieId.isEmpty()) {
            finish()
            return
        }

        val database = DatabaseProvider.getDatabase(this)
        repository = TvRepository(database)
        favoritePlaylistRepository = FavoritePlaylistRepository(database.favoritePlaylistDao())
        
        // Initialize default playlists if needed
        lifecycleScope.launch {
            favoritePlaylistRepository.createDefaultPlaylistsIfNeeded()
        }

        initializeViews()
        setupTVRemoteNavigation()
        setupWatchMovieButton()
        loadMovieInfo()
    }

    private fun initializeViews() {
        movieCover = findViewById(R.id.movieCover)
        movieBackdrop = findViewById(R.id.movieBackdrop)
        movieTitle = findViewById(R.id.movieTitle)
        moviePlot = findViewById(R.id.moviePlot)
        movieGenre = findViewById(R.id.movieGenre)
        movieReleaseDate = findViewById(R.id.movieReleaseDate)
        movieRating = findViewById(R.id.movieRating)
        movieDuration = findViewById(R.id.movieDuration)
        movieDirector = findViewById(R.id.movieDirector)
        movieCast = findViewById(R.id.movieCast)
        movieCountry = findViewById(R.id.movieCountry)
        movieLanguage = findViewById(R.id.movieLanguage)
        movieAgeRating = findViewById(R.id.movieAgeRating)
        loadingText = findViewById(R.id.loadingText)
        errorText = findViewById(R.id.errorText)
        watchMovieButton = findViewById<Button>(R.id.watchMovieButton)
        
        // Initialize favorite button
        val favoriteButton = findViewById<ImageButton>(R.id.favoriteButton)
        favoriteButton.setOnClickListener {
            showFavoriteDialog()
        }
        
        // Make favorite button focusable for TV remote
        favoriteButton.isFocusable = true
        favoriteButton.isFocusableInTouchMode = true
        
        // Add focus change listener for visual feedback
        favoriteButton.setOnFocusChangeListener { _, hasFocus ->
            isFavoriteButtonFocused = hasFocus
            if (hasFocus) {
                android.util.Log.d("MovieInfoScreen", "🎯 Favorite button focused")
                favoriteButton.background = getDrawable(R.drawable.button_focused_background)
            } else {
                favoriteButton.background = getDrawable(R.drawable.button_background)
            }
        }

        // Set initial title
        title = "Movie Info: $movieName"
    }

    private fun setupTVRemoteNavigation() {
        // TV remote navigation is handled in dispatchKeyEvent
    }

    private fun setupWatchMovieButton() {
        watchMovieButton.setOnClickListener {
            playMovie()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (isFavoriteButtonFocused) {
                        // Stay on favorite button
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (isFavoriteButtonFocused) {
                        // Move to watch movie button
                        moveToWatchMovieButton()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (isFavoriteButtonFocused) {
                        // Stay on favorite button
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (isFavoriteButtonFocused) {
                        // Stay on favorite button
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (isFavoriteButtonFocused) {
                        android.util.Log.d("MovieInfoScreen", "✅ Favorite button selected")
                        showFavoriteDialog()
                        return true
                    } else {
                        // Play movie
                        playMovie()
                        return true
                    }
                }
                KeyEvent.KEYCODE_BACK -> {
                    android.util.Log.d("MovieInfoScreen", "BACK key pressed - finishing activity")
                    finish()
                    return true
                }
            }
        }
        
        return super.dispatchKeyEvent(event)
    }
    
    private fun moveToWatchMovieButton() {
        isFavoriteButtonFocused = false
        watchMovieButton.requestFocus()
        android.util.Log.d("MovieInfoScreen", "🎯 Moved focus to Watch Movie button")
    }

    private fun loadMovieInfo() {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        watchMovieButton.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // First sync movie info from API (same pattern as Series screen)
                repository.syncInfo("movie", movieId)
                
                // Then get the synced info from database
                val movieInfo = repository.getInfo(movieId, "movie")
                if (movieInfo != null) {
                    displayMovieInfo(movieInfo)
                    loadingText.visibility = View.GONE
                    KeyEventLogger.logScreenEvent("MovieInfoScreen", "Loaded movie info successfully")
                } else {
                    loadingText.visibility = View.GONE
                    errorText.text = "Movie info not found"
                    KeyEventLogger.logError("MovieInfoScreen", "Movie info not found in database after sync", "No data returned")
                    errorText.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                loadingText.visibility = View.GONE
                errorText.text = "Failed to load movie info: ${e.message}"
                errorText.visibility = View.VISIBLE
                KeyEventLogger.logError("MovieInfoScreen", "Failed to load movie info", e.message ?: "Unknown error")
            }
        }
    }

    private fun displayMovieInfo(movieInfo: InfoEntity) {
        // Set basic movie information - using exact same properties as Series screen
        movieTitle.text = movieInfo.name ?: movieName
        moviePlot.text = movieInfo.plot ?: "No plot available"
        movieGenre.text = movieInfo.genre ?: "Unknown Genre"
        movieReleaseDate.text = movieInfo.releaseDate ?: "Unknown Date"
        movieRating.text = movieInfo.rating5Based?.toString() ?: "N/A"
        movieDuration.text = movieInfo.episodeRunTime?.toString() ?: "Unknown Duration"
        movieDirector.text = movieInfo.director ?: "Unknown Director"
        movieCast.text = movieInfo.cast ?: "Unknown Cast"
        movieCountry.text = "Unknown Country" // InfoEntity doesn't have country
        movieLanguage.text = "English" // Default language
        movieAgeRating.text = "N/A" // InfoEntity doesn't have age

        // Load backdrop image - same pattern as Series screen
        movieInfo.backdropPath?.let { backdropUrl ->
            Glide.with(this)
                .load(backdropUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(movieBackdrop)
        } ?: run {
            movieBackdrop.setImageResource(R.drawable.placeholder_image)
        }

        // Show the watch movie button
        watchMovieButton.visibility = View.VISIBLE

        // Log display information - using simple strings like Series screen
        KeyEventLogger.logScreenEvent("MovieInfoScreen", "Displayed movie info")
        KeyEventLogger.logScreenEvent("MovieInfoScreen", "Movie info updated")
    }

    private fun playMovie() {
        // Construct playback URL using the correct CDN pattern from API mapping
        val playbackUrl = "http://aws85485.amazonedge.net//movie/moh7amed819/150730/$movieId.mkv"

        // Launch video player
        val intent = Intent(this@MovieInfoScreen, VideoPlayerActivity::class.java).apply {
            putExtra("video_url", playbackUrl)
            putExtra("video_title", movieName)
            putExtra("content_type", "movie")
            putExtra("movie_id", movieId)
        }
        startActivity(intent)

        // Log the action
        KeyEventLogger.logScreenEvent("MovieInfoScreen", "Playing movie")
        KeyEventLogger.logScreenEvent("MovieInfoScreen", "Movie playback started")
    }
    
    private fun showFavoriteDialog() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("MovieInfoScreen", "🔍 Getting playlists for favorite dialog...")
                
                // Ensure default playlists are created first
                favoritePlaylistRepository.createDefaultPlaylistsIfNeeded()
                
                val playlists = favoritePlaylistRepository.getAllPlaylistsSync()
                android.util.Log.d("MovieInfoScreen", "📋 Found ${playlists.size} playlists: ${playlists.map { it.name }}")
                
                runOnUiThread {
                    showCustomFavoriteDialog(playlists)
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieInfoScreen", "❌ Failed to show favorite dialog", e)
                e.printStackTrace()
            }
        }
    }
    
    private fun showCustomFavoriteDialog(playlists: List<FavoritePlaylistEntity>) {
        android.util.Log.d("MovieInfoScreen", "🎨 Creating custom favorite dialog...")
        
        // Create custom dialog
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_favorite_playlist)
        dialog.setCancelable(true)
        
        // Get dialog views
        val dialogTitle = dialog.findViewById<android.widget.TextView>(R.id.dialogTitle)
        val dialogMessage = dialog.findViewById<android.widget.TextView>(R.id.dialogMessage)
        val createNewPlaylistButton = dialog.findViewById<android.widget.Button>(R.id.createNewPlaylistButton)
        val playlistRecyclerView = dialog.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.playlistRecyclerView)
        val cancelButton = dialog.findViewById<android.widget.Button>(R.id.cancelButton)
        
        // Set dialog content
        dialogTitle.text = "Add to Favorites"
        dialogMessage.text = "Choose a playlist to add '${movieName}' to:"
        
        // Setup create new playlist button
        createNewPlaylistButton.setOnClickListener {
            android.util.Log.d("MovieInfoScreen", "➕ Create new playlist button clicked")
            dialog.dismiss()
            showCreatePlaylistDialog()
        }
        
        // Setup playlist RecyclerView
        playlistRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val adapter = PlaylistDialogAdapter(playlists) { playlist ->
            android.util.Log.d("MovieInfoScreen", "📝 Adding to playlist: ${playlist.name}")
            dialog.dismiss()
            addToPlaylist(playlist.id)
        }
        playlistRecyclerView.adapter = adapter
        
        // Setup cancel button
        cancelButton.setOnClickListener {
            android.util.Log.d("MovieInfoScreen", "❌ Dialog cancelled")
            dialog.dismiss()
        }
        
        // Setup TV remote navigation
        setupDialogNavigation(dialog, createNewPlaylistButton, playlistRecyclerView, cancelButton)
        
        // Show dialog
        dialog.show()
        
        // Set initial focus
        createNewPlaylistButton.requestFocus()
        
        android.util.Log.d("MovieInfoScreen", "✅ Custom dialog created and shown with ${playlists.size} playlists")
    }
    
    private fun setupDialogNavigation(
        dialog: android.app.Dialog,
        createButton: android.widget.Button,
        recyclerView: androidx.recyclerview.widget.RecyclerView,
        cancelButton: android.widget.Button
    ) {
        // Handle key events for TV remote navigation
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (createButton.hasFocus()) {
                            recyclerView.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                        if (recyclerView.hasFocus()) {
                            createButton.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    android.view.KeyEvent.KEYCODE_BACK -> {
                        dialog.dismiss()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }
    
    private fun showCreatePlaylistDialog() {
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
                    createPlaylistAndAdd(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createPlaylistAndAdd(name: String) {
        lifecycleScope.launch {
            try {
                val playlist = favoritePlaylistRepository.createPlaylist(name)
                addToPlaylist(playlist.id)
                android.util.Log.d("MovieInfoScreen", "✅ Created playlist and added movie: $name")
            } catch (e: Exception) {
                android.util.Log.e("MovieInfoScreen", "❌ Failed to create playlist", e)
            }
        }
    }
    
    private fun addToPlaylist(playlistId: String) {
        lifecycleScope.launch {
            try {
                val success = favoritePlaylistRepository.addItemToPlaylist(
                    playlistId = playlistId,
                    contentId = movieId,
                    contentType = "movie",
                    title = movieName,
                    cover = null,
                    streamUrl = null,
                    seriesId = null,
                    seasonNumber = null,
                    episodeNumber = null
                )
                
                if (success) {
                    android.util.Log.d("MovieInfoScreen", "✅ Added movie to playlist: $movieName")
                    // Show success message and change heart color
                    runOnUiThread {
                        android.widget.Toast.makeText(this@MovieInfoScreen, "Added to favorites!", android.widget.Toast.LENGTH_SHORT).show()
                        updateFavoriteButtonState(true)
                    }
                } else {
                    android.util.Log.d("MovieInfoScreen", "⚠️ Movie already in playlist")
                    runOnUiThread {
                        android.widget.Toast.makeText(this@MovieInfoScreen, "Already in favorites!", android.widget.Toast.LENGTH_SHORT).show()
                        updateFavoriteButtonState(true)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieInfoScreen", "❌ Failed to add to playlist", e)
            }
        }
    }
    
    private fun updateFavoriteButtonState(isInFavorites: Boolean) {
        val favoriteButton = findViewById<ImageButton>(R.id.favoriteButton)
        if (isInFavorites) {
            // Change to filled heart (red)
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
            favoriteButton.setColorFilter(getColor(R.color.red))
        } else {
            // Change to border heart (white)
            favoriteButton.setImageResource(R.drawable.ic_favorite_border)
            favoriteButton.setColorFilter(getColor(R.color.white))
        }
    }
}
