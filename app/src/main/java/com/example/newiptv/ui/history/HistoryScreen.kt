package com.example.newiptv.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.WatchHistoryEntity
import com.example.newiptv.data.repository.WatchHistoryRepository
import com.example.newiptv.ui.movieinfo.MovieInfoScreen
import com.example.newiptv.ui.seriesinfo.SeriesInfoScreen
import com.example.newiptv.utils.KeyEventLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryScreen : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var loadingText: TextView
    private lateinit var errorText: TextView
    private lateinit var emptyText: TextView

    private lateinit var watchHistoryRepository: WatchHistoryRepository
    private var historyItems: List<WatchHistoryEntity> = emptyList()
    
    private var selectedHistoryIndex = 0
    private var isInHistoryPanel = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_screen)

        // Set screen title
        title = "Watch History"

        val database = DatabaseProvider.getDatabase(this)
        watchHistoryRepository = WatchHistoryRepository(database)

        initializeViews()
        loadHistory()
    }

    private fun initializeViews() {
        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        loadingText = findViewById(R.id.loadingText)
        errorText = findViewById(R.id.errorText)
        emptyText = findViewById(R.id.emptyText)

        // Set up RecyclerView with grid layout
        val layoutManager = GridLayoutManager(this, 3)
        historyRecyclerView.layoutManager = layoutManager

        // Set up adapter
        historyAdapter = HistoryAdapter { historyItem ->
            navigateToContent(historyItem)
        }
        historyRecyclerView.adapter = historyAdapter

        // Set initial focus
        historyRecyclerView.requestFocus()
    }

    private fun loadHistory() {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        emptyText.visibility = View.GONE

        lifecycleScope.launch {
            watchHistoryRepository.getAllHistory().collectLatest { historyList ->
                historyItems = historyList
                
                if (historyList.isEmpty()) {
                    loadingText.visibility = View.GONE
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = "No watch history found"
                } else {
                    loadingText.visibility = View.GONE
                    historyAdapter.updateHistory(historyList)
                    updateHistoryFocus()
                }
            }
        }
    }

    private fun navigateToContent(historyItem: WatchHistoryEntity) {
        android.util.Log.d("HistoryScreen", "Navigating to content: ${historyItem.title}")
        android.util.Log.d("HistoryScreen", "Content Type: ${historyItem.contentType}")
        android.util.Log.d("HistoryScreen", "Content ID: ${historyItem.contentId}")

        when (historyItem.contentType) {
            "movie" -> {
                val intent = Intent(this, MovieInfoScreen::class.java).apply {
                    putExtra("movie_id", historyItem.contentId)
                    putExtra("movie_name", historyItem.title)
                    putExtra("movie_category", historyItem.categoryName ?: "")
                }
                startActivity(intent)
            }
            "series" -> {
                val intent = Intent(this, SeriesInfoScreen::class.java).apply {
                    putExtra("series_id", historyItem.contentId)
                    putExtra("series_name", historyItem.title)
                    putExtra("series_category", historyItem.categoryName ?: "")
                }
                startActivity(intent)
            }
            "episode" -> {
                // For episodes, navigate to series info screen
                val seriesId = historyItem.seriesId ?: historyItem.contentId
                val intent = Intent(this, SeriesInfoScreen::class.java).apply {
                    putExtra("series_id", seriesId)
                    putExtra("series_name", historyItem.title)
                    putExtra("series_category", historyItem.categoryName ?: "")
                    // Pass episode info if available
                    if (historyItem.seasonNumber != null && historyItem.episodeNumber != null) {
                        putExtra("season_number", historyItem.seasonNumber)
                        putExtra("episode_number", historyItem.episodeNumber)
                    }
                }
                startActivity(intent)
            }
            else -> {
                android.util.Log.w("HistoryScreen", "Unknown content type: ${historyItem.contentType}")
            }
        }
    }

    private fun updateHistoryFocus() {
        try {
            historyRecyclerView.scrollToPosition(selectedHistoryIndex)
            val viewHolder = historyRecyclerView.findViewHolderForAdapterPosition(selectedHistoryIndex)
            if (viewHolder != null) {
                viewHolder.itemView.requestFocus()
                KeyEventLogger.logFocusChange("HistoryScreen", "History", selectedHistoryIndex)
            }
        } catch (e: Exception) {
            KeyEventLogger.logError("HistoryScreen", "Error updating history focus", e.message ?: "Unknown error")
        }
    }

    private fun getCurrentFocusedHistoryPosition(): Int {
        val focusedView = historyRecyclerView.focusedChild
        if (focusedView != null) {
            val position = historyRecyclerView.getChildAdapterPosition(focusedView)
            if (position != RecyclerView.NO_POSITION) {
                selectedHistoryIndex = position
                return position
            }
        }
        return selectedHistoryIndex
    }

    private fun navigateHistoryUp() {
        val spanCount = 3
        val currentPosition = getCurrentFocusedHistoryPosition()
        if (currentPosition - spanCount >= 0) {
            selectedHistoryIndex = currentPosition - spanCount
            updateHistoryFocus()
            KeyEventLogger.logFocusChange("HistoryScreen", "History", selectedHistoryIndex)
        } else {
            KeyEventLogger.logError("HistoryScreen", "Cannot navigate UP", "Top row reached")
        }
    }

    private fun navigateHistoryDown() {
        val spanCount = 3
        val totalItems = historyAdapter.itemCount
        val currentPosition = getCurrentFocusedHistoryPosition()
        if (currentPosition + spanCount < totalItems) {
            selectedHistoryIndex = currentPosition + spanCount
            updateHistoryFocus()
            KeyEventLogger.logFocusChange("HistoryScreen", "History", selectedHistoryIndex)
        } else {
            KeyEventLogger.logError("HistoryScreen", "Cannot navigate DOWN", "Bottom row reached")
        }
    }

    private fun navigateHistoryLeft() {
        val currentPosition = getCurrentFocusedHistoryPosition()
        if (currentPosition > 0) {
            selectedHistoryIndex = currentPosition - 1
            updateHistoryFocus()
            KeyEventLogger.logFocusChange("HistoryScreen", "History", selectedHistoryIndex)
        } else {
            KeyEventLogger.logError("HistoryScreen", "Cannot navigate LEFT", "Leftmost column reached")
        }
    }

    private fun navigateHistoryRight() {
        val totalItems = historyAdapter.itemCount
        val currentPosition = getCurrentFocusedHistoryPosition()
        if (currentPosition < totalItems - 1) {
            selectedHistoryIndex = currentPosition + 1
            updateHistoryFocus()
            KeyEventLogger.logFocusChange("HistoryScreen", "History", selectedHistoryIndex)
        } else {
            KeyEventLogger.logError("HistoryScreen", "Cannot navigate RIGHT", "Rightmost column reached")
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    navigateHistoryUp()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    navigateHistoryDown()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    navigateHistoryLeft()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    navigateHistoryRight()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    val focusedView = historyRecyclerView.focusedChild
                    if (focusedView != null) {
                        val position = historyRecyclerView.getChildAdapterPosition(focusedView)
                        if (position != RecyclerView.NO_POSITION && position < historyItems.size) {
                            val selectedHistory = historyItems[position]
                            KeyEventLogger.logItemSelection("HistoryScreen", "History", position, selectedHistory.title)
                            selectedHistoryIndex = position
                            navigateToContent(selectedHistory)
                        }
                    }
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
}
