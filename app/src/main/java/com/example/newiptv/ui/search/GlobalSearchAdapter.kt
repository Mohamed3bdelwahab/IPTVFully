package com.example.newiptv.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.ItemEntity

/**
 * Adapter for global search results
 */
class GlobalSearchAdapter(
    private val onItemClick: (ItemEntity) -> Unit
) : RecyclerView.Adapter<GlobalSearchAdapter.SearchResultViewHolder>() {
    
    private var searchResults = listOf<ItemEntity>()
    private var focusedPosition = 0
    
    fun updateResults(results: List<ItemEntity>) {
        searchResults = results
        notifyDataSetChanged()
    }
    
    fun setFocusedPosition(position: Int) {
        val oldPosition = focusedPosition
        focusedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(focusedPosition)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val item = searchResults[position]
        holder.bind(item, position == focusedPosition)
    }
    
    override fun getItemCount(): Int = searchResults.size
    
    inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImage: ImageView = itemView.findViewById(R.id.posterImage)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val typeText: TextView = itemView.findViewById(R.id.typeText)
        private val yearText: TextView = itemView.findViewById(R.id.yearText)
        
        fun bind(item: ItemEntity, isFocused: Boolean) {
            titleText.text = item.name
            typeText.text = item.type.replaceFirstChar { it.uppercase() }
            
            // Extract year from release date
            val year = item.releaseDate?.take(4) ?: "Unknown"
            yearText.text = year
            
            // Load poster image
            if (!item.cover.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(item.cover)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(posterImage)
            } else {
                posterImage.setImageResource(R.drawable.ic_image_placeholder)
            }
            
            // Set focus state
            itemView.setBackgroundResource(
                if (isFocused) R.drawable.item_focused_background
                else R.drawable.item_default_background
            )
            
            // Set click listener
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
