package com.example.newiptv.ui.favorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.FavoritePlaylistEntity

class PlaylistDialogAdapter(
    private val playlists: List<FavoritePlaylistEntity>,
    private val onPlaylistSelected: (FavoritePlaylistEntity) -> Unit
) : RecyclerView.Adapter<PlaylistDialogAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playlistName: TextView = itemView.findViewById(R.id.playlistName)
        val playlistItemCount: TextView = itemView.findViewById(R.id.playlistItemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_dialog, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        
        holder.playlistName.text = playlist.name
        holder.playlistItemCount.text = "${playlist.itemCount} items"
        
        holder.itemView.setOnClickListener {
            android.util.Log.d("PlaylistDialogAdapter", "🎯 Playlist selected: ${playlist.name}")
            onPlaylistSelected(playlist)
        }
        
        // Set focus change listener for TV remote navigation
        holder.itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                android.util.Log.d("PlaylistDialogAdapter", "🎯 Playlist focused: ${playlist.name}")
                holder.itemView.setBackgroundResource(R.drawable.item_focused_background)
            } else {
                holder.itemView.setBackgroundResource(R.drawable.item_default_background)
            }
        }
    }

    override fun getItemCount(): Int = playlists.size
}
