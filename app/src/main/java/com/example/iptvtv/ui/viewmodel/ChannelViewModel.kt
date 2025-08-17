package com.example.iptvtv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvtv.data.model.Channel
import com.example.iptvtv.data.repository.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {
    
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()
    
    private val _favoriteChannels = MutableStateFlow<List<Channel>>(emptyList())
    val favoriteChannels: StateFlow<List<Channel>> = _favoriteChannels.asStateFlow()
    
    private val _playlists = MutableStateFlow<List<PlaylistConfig>>(emptyList())
    val playlists: StateFlow<List<PlaylistConfig>> = _playlists.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    init {
        loadChannelsFromDatabase()
        loadFavoriteChannels()
        loadPlaylists()
    }
    
    private fun loadChannelsFromDatabase() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                channelRepository.getAllChannels().collect { channels ->
                    if (channels.isEmpty()) {
                        // Load sample data if database is empty
                        loadSampleChannels()
                    } else {
                        _channels.value = channels
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load channels: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadSampleChannels() {
        viewModelScope.launch {
            try {
                // Load sample channels for demonstration
                val sampleChannels = listOf(
                    Channel("1", "BBC News", "http://example.com/bbc.m3u8", category = "News"),
                    Channel("2", "CNN", "http://example.com/cnn.m3u8", category = "News"),
                    Channel("3", "ESPN", "http://example.com/espn.m3u8", category = "Sports"),
                    Channel("4", "Discovery", "http://example.com/discovery.m3u8", category = "Documentary"),
                    Channel("5", "HBO", "http://example.com/hbo.m3u8", category = "Entertainment"),
                    Channel("6", "Netflix", "http://example.com/netflix.m3u8", category = "Entertainment")
                )
                channelRepository.insertSampleChannels(sampleChannels)
                _channels.value = sampleChannels
            } catch (e: Exception) {
                _error.value = "Failed to load sample channels: ${e.message}"
            }
        }
    }
    
    private fun loadPlaylists() {
        viewModelScope.launch {
            try {
                channelRepository.getAllPlaylists().collect { playlists ->
                    _playlists.value = playlists
                }
            } catch (e: Exception) {
                _error.value = "Failed to load playlists: ${e.message}"
            }
        }
    }
    
    private fun loadFavoriteChannels() {
        viewModelScope.launch {
            try {
                channelRepository.getFavoriteChannels().collect { favorites ->
                    _favoriteChannels.value = favorites
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            try {
                channelRepository.toggleFavorite(channel)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun loadChannelsFromM3U(m3uUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // Clear previous errors
            _successMessage.value = null
            try {
                val channels = channelRepository.loadChannelsFromM3U(m3uUrl)
                _channels.value = channels
                _successMessage.value = "Successfully loaded ${channels.size} channels"
            } catch (e: Exception) {
                _error.value = "Failed to load playlist: ${e.message}"
                println("Error loading M3U: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadEPGFromUrl(epgUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null
            try {
                val programs = channelRepository.loadEPGFromUrl(epgUrl)
                _successMessage.value = "Successfully loaded ${programs.size} programs"
            } catch (e: Exception) {
                _error.value = "Failed to load EPG: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun savePlaylist(playlist: PlaylistConfig) {
        viewModelScope.launch {
            try {
                channelRepository.savePlaylist(playlist)
                _successMessage.value = "Playlist saved successfully"
            } catch (e: Exception) {
                _error.value = "Failed to save playlist: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
