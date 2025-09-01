package com.example.newiptv.ui.series

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.ItemEntity

class SeriesAdapter(
    private val onSeriesClick: (ItemEntity) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.SeriesViewHolder>() {

    private var seriesList = listOf<ItemEntity>()
    private var selectedIndex = RecyclerView.NO_POSITION

    class SeriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val seriesCard: CardView = itemView.findViewById(R.id.seriesCard)
        val seriesCover: ImageView = itemView.findViewById(R.id.seriesCover)
        val seriesTitle: TextView = itemView.findViewById(R.id.seriesTitle)
        val seriesRating: TextView = itemView.findViewById(R.id.seriesRating)
        val seriesYear: TextView = itemView.findViewById(R.id.seriesYear)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_series, parent, false)
        return SeriesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        val series = seriesList[position]
        
        // Set series title
        holder.seriesTitle.text = series.name
        
        // Load cover image using Glide
        if (!series.cover.isNullOrEmpty()) {
            Glide.with(holder.seriesCover.context)
                .load(series.cover)
                .placeholder(R.color.panel_background)
                .error(R.color.panel_background)
                .centerCrop()
                .into(holder.seriesCover)
        } else {
            holder.seriesCover.setImageResource(R.color.panel_background)
        }
        
            // Set rating with both rating and rating_5based
    val rating5Based = series.rating5Based ?: 0.0
    val ratingString = series.rating ?: ""
    
    if (rating5Based > 0 || ratingString.isNotEmpty()) {
        val ratingText = if (ratingString.isNotEmpty() && rating5Based > 0) {
            "★ ${String.format("%.1f", rating5Based)} (${ratingString})"
        } else if (rating5Based > 0) {
            "★ ${String.format("%.1f", rating5Based)}"
        } else {
            "★ $ratingString"
        }
        holder.seriesRating.text = ratingText
        holder.seriesRating.visibility = android.view.View.VISIBLE
    } else {
        holder.seriesRating.visibility = android.view.View.GONE
    }
        
        // Set year from release date
        val year = series.releaseDate?.let { date ->
            try {
                date.split("-").firstOrNull() ?: ""
            } catch (e: Exception) {
                ""
            }
        } ?: ""
        
        if (year.isNotEmpty()) {
            holder.seriesYear.text = "($year)"
            holder.seriesYear.visibility = android.view.View.VISIBLE
        } else {
            holder.seriesYear.visibility = android.view.View.GONE
        }

        // Set focus change listener for scale animation
        holder.seriesCard.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                selectedIndex = position
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                android.util.Log.d("SeriesAdapter", "Series $position got FOCUS")
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
            }
        }

        // Set click listener
        holder.seriesCard.setOnClickListener {
            onSeriesClick(series)
        }
    }

    override fun getItemCount(): Int = seriesList.size

    fun updateSeries(newSeries: List<ItemEntity>) {
        seriesList = newSeries
        selectedIndex = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun setSelectedIndex(index: Int) {
        val oldIndex = selectedIndex
        selectedIndex = index
        notifyItemChanged(oldIndex)
        notifyItemChanged(selectedIndex)
    }

    fun getSeriesList(): List<ItemEntity> = seriesList

    fun getSeriesAt(index: Int): ItemEntity? {
        return if (index in seriesList.indices) seriesList[index] else null
    }
}
