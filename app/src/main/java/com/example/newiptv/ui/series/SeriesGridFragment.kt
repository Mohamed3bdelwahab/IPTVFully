package com.example.newiptv.ui.series

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.VerticalGridPresenter
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.ItemEntity
import com.example.newiptv.data.repository.TvRepository
import com.example.newiptv.ui.seriesinfo.SeriesInfoActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SeriesGridFragment : VerticalGridSupportFragment() {

    private val adapter = ArrayObjectAdapter(SeriesCardPresenter())
    private lateinit var repository: TvRepository
    private var selectedCategoryId: String? = null
    private var seriesList: List<ItemEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repository
        repository = TvRepository(DatabaseProvider.getDatabase(requireContext()))

        // Get category ID from arguments
        selectedCategoryId = arguments?.getString("categoryId")
        Log.d("SeriesGridFragment", "Category ID: $selectedCategoryId")

        setupGrid()
        loadSeries()
    }

    private fun setupGrid() {
        // Configure grid with 3 columns and zoom focus effect
        val gridPresenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false).apply {
            numberOfColumns = 3
        }
        setGridPresenter(gridPresenter)

        // Set adapter
        setAdapter(adapter)

        // Handle item clicks (DPAD_CENTER auto works!)
        setOnItemViewClickedListener { _, item, _, _ ->
            val series = item as ItemEntity
            Log.d("SeriesGridFragment", "Clicked on series: ${series.name}")
            
            // Navigate to SeriesInfoActivity
            val intent = Intent(requireContext(), SeriesInfoActivity::class.java).apply {
                putExtra("seriesId", series.itemId)
                putExtra("seriesName", series.name)
            }
            startActivity(intent)
        }
    }

    private fun loadSeries() {
        selectedCategoryId?.let { categoryId ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("SeriesGridFragment", "Loading series for category: $categoryId")
                    
                    // Load series with sync
                    repository.loadItemsWithSync("series", categoryId).collect { result ->
                        result.fold(
                            onSuccess = { items ->
                                seriesList = items
                                Log.d("SeriesGridFragment", "Loaded ${items.size} series")
                                
                                withContext(Dispatchers.Main) {
                                    // Clear and add all series to adapter
                                    adapter.clear()
                                    items.forEach { series ->
                                        adapter.add(series)
                                    }
                                    
                                    // Update category count if parent activity supports it
                                    (activity as? SeriesScreen)?.updateCategoryCount(
                                        getCategoryIndex(categoryId), 
                                        items.size
                                    )
                                }
                            },
                            onFailure = { e ->
                                Log.e("SeriesGridFragment", "Failed to load series", e)
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SeriesGridFragment", "Exception loading series", e)
                }
            }
        }
    }

    private fun getCategoryIndex(categoryId: String): Int {
        // This would need to be implemented based on how categories are stored
        // For now, return 0 as default
        return 0
    }

    fun updateSeriesList(newSeriesList: List<ItemEntity>) {
        seriesList = newSeriesList
        adapter.clear()
        newSeriesList.forEach { series ->
            adapter.add(series)
        }
    }

    fun getCurrentSeriesList(): List<ItemEntity> = seriesList
}
