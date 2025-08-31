package com.example.newiptv.ui.series

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.ItemEntity

class SeriesAdapter(
    private val onSeriesClick: (ItemEntity) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.SeriesViewHolder>() {

    private var seriesList = listOf<ItemEntity>()
    private var selectedIndex = RecyclerView.NO_POSITION

    class SeriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val seriesCard: CardView = itemView.findViewById(R.id.seriesCard)
        val seriesTitle: TextView = itemView.findViewById(R.id.seriesTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_series, parent, false)
        return SeriesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        val series = seriesList[position]
        holder.seriesTitle.text = series.name

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

        return@onBindViewHolder
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
