package com.example.newiptv.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ListView
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

class SeriesScreen : AppCompatActivity() {

    private lateinit var categoryListView: ListView
    private lateinit var seriesRecyclerView: RecyclerView
    private lateinit var seriesAdapter: SeriesAdapter
    private lateinit var loadingText: TextView
    private lateinit var errorText: TextView

    private var selectedCategoryIndex = 0
    private var selectedSeriesIndex = 0
    private var isInCategoryPanel = true

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
                    
                    // Always update selection and load series when position changes
                    if (position != selectedCategoryIndex) {
                        selectedCategoryIndex = position
                        android.util.Log.d("SeriesScreen", "=== CATEGORY SELECTION CHANGED (TV REMOTE) ===")
                        android.util.Log.d("SeriesScreen", "New Position: $position")
                        android.util.Log.d("SeriesScreen", "Category ID: ${category.categoryId}")

                        // ✅ Trigger series loading immediately when selection changes
                        loadSeriesForCategory(category.categoryId)
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

        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (!isInCategoryPanel && selectedSeriesIndex % 3 == 0) {
                            isInCategoryPanel = true
                            categoryListView.requestFocus()
                            return@setOnKeyListener true
                        }
                        false
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (isInCategoryPanel) {
                            isInCategoryPanel = false
                            seriesRecyclerView.requestFocus()
                            return@setOnKeyListener true
                        }
                        false
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (isInCategoryPanel) {
                            if (selectedCategoryIndex < categories.size) {
                                val category = categories[selectedCategoryIndex]
                                loadSeriesForCategory(category.categoryId)
                            }
                        } else {
                            val series = seriesAdapter.getSeriesAt(selectedSeriesIndex)
                            if (series != null) {
                                val categoryName = if (selectedCategoryIndex < categories.size) categories[selectedCategoryIndex].categoryName else ""
                                val intent = Intent(this, SeriesInfoScreen::class.java).apply {
                                    putExtra("series_name", series.name)
                                    putExtra("series_category", categoryName)
                                    putExtra("series_id", series.itemId)
                                }
                                startActivity(intent)
                            }
                        }
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        finish()
                        true
                    }
                    else -> false
                }
            } else false
        }
    }

    private fun updateCategorySelection() {
        categoryListView.setSelection(selectedCategoryIndex)
        updateCategoryVisualSelection()
    }

    private fun loadCategories() {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE

        lifecycleScope.launch {
            repository.loadCategoriesWithSync("series").collectLatest { result ->
                result.fold(
                    onSuccess = { categoryList ->
                        categories = categoryList
                        val categoryNames = categoryList.map { it.categoryName }
                        val categoryAdapter = CategoryAdapter(this@SeriesScreen, categoryNames)
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
