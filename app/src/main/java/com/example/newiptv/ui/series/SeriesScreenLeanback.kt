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
import androidx.fragment.app.FragmentTransaction
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.CategoryEntity
import com.example.newiptv.data.db.entities.ItemEntity
import com.example.newiptv.data.repository.TvRepository
import com.example.newiptv.ui.seriesinfo.SeriesInfoActivity
import com.example.newiptv.utils.KeyEventLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SeriesScreenLeanback : AppCompatActivity() {

    private lateinit var categoryListView: ListView
    private lateinit var loadingText: TextView
    private lateinit var errorText: TextView
    private lateinit var filterSpinner: Spinner

    private var selectedCategoryIndex = 0
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

    private lateinit var repository: TvRepository
    private var categories: List<CategoryEntity> = emptyList()
    private var currentSeriesList: List<ItemEntity> = emptyList()
    private var currentSeriesGridFragment: SeriesGridFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_screen_leanback)

        val database = DatabaseProvider.getDatabase(this)
        repository = TvRepository(database)

        initializeViews()
        setupCategoryList()
        setupTVRemoteNavigation()
        loadCategories()
    }

    private fun initializeViews() {
        categoryListView = findViewById(R.id.categoryListView)
        loadingText = findViewById(R.id.loadingText)
        errorText = findViewById(R.id.errorText)
        filterSpinner = findViewById(R.id.filterSpinner)
        
        // Set initial focus to category panel
        categoryListView.requestFocus()
        isInCategoryPanel = true
        selectedCategoryIndex = 0
        
        // Setup filter spinner
        setupFilterSpinner()
        
        // Ensure proper focus handling
        KeyEventLogger.logScreenEvent("SeriesScreenLeanback", "Views initialized")
        KeyEventLogger.logFocusChange("SeriesScreenLeanback", "Category", selectedCategoryIndex)
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
                android.util.Log.d("SeriesScreenLeanback", "=== ON ITEM SELECTED CALLED ===")
                android.util.Log.d("SeriesScreenLeanback", "Position: $position, Selected Index: $selectedCategoryIndex")
                android.util.Log.d("SeriesScreenLeanback", "Categories size: ${categories.size}")
                
                if (position < categories.size) {
                    val category = categories[position]
                    android.util.Log.d("SeriesScreenLeanback", "Category: ${category.categoryName} (ID: ${category.categoryId})")
                    
                    // Only update selection index, don't load series automatically
                    if (position != selectedCategoryIndex) {
                        selectedCategoryIndex = position
                        android.util.Log.d("SeriesScreenLeanback", "=== CATEGORY FOCUS CHANGED (TV REMOTE) ===")
                        android.util.Log.d("SeriesScreenLeanback", "New Position: $position")
                        android.util.Log.d("SeriesScreenLeanback", "Category ID: ${category.categoryId}")

                        // ✅ Only update visual selection, don't load series yet
                        updateCategorySelection()
                    } else {
                        android.util.Log.d("SeriesScreenLeanback", "Same position, no change needed")
                    }
                } else {
                    android.util.Log.e("SeriesScreenLeanback", "Invalid position: $position, categories size: ${categories.size}")
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                android.util.Log.d("SeriesScreenLeanback", "=== NOTHING SELECTED ===")
            }
        })
        
        // Add click listener as backup for mouse clicks
        categoryListView.setOnItemClickListener { _, _, position, _ ->
            android.util.Log.d("SeriesScreenLeanback", "=== CATEGORY CLICKED (MOUSE) ===")
            android.util.Log.d("SeriesScreenLeanback", "Position: $position")
            
            if (position < categories.size) {
                selectedCategoryIndex = position
                val category = categories[position]
                android.util.Log.d("SeriesScreenLeanback", "Category Name: ${category.categoryName}")
                android.util.Log.d("SeriesScreenLeanback", "Category ID: ${category.categoryId}")
                loadSeriesForCategory(category.categoryId)
                updateCategorySelection()
            }
        }
    }
    
    private fun applyFilter(filterType: String) {
        val currentSeries = currentSeriesList.toMutableList()
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
        
        currentSeriesList = sortedSeries
        currentSeriesGridFragment?.updateSeriesList(sortedSeries)
        KeyEventLogger.logScreenEvent("SeriesScreenLeanback", "Applied filter: $filterType")
    }

    private fun setupTVRemoteNavigation() {
        // TV remote navigation is now handled by Leanback automatically
        // We only need to handle panel switching and Enter key for category selection
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            KeyEventLogger.logKeyEvent("SeriesScreenLeanback", event)
            
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (isInCategoryPanel) {
                        selectCurrentCategory()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (isInCategoryPanel && selectedCategoryIndex < categories.size) {
                        // Switch to series panel
                        isInCategoryPanel = false
                        KeyEventLogger.logNavigation("SeriesScreenLeanback", "Category → Series")
                        
                        // Load series for the selected category
                        val category = categories[selectedCategoryIndex]
                        loadSeriesForCategory(category.categoryId)
                        
                        // Focus will be handled by Leanback automatically
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (!isInCategoryPanel) {
                        // Switch back to category panel
                        isInCategoryPanel = true
                        categoryListView.requestFocus()
                        KeyEventLogger.logNavigation("SeriesScreenLeanback", "Series → Category")
                        return true
                    }
                }
            }
        }
        
        return super.dispatchKeyEvent(event)
    }

    private fun selectCurrentCategory() {
        if (selectedCategoryIndex < categories.size) {
            val category = categories[selectedCategoryIndex]
            android.util.Log.d("SeriesScreenLeanback", "=== SELECTING CATEGORY ===")
            android.util.Log.d("SeriesScreenLeanback", "Category: ${category.categoryName}")
            android.util.Log.d("SeriesScreenLeanback", "Category ID: ${category.categoryId}")
            
            KeyEventLogger.logItemSelection("SeriesScreenLeanback", "Category", selectedCategoryIndex, category.categoryName)
            
            // Load series for this category
            loadSeriesForCategory(category.categoryId)
        }
    }

    private fun updateCategorySelection() {
        // Update visual selection in category list
        categoryListView.setSelection(selectedCategoryIndex)
        KeyEventLogger.logFocusChange("SeriesScreenLeanback", "Category", selectedCategoryIndex)
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                loadingText.visibility = View.VISIBLE
                errorText.visibility = View.GONE
                
                repository.loadCategoriesWithSync("series").collectLatest { result ->
                    result.fold(
                        onSuccess = { categoryList ->
                            categories = categoryList
                            loadingText.visibility = View.GONE
                            
                            val categoryAdapter = CategoryAdapter(this@SeriesScreenLeanback, categoryList)
                            categoryListView.adapter = categoryAdapter
                            
                            android.util.Log.d("SeriesScreenLeanback", "Loaded ${categoryList.size} categories")
                            
                            // Auto-load first category if available
                            if (categoryList.isNotEmpty()) {
                                selectedCategoryIndex = 0
                                updateCategorySelection()
                            }
                        },
                        onFailure = { e ->
                            loadingText.visibility = View.GONE
                            errorText.text = "Failed to load categories: ${e.message}"
                            errorText.visibility = View.VISIBLE
                            android.util.Log.e("SeriesScreenLeanback", "Failed to load categories", e)
                        }
                    )
                }
            } catch (e: Exception) {
                loadingText.visibility = View.GONE
                errorText.text = "Error: ${e.message}"
                errorText.visibility = View.VISIBLE
                android.util.Log.e("SeriesScreenLeanback", "Exception fetching categories", e)
            }
        }
    }

    private fun loadSeriesForCategory(categoryId: String) {
        if (selectedCategoryIndex < categories.size) {
            val category = categories[selectedCategoryIndex]
            android.util.Log.d("SeriesScreenLeanback", "=== LOADING SERIES FOR CATEGORY ===")
            android.util.Log.d("SeriesScreenLeanback", "Category: ${category.categoryName}")
            android.util.Log.d("SeriesScreenLeanback", "Category ID: $categoryId")
            
            // Create or update the SeriesGridFragment
            val fragment = SeriesGridFragment().apply {
                arguments = Bundle().apply {
                    putString("categoryId", categoryId)
                }
            }
            
            currentSeriesGridFragment = fragment
            
            // Replace the fragment container
            supportFragmentManager.beginTransaction()
                .replace(R.id.seriesFragmentContainer, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit()
            
            KeyEventLogger.logDataLoading("SeriesScreenLeanback", "Series", category.categoryName)
        }
    }

    fun updateCategoryCount(categoryIndex: Int, count: Int) {
        // Update category count in the adapter
        (categoryListView.adapter as? CategoryAdapter)?.updateCategoryCount(categoryIndex, count)
    }
}
