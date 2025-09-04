package com.example.newiptv.ui.movieinfo

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.InfoEntity
import com.example.newiptv.data.mapping.ApiTVMapping
import com.example.newiptv.data.repository.TvRepository
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
    private var movieId: String = ""
    private var movieName: String = ""
    private var movieCategory: String = ""

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
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    // Play movie
                    playMovie()
                    return true
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
}
