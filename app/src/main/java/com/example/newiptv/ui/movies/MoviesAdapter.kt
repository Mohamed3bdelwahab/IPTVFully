package com.example.newiptv.ui.movies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.MovieItemEntity
import com.example.newiptv.utils.KeyEventLogger

class MoviesAdapter(
    private val onMovieClick: (MovieItemEntity) -> Unit
) : RecyclerView.Adapter<MoviesAdapter.MovieViewHolder>() {

    private var movies: List<MovieItemEntity> = emptyList()

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val movieCover: ImageView = itemView.findViewById(R.id.movieCover)
        private val movieTitle: TextView = itemView.findViewById(R.id.movieTitle)
        private val movieRating: TextView = itemView.findViewById(R.id.movieRating)
        private val movieYear: TextView = itemView.findViewById(R.id.movieYear)

        fun bind(movie: MovieItemEntity) {
            movieTitle.text = movie.name
            movieRating.text = movie.rating5Based?.toString() ?: "N/A"
            
            // Extract year from added timestamp or name
            val year = extractYear(movie)
            movieYear.text = year

            // Load movie cover image
            movie.streamIcon?.let { iconUrl ->
                Glide.with(itemView.context)
                    .load(iconUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(movieCover)
            } ?: run {
                movieCover.setImageResource(R.drawable.placeholder_image)
            }

            // Set click listener
            itemView.setOnClickListener {
                onMovieClick(movie)
                KeyEventLogger.logItemSelection("MoviesAdapter", "Movie", adapterPosition, movie.name)
            }

            // Set focus change listener for scale animation
            itemView.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                    android.util.Log.d("MoviesAdapter", "Movie $adapterPosition got FOCUS")
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                }
            }
        }

        private fun extractYear(movie: MovieItemEntity): String {
            // Try to extract year from name first
            val nameYear = movie.name.takeLastWhile { it.isDigit() }.take(4)
            if (nameYear.length == 4 && nameYear.toIntOrNull() in 1900..2030) {
                return nameYear
            }
            
            // Try to extract year from added timestamp
            movie.added?.let { timestamp ->
                if (timestamp.length >= 4) {
                    val timestampYear = timestamp.take(4)
                    if (timestampYear.toIntOrNull() in 1900..2030) {
                        return timestampYear
                    }
                }
            }
            
            return "N/A"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]
        holder.bind(movie)
    }

    override fun getItemCount(): Int = movies.size

    fun updateMovies(newMovies: List<MovieItemEntity>) {
        movies = newMovies
        notifyDataSetChanged()
        KeyEventLogger.logScreenEvent("MoviesAdapter", "Updated movies: ${newMovies.size} items")
    }

    fun getMovieAt(position: Int): MovieItemEntity? {
        return if (position < movies.size) movies[position] else null
    }
}
