package com.example.iptvtv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.example.iptvtv.data.model.Channel
import com.example.iptvtv.ui.components.ChannelCard

@Composable
fun FavoritesScreen(
    onChannelClick: (Channel) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var favoriteChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Sample favorite channels for demonstration
    LaunchedEffect(Unit) {
        favoriteChannels = listOf(
            Channel("1", "BBC News", "http://example.com/bbc.m3u8", category = "News", isFavorite = true),
            Channel("3", "ESPN", "http://example.com/espn.m3u8", category = "Sports", isFavorite = true)
        )
        isLoading = false
    }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            TvLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Favorite Channels",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Button(onClick = onBackPressed) {
                            Text("Back")
                        }
                    }
                }
                
                if (favoriteChannels.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No favorite channels yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(favoriteChannels) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
