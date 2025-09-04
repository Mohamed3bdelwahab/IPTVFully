package com.example.newiptv.ui.movies

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.MovieCategoryEntity
import com.example.newiptv.utils.KeyEventLogger

class MovieCategoryAdapter(
    context: Context,
    private var categories: List<MovieCategoryEntity>
) : ArrayAdapter<MovieCategoryEntity>(context, 0, categories) {

    private val inflater = LayoutInflater.from(context)
    private val categoryCounts = mutableMapOf<Int, Int>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_movie_category, parent, false)
        
        val category = getItem(position)
        if (category != null) {
            val categoryNameText = view.findViewById<TextView>(R.id.categoryNameText)
            val categoryCountText = view.findViewById<TextView>(R.id.categoryCountText)
            
            categoryNameText.text = category.categoryName
            categoryCountText.text = categoryCounts[position]?.toString() ?: "0"
            
            // Set focus change listener for scale animation and color logging
            view.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                    
                    // Log focus state and colors
                    val categoryTextColor = categoryNameText.currentTextColor
                    val countTextColor = categoryCountText.currentTextColor
                    
                    android.util.Log.d("MovieCategoryAdapter", "🎯 Category $position got FOCUS")
                    android.util.Log.d("MovieCategoryAdapter", "   🎨 Category Name: ${category.categoryName}")
                    android.util.Log.d("MovieCategoryAdapter", "   🔢 Movie Count: ${categoryCounts[position] ?: 0}")
                    android.util.Log.d("MovieCategoryAdapter", "   🟢 Category Text Color: #${String.format("%06X", 0xFFFFFF and categoryTextColor)}")
                    android.util.Log.d("MovieCategoryAdapter", "   🔢 Count Text Color: #${String.format("%06X", 0xFFFFFF and countTextColor)}")
                    
                    // Force background refresh
                    v.invalidate()
                    
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    
                    // Log unfocus state
                    val categoryTextColor = categoryNameText.currentTextColor
                    val countTextColor = categoryCountText.currentTextColor
                    
                    android.util.Log.d("MovieCategoryAdapter", "❌ Category $position lost FOCUS")
                    android.util.Log.d("MovieCategoryAdapter", "   🟢 Category Text Color: #${String.format("%06X", 0xFFFFFF and categoryTextColor)}")
                    android.util.Log.d("MovieCategoryAdapter", "   🔢 Count Text Color: #${String.format("%06X", 0xFFFFFF and countTextColor)}")
                }
            }
        }
        
        return view
    }

    fun updateCategories(newCategories: List<MovieCategoryEntity>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    fun updateCategoryCount(categoryIndex: Int, count: Int) {
        categoryCounts[categoryIndex] = count
        notifyDataSetChanged()
    }

    fun getCategoryAt(position: Int): MovieCategoryEntity? {
        return if (position < categories.size) categories[position] else null
    }
}
