package com.example.iptvtv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String? = null,
    val isFavorite: Boolean = false
)
