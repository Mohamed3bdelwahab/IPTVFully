package com.example.newiptv.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.newiptv.R
import com.example.newiptv.ui.series.SeriesScreen
import com.example.newiptv.ui.movies.MoviesScreen
import com.example.newiptv.ui.settings.SettingsActivity
import com.example.newiptv.ui.search.GlobalSearchScreen
import com.example.newiptv.player.MXPlayerIntegration

class HomeScreen : AppCompatActivity() {

    private lateinit var menuGrid: GridLayout
    private var selectedCardIndex = 0
    private val menuCards = mutableListOf<CardView>()
    
    // Menu items configuration
    private val menuItems = listOf(
        MenuItem("Series", R.drawable.ic_series, R.color.series_color),
        MenuItem("Movies", R.drawable.ic_movies, R.color.movies_color),
        MenuItem("Search", R.drawable.ic_search, R.color.accent_color),
        MenuItem("Live TV", R.drawable.ic_live_tv, R.color.live_tv_color),
        MenuItem("Settings", R.drawable.ic_settings, R.color.settings_color),
        MenuItem("History", R.drawable.ic_history, R.color.history_color),
        MenuItem("Favorites", R.drawable.ic_favorites, R.color.favorites_color),
        MenuItem("Test MX Player", R.drawable.ic_series, R.color.series_color) // Test button for MX Player playlist
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)
        
        initializeViews()
        setupMenuCards()
        setupTVRemoteNavigation()
        updateSelection()
    }

    private fun initializeViews() {
        menuGrid = findViewById(R.id.menuGrid)
    }

    private fun setupMenuCards() {
        // Create menu cards dynamically
        for (i in menuItems.indices) {
            val card = createMenuCard(menuItems[i], i)
            menuCards.add(card)
            menuGrid.addView(card)
        }
    }

    private fun createMenuCard(menuItem: MenuItem, index: Int): CardView {
        val card = CardView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                // Get column count from resources (responsive)
                val columnCount = resources.getInteger(R.integer.menu_column_count)
                columnSpec = GridLayout.spec(index % columnCount, 1f)
                rowSpec = GridLayout.spec(index / columnCount)
                setMargins(16, 16, 16, 16)
            }
            radius = 16f
            elevation = 8f
            isFocusable = true
            isFocusableInTouchMode = true
            tag = index
            
            // Set focus change listener for immediate visual feedback
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    selectedCardIndex = index
                    updateSelection()
                    android.util.Log.d("HomeScreen", "Card $index got FOCUS")
                }
            }
        }

        // Card content
        val contentView = layoutInflater.inflate(R.layout.item_menu_card, card, false)
        val iconView = contentView.findViewById<ImageView>(R.id.menuIcon)
        val titleView = contentView.findViewById<TextView>(R.id.menuTitle)

        iconView.setImageResource(menuItem.iconRes)
        titleView.text = menuItem.title

        card.addView(contentView)
        
        // Set click listener
        card.setOnClickListener {
            navigateToSection(index)
        }

        return card
    }

    private fun setupTVRemoteNavigation() {
        // Set initial focus
        menuCards[selectedCardIndex].requestFocus()
        
        // Handle key events for TV remote
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                android.util.Log.d("HomeScreen", "Key pressed: $keyCode")
                
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        navigateUp()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        navigateDown()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        navigateLeft()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        navigateRight()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        navigateToSection(selectedCardIndex)
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        finish()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    private fun navigateUp() {
        val columnCount = resources.getInteger(R.integer.menu_column_count)
        if (selectedCardIndex >= columnCount) {
            val newIndex = selectedCardIndex - columnCount
            menuCards[newIndex].requestFocus()
        }
    }

    private fun navigateDown() {
        val columnCount = resources.getInteger(R.integer.menu_column_count)
        if (selectedCardIndex < columnCount) {
            val newIndex = selectedCardIndex + columnCount
            if (newIndex < menuItems.size) {
                menuCards[newIndex].requestFocus()
            }
        }
    }

    private fun navigateLeft() {
        val columnCount = resources.getInteger(R.integer.menu_column_count)
        if (selectedCardIndex % columnCount > 0) {
            val newIndex = selectedCardIndex - 1
            menuCards[newIndex].requestFocus()
        }
    }

    private fun navigateRight() {
        val columnCount = resources.getInteger(R.integer.menu_column_count)
        if (selectedCardIndex % columnCount < columnCount - 1 && selectedCardIndex < menuItems.size - 1) {
            val newIndex = selectedCardIndex + 1
            menuCards[newIndex].requestFocus()
        }
    }

    private fun updateSelection() {
        // Update all cards with enhanced visual feedback
        for (i in menuCards.indices) {
            val card = menuCards[i]
            val isSelected = i == selectedCardIndex
            
            if (isSelected) {
                // Enhanced selected state with focus animation
                card.elevation = 32f
                card.setCardBackgroundColor(ContextCompat.getColor(this, menuItems[i].colorRes))
                card.alpha = 1.0f
                card.scaleX = 1.15f  // Increased scale for better focus effect
                card.scaleY = 1.15f
                
                // Add rotation animation for flip effect
                card.animate()
                    .rotationY(8f)
                    .setDuration(150)
                    .start()
                    
                android.util.Log.d("HomeScreen", "Card $i is now FOCUSED")
            } else {
                // Normal state
                card.elevation = 8f
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background))
                card.alpha = 0.7f
                card.scaleX = 1.0f
                card.scaleY = 1.0f
                
                // Reset rotation
                card.animate()
                    .rotationY(0f)
                    .setDuration(150)
                    .start()
            }
        }
    }

    private fun navigateToSection(index: Int) {
        when (index) {
            0 -> { // Series
                val intent = Intent(this, SeriesScreen::class.java).apply {
                    putExtra("content_type", "series")
                }
                startActivity(intent)
            }
            1 -> { // Movies
                val intent = Intent(this, MoviesScreen::class.java)
                startActivity(intent)
            }
            2 -> { // Search
                val intent = Intent(this, GlobalSearchScreen::class.java)
                startActivity(intent)
            }
            3 -> { // Live TV
                // TODO: Implement Live TV screen
                showComingSoon("Live TV")
            }
            4 -> { // Settings
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            5 -> { // History
                val intent = Intent(this, com.example.newiptv.ui.history.HistoryScreen::class.java)
                startActivity(intent)
            }
            6 -> { // Favorites
                val intent = Intent(this, com.example.newiptv.ui.favorites.FavoritesScreen::class.java)
                startActivity(intent)
            }
            7 -> { // Test MX Player
                testMXPlayerPlaylist()
            }
        }
    }

    private fun showComingSoon(section: String) {
        // Simple toast for now, will be replaced with proper UI
        android.widget.Toast.makeText(this, "$section - Coming Soon!", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun testMXPlayerPlaylist() {
        android.util.Log.d("HomeScreen", "🧪 Testing MX Player playlist with correct data types...")
        
        // Test playlist with correct data types (based on working Official API method)
        val videoUrls = listOf(
            "http://aws85485.amazonedge.net/series/moh7amed819/150730/497321.mkv",
            "http://aws85485.amazonedge.net/series/moh7amed819/150730/497322.mkv",
            "http://aws85485.amazonedge.net/series/moh7amed819/150730/497323.mkv"
        )
        val videoNames = listOf("Episode 1", "Episode 2", "Episode 3")
        
        val packageName = if (MXPlayerIntegration(this, null).isMXPlayerInstalled()) {
            try {
                val packageManager = packageManager
                val proInfo = packageManager.getPackageInfo("com.mxtech.videoplayer.pro", 0)
                "com.mxtech.videoplayer.pro"
            } catch (e: Exception) {
                "com.mxtech.videoplayer.ad"
            }
        } else {
            "com.mxtech.videoplayer.ad"
        }
        
        // Convert to URIs with correct data types
        val videoUris = videoUrls.map { android.net.Uri.parse(it) }.toTypedArray()
        val videoNamesArray = videoNames.toTypedArray()
        
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setPackage(packageName)
            type = "video/*"
            
            // Set the first video as the primary data
            if (videoUris.isNotEmpty()) {
                setDataAndType(videoUris[0], "video/*")
            }
            
            // Use correct data types to avoid ClassCastException
            putExtra("video_list", videoUris) // Uri[] instead of ArrayList<Uri>
            putExtra("video_list.name", videoNamesArray) // String[] instead of ArrayList<String>
            putExtra("video_list.play_index", 0)
            putExtra("video_list_is_explicit", true)
            
            // Additional parameters with correct data types
            putExtra("title", "MX Player Test Playlist")
            putExtra("decode_mode", 0.toByte()) // Byte instead of Integer
            putExtra("return_result", true)
            putExtra("secure_uri", true)
        }
        
        try {
            startActivity(intent)
            android.widget.Toast.makeText(this, "MX Player launched with playlist!", android.widget.Toast.LENGTH_LONG).show()
            android.util.Log.d("HomeScreen", "✅ MX Player playlist launched successfully")
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "❌ Failed to launch MX Player playlist", e)
            android.widget.Toast.makeText(this, "Failed to launch MX Player playlist: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    data class MenuItem(
        val title: String,
        val iconRes: Int,
        val colorRes: Int
    )
}

