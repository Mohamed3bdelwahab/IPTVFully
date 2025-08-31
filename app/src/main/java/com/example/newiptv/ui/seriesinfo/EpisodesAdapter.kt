package com.example.newiptv.ui.seriesinfo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.EpisodeEntity

class EpisodesAdapter(
    private val onEpisodeClick: (EpisodeEntity) -> Unit
) : RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder>() {

    private var episodesList = listOf<EpisodeEntity>()
    private var selectedIndex = RecyclerView.NO_POSITION

    class EpisodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val episodeCard: CardView = itemView.findViewById(R.id.episodeCard)
        val episodeTitle: TextView = itemView.findViewById(R.id.episodeTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return EpisodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = episodesList[position]
        holder.episodeTitle.text = episode.title ?: "Episode ${episode.episodeNum}"

        // Set focus change listener for scale animation
        holder.episodeCard.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                selectedIndex = position
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                android.util.Log.d("EpisodesAdapter", "Episode $position got FOCUS")
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
            }
        }

        // Set click listener
        holder.episodeCard.setOnClickListener {
            onEpisodeClick(episode)
        }

        return@onBindViewHolder
    }

    override fun getItemCount(): Int = episodesList.size

    fun updateEpisodes(newEpisodes: List<EpisodeEntity>) {
        episodesList = newEpisodes
        selectedIndex = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun setSelectedIndex(index: Int) {
        val oldIndex = selectedIndex
        selectedIndex = index
        notifyItemChanged(oldIndex)
        notifyItemChanged(selectedIndex)
    }

    fun getEpisodesList(): List<EpisodeEntity> = episodesList

    fun getEpisodeAt(index: Int): EpisodeEntity? {
        return if (index in episodesList.indices) episodesList[index] else null
    }
}
