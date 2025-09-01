package com.example.newiptv.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.CategoryEntity
import com.example.newiptv.data.db.entities.ItemEntity
import com.example.newiptv.data.repository.TvRepository
import com.example.newiptv.ui.seriesinfo.SeriesInfoScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.newiptv.ui.series.SeriesAdapter
import com.example.newiptv.ui.series.CategoryAdapter
import com.example.newiptv.utils.KeyEventLogger

class SeriesScreen : AppCompatActivity() {

    private lateinit var categoryListView: ListView
    private lateinit var seriesRecyclerView: RecyclerView
    private lateinit var seriesAdapter: SeriesAdapter
    private lateinit var loadingText: TextView
    private lateinit var errorText: TextView
    private lateinit var filterSpinner: Spinner

    private var selectedCategoryIndex = 0
    private var selectedSeriesIndex = 0
    private var isInCategoryPanel = true
    
    // Filter options
    private val filterOptions = listOf(
        "Default",
        "A-Z",
        "Z-A", 
        "Latest",
        "Oldest",
        "Rating (High to Low)",
        "Rating (Low to High)",
        "Rating String (High to Low)",
        "Rating String (Low to High)",
        "Year (Newest)",
        "Year (Oldest)"
    )
    private var currentFocusIndex = 0

    private lateinit var repository: TvRepository
    private var categories: List<CategoryEntity> = emptyList()
    private var currentSeries: List<ItemEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_screen)

        val database = DatabaseProvider.getDatabase(this)
        repository = TvRepository(database)

        initializeViews()
        setupCategoryList()
        setupSeriesGrid()
        setupTVRemoteNavigation()
        loadCategories()
    }

    private fun initializeViews() {
        categoryListView = findViewById(R.id.categoryListView)
        seriesRecyclerView = findViewById(R.id.seriesRecyclerView)
        loadingText = findViewById(R.id.loadingText)
        errorText = findViewById(R.id.errorText)
        filterSpinner = findViewById(R.id.filterSpinner)
        
        // Set initial focus to category panel
        categoryListView.requestFocus()
        isInCategoryPanel = true
        selectedCategoryIndex = 0
        selectedSeriesIndex = 0
        
        // Setup filter spinner
        setupFilterSpinner()
        
        // Ensure proper focus handling
        KeyEventLogger.logScreenEvent("SeriesScreen", "Views initialized")
        KeyEventLogger.logFocusChange("SeriesScreen", "Category", selectedCategoryIndex)
    }
    
    private fun setupFilterSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = adapter
        
        filterSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                applyFilter(filterOptions[position])
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupCategoryList() {
        categoryListView.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                android.util.Log.d("SeriesScreen", "=== ON ITEM SELECTED CALLED ===")
                android.util.Log.d("SeriesScreen", "Position: $position, Selected Index: $selectedCategoryIndex")
                android.util.Log.d("SeriesScreen", "Categories size: ${categories.size}")
                
                if (position < categories.size) {
                    val category = categories[position]
                    android.util.Log.d("SeriesScreen", "Category: ${category.categoryName} (ID: ${category.categoryId})")
                    
                    // Only update selection index, don't load series automatically
                    if (position != selectedCategoryIndex) {
                        selectedCategoryIndex = position
                        android.util.Log.d("SeriesScreen", "=== CATEGORY FOCUS CHANGED (TV REMOTE) ===")
                        android.util.Log.d("SeriesScreen", "New Position: $position")
                        android.util.Log.d("SeriesScreen", "Category ID: ${category.categoryId}")

                        // ✅ Only update visual selection, don't load series yet
                        updateCategorySelection()
                    } else {
                        android.util.Log.d("SeriesScreen", "Same position, no change needed")
                    }
                } else {
                    android.util.Log.e("SeriesScreen", "Invalid position: $position, categories size: ${categories.size}")
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                android.util.Log.d("SeriesScreen", "=== NOTHING SELECTED ===")
            }
                })
        
        // Add click listener as backup for mouse clicks
        categoryListView.setOnItemClickListener { _, _, position, _ ->
            android.util.Log.d("SeriesScreen", "=== CATEGORY CLICKED (MOUSE) ===")
            android.util.Log.d("SeriesScreen", "Position: $position")
            
            if (position < categories.size) {
                selectedCategoryIndex = position
                val category = categories[position]
                android.util.Log.d("SeriesScreen", "Category Name: ${category.categoryName}")
                android.util.Log.d("SeriesScreen", "Category ID: ${category.categoryId}")
                loadSeriesForCategory(category.categoryId)
                updateCategorySelection()
            }
        }
        

    }
    
    private fun applyFilter(filterType: String) {
        val currentSeries = seriesAdapter.getSeriesList().toMutableList()
        val sortedSeries = when (filterType) {
            "A-Z" -> currentSeries.sortedBy { it.name }
            "Z-A" -> currentSeries.sortedByDescending { it.name }
            "Latest" -> currentSeries.sortedByDescending { it.lastModified }
            "Oldest" -> currentSeries.sortedBy { it.lastModified }
            "Rating (High to Low)" -> currentSeries.sortedByDescending { it.rating5Based ?: 0.0 }
            "Rating (Low to High)" -> currentSeries.sortedBy { it.rating5Based ?: 0.0 }
            "Rating String (High to Low)" -> currentSeries.sortedByDescending { 
                it.rating?.toDoubleOrNull() ?: 0.0 
            }
            "Rating String (Low to High)" -> currentSeries.sortedBy { 
                it.rating?.toDoubleOrNull() ?: 0.0 
            }
            "Year (Newest)" -> currentSeries.sortedByDescending { 
                it.releaseDate?.split("-")?.firstOrNull()?.toIntOrNull() ?: 0 
            }
            "Year (Oldest)" -> currentSeries.sortedBy { 
                it.releaseDate?.split("-")?.firstOrNull()?.toIntOrNull() ?: 0 
            }
            else -> currentSeries // Default - no sorting
        }
        
        seriesAdapter.updateSeries(sortedSeries)
        KeyEventLogger.logScreenEvent("SeriesScreen", "Applied filter: $filterType")
    }

    private fun setupSeriesGrid() {
        seriesAdapter = SeriesAdapter { series ->
            val categoryName = if (selectedCategoryIndex < categories.size) categories[selectedCategoryIndex].categoryName else ""
            val intent = Intent(this, SeriesInfoScreen::class.java).apply {
                putExtra("series_name", series.name)
                putExtra("series_category", categoryName)
                putExtra("series_id", series.itemId)
            }
            startActivity(intent)
        }

        seriesRecyclerView.apply {
            layoutManager = GridLayoutManager(this@SeriesScreen, 3)
            adapter = seriesAdapter
        }
    }

    private fun setupTVRemoteNavigation() {
        categoryListView.requestFocus()
    }
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val additionalInfo = "Panel: ${if (isInCategoryPanel) "Category" else "Series"}, " +
                           "CategoryIndex: $selectedCategoryIndex, " +
                           "SeriesIndex: $selectedSeriesIndex"
        
        KeyEventLogger.logKeyEvent("SeriesScreen", event, additionalInfo)
        
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (!isInCategoryPanel && selectedSeriesIndex % 3 == 0) {
                        isInCategoryPanel = true
                        categoryListView.requestFocus()
                        android.util.Log.d("SeriesScreen", "DPAD_LEFT: Moved to category panel")
                        return true
                    }
                }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (isInCategoryPanel) {
                                isInCategoryPanel = false
                                seriesRecyclerView.requestFocus()
                                KeyEventLogger.logNavigation("SeriesScreen", "RIGHT", "Category", "Series")
                                // Ensure series focus is properly set
                                seriesRecyclerView.postDelayed({
                                    updateSeriesFocus()
                                }, 100)
                                return true
                            }
                        }
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                            KeyEventLogger.logNavigation("SeriesScreen", "UP", if (isInCategoryPanel) "Category" else "Series")
                            if (isInCategoryPanel) {
                                navigateCategoryUp()
                            } else {
                                navigateSeriesUp()
                            }
                            return true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            KeyEventLogger.logNavigation("SeriesScreen", "DOWN", if (isInCategoryPanel) "Category" else "Series")
                            if (isInCategoryPanel) {
                                navigateCategoryDown()
                            } else {
                                navigateSeriesDown()
                            }
                            return true
                        }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (isInCategoryPanel) {
                        KeyEventLogger.logItemSelection("SeriesScreen", "Category", selectedCategoryIndex, 
                            if (selectedCategoryIndex < categories.size) categories[selectedCategoryIndex].categoryName else "Unknown")
                        selectCurrentCategory()
                    } else {
                        val series = seriesAdapter.getSeriesAt(selectedSeriesIndex)
                        KeyEventLogger.logItemSelection("SeriesScreen", "Series", selectedSeriesIndex, 
                            series?.name ?: "Unknown")
                        selectCurrentSeries()
                    }
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    android.util.Log.d("SeriesScreen", "BACK key pressed - finishing activity")
                    finish()
                    return true
                }
            }
        }
        
        return super.dispatchKeyEvent(event)
    }
    
    private fun getKeyCodeName(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> "DPAD_LEFT"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "DPAD_RIGHT"
            KeyEvent.KEYCODE_DPAD_UP -> "DPAD_UP"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DPAD_DOWN"
            KeyEvent.KEYCODE_DPAD_CENTER -> "DPAD_CENTER"
            KeyEvent.KEYCODE_ENTER -> "ENTER"
            KeyEvent.KEYCODE_BACK -> "BACK"
            else -> "UNKNOWN($keyCode)"
        }
    }

    private fun updateCategorySelection() {
        categoryListView.setSelection(selectedCategoryIndex)
        updateCategoryVisualSelection()
    }
    
    private fun navigateCategoryUp() {
        if (selectedCategoryIndex > 0) {
            selectedCategoryIndex--
            updateCategorySelection()
            KeyEventLogger.logFocusChange("SeriesScreen", "Category", selectedCategoryIndex)
        }
    }
    
    private fun navigateCategoryDown() {
        if (selectedCategoryIndex < categories.size - 1) {
            selectedCategoryIndex++
            updateCategorySelection()
            KeyEventLogger.logFocusChange("SeriesScreen", "Category", selectedCategoryIndex)
        }
    }
    
    private fun navigateSeriesUp() {
        val spanCount = 3 // same as GridLayoutManager spanCount
        if (selectedSeriesIndex - spanCount >= 0) {
            selectedSeriesIndex -= spanCount
            updateSeriesFocus()
            KeyEventLogger.logFocusChange("SeriesScreen", "Series", selectedSeriesIndex)
        } else {
            // Already in top row
            KeyEventLogger.logError("SeriesScreen", "Cannot navigate UP", "Top row reached")
        }
        }

        private fun navigateSeriesDown() {
        val spanCount = 3
        val totalSeries = seriesAdapter.itemCount
        if (selectedSeriesIndex + spanCount < totalSeries) {
            selectedSeriesIndex += spanCount
            updateSeriesFocus()
            KeyEventLogger.logFocusChange("SeriesScreen", "Series", selectedSeriesIndex)
        } else {
            // Bottom row handling
            KeyEventLogger.logError("SeriesScreen", "Cannot navigate DOWN", "Bottom row reached")
        }
    }

    
    private fun updateSeriesFocus() {
        seriesRecyclerView.post {
            try {
                // Scroll to the selected position
                seriesRecyclerView.smoothScrollToPosition(selectedSeriesIndex)
                
                // Wait a bit for scroll to complete, then set focus
                seriesRecyclerView.postDelayed({
                    val viewHolder = seriesRecyclerView.findViewHolderForAdapterPosition(selectedSeriesIndex)
                    if (viewHolder != null) {
                        viewHolder.itemView.requestFocus()
                        KeyEventLogger.logFocusChange("SeriesScreen", "Series", selectedSeriesIndex)
                    } else {
                        // If viewHolder is null, try to scroll again
                        seriesRecyclerView.scrollToPosition(selectedSeriesIndex)
                        seriesRecyclerView.postDelayed({
                            val retryViewHolder = seriesRecyclerView.findViewHolderForAdapterPosition(selectedSeriesIndex)
                            retryViewHolder?.itemView?.requestFocus()
                        }, 100)
                    }
                }, 150)
            } catch (e: Exception) {
                KeyEventLogger.logError("SeriesScreen", "Error updating series focus", e.message ?: "Unknown error")
            }
        }
    }
    
    private fun selectCurrentCategory() {
        if (selectedCategoryIndex < categories.size) {
            val category = categories[selectedCategoryIndex]
            KeyEventLogger.logDataLoading("SeriesScreen", "Series", "Category Selection")
            loadSeriesForCategory(category.categoryId)
        }
    }
    
    private fun selectCurrentSeries() {
        val series = seriesAdapter.getSeriesAt(selectedSeriesIndex)
        if (series != null) {
            android.util.Log.d("SeriesScreen", "Selecting series: ${series.name} at index: $selectedSeriesIndex")
            val categoryName = if (selectedCategoryIndex < categories.size) categories[selectedCategoryIndex].categoryName else ""
            val intent = Intent(this, SeriesInfoScreen::class.java).apply {
                putExtra("series_name", series.name)
                putExtra("series_category", categoryName)
                putExtra("series_id", series.itemId)
            }
            startActivity(intent)
        }
    }

    private fun loadCategories() {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE

        lifecycleScope.launch {
            repository.loadCategoriesWithSync("series").collectLatest { result ->
                result.fold(
                    onSuccess = { categoryList ->
                        categories = categoryList
                        val categoryAdapter = CategoryAdapter(this@SeriesScreen, categoryList)
                        categoryListView.adapter = categoryAdapter
                        loadingText.visibility = View.GONE

                        if (categoryList.isNotEmpty()) {
                            loadSeriesForCategory(categoryList[0].categoryId)
                        }
                    },
                    onFailure = { exception ->
                        loadingText.visibility = View.GONE
                        errorText.text = "Failed to load categories: ${exception.message}"
                        errorText.visibility = View.VISIBLE
                    }
                )
            }
        }
    }

    private fun loadSeriesForCategory(categoryId: String) {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        
        android.util.Log.d("SeriesScreen", "=== LOADING SERIES FOR CATEGORY ===")
        android.util.Log.d("SeriesScreen", "Category ID: $categoryId")
        android.util.Log.d("SeriesScreen", "Selected Category Index: $selectedCategoryIndex")

        lifecycleScope.launch {
            repository.loadItemsWithSync("series", categoryId).collectLatest { result ->
                result.fold(
                    onSuccess = { seriesList ->
                        currentSeries = seriesList
                        android.util.Log.d("SeriesScreen", "=== SERIES LOADED SUCCESSFULLY ===")
                        android.util.Log.d("SeriesScreen", "Category ID: $categoryId")
                        android.util.Log.d("SeriesScreen", "Total Series Loaded: ${seriesList.size}")
                        
                        // Log first few series to see their data
                        seriesList.take(3).forEach { series ->
                            android.util.Log.d("SeriesScreen", "Series: ${series.name}, ID: ${series.itemId}, Category: ${series.categoryId}")
                        }
                        
                        seriesAdapter.updateSeries(seriesList)
                        selectedSeriesIndex = 0
                        loadingText.visibility = View.GONE
                        
                        // Update category count for current category
                        updateCategoryCount(selectedCategoryIndex, seriesList.size)
                    },
                    onFailure = { exception ->
                        loadingText.visibility = View.GONE
                        errorText.text = "Failed to load series: ${exception.message}"
                        errorText.visibility = View.VISIBLE
                        android.util.Log.e("SeriesScreen", "=== FAILED TO LOAD SERIES ===")
                        android.util.Log.e("SeriesScreen", "Category ID: $categoryId")
                        android.util.Log.e("SeriesScreen", "Error: ${exception.message}")
                        exception.printStackTrace()
                    }
                )
            }
        }
    }

    private fun updateCategoryCount(categoryIndex: Int, count: Int) {
        val adapter = categoryListView.adapter as? CategoryAdapter
        adapter?.updateCategoryCount(categoryIndex, count)
    }
    
    private fun updateCategoryVisualSelection() {
        for (i in 0 until categoryListView.count) {
            val view = categoryListView.getChildAt(i)
            if (view != null) {
                val card = view.findViewById<CardView>(R.id.categoryCard)
                if (i == selectedCategoryIndex) {
                    card.elevation = 24f
                    card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.selection_primary))
                    card.scaleX = 1.1f
                    card.scaleY = 1.1f
                } else {
                    card.elevation = 8f
                    card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background))
                    card.scaleX = 1.0f
                    card.scaleY = 1.0f
                }
            }
        }
    }
}
