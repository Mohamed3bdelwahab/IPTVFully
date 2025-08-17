package com.example.iptvtv.data.repository

import com.example.iptvtv.data.local.ChannelDao
import com.example.iptvtv.data.local.EPGDao
import com.example.iptvtv.data.local.PlaylistConfigDao
import com.example.iptvtv.data.model.Channel
import com.example.iptvtv.data.model.EPGProgram
import com.example.iptvtv.data.model.PlaylistConfig
import com.example.iptvtv.data.remote.IPTVService
import com.example.iptvtv.data.remote.EPGService
import com.example.iptvtv.util.M3UParser
import com.example.iptvtv.util.EPGParser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    private val channelDao: ChannelDao,
    private val epgDao: EPGDao,
    private val playlistConfigDao: PlaylistConfigDao,
    private val iptvService: IPTVService,
    private val epgService: EPGService,
    private val m3uParser: M3UParser,
    private val epgParser: EPGParser
) {
    fun getAllChannels(): Flow<List<Channel>> = channelDao.getAllChannels()
    
    fun getFavoriteChannels(): Flow<List<Channel>> = channelDao.getFavoriteChannels()
    
    fun getChannelsByCategory(category: String): Flow<List<Channel>> = 
        channelDao.getChannelsByCategory(category)
    
    suspend fun loadChannelsFromM3U(m3uUrl: String): List<Channel> {
        return try {
            val m3uContent = iptvService.getM3UPlaylist(m3uUrl)
            val channels = m3uParser.parseM3U(m3uContent)
            if (channels.isNotEmpty()) {
                channelDao.insertChannels(channels)
            }
            channels
        } catch (e: Exception) {
            throw Exception("Failed to load M3U playlist: ${e.message}")
        }
    }
    
    suspend fun loadEPGFromUrl(epgUrl: String): List<EPGProgram> {
        return try {
            val epgContent = epgService.getEPGData(epgUrl)
            val programs = epgParser.parseEPG(epgContent)
            if (programs.isNotEmpty()) {
                epgDao.insertPrograms(programs)
            }
            programs
        } catch (e: Exception) {
            throw Exception("Failed to load EPG data: ${e.message}")
        }
    }
    
    fun getProgramsForChannel(channelId: String): Flow<List<EPGProgram>> {
        return epgDao.getProgramsForChannel(channelId)
    }
    
    fun getAllPlaylists(): Flow<List<PlaylistConfig>> {
        return playlistConfigDao.getAllPlaylists()
    }
    
    suspend fun savePlaylist(playlist: PlaylistConfig) {
        playlistConfigDao.insertPlaylist(playlist)
    }
    
    suspend fun deletePlaylist(playlist: PlaylistConfig) {
        playlistConfigDao.deletePlaylist(playlist)
    }
    
    suspend fun insertSampleChannels(channels: List<Channel>) {
        channelDao.insertChannels(channels)
    }
    
    suspend fun toggleFavorite(channel: Channel) {
        channelDao.updateChannel(channel.copy(isFavorite = !channel.isFavorite))
    }
    
    suspend fun updateChannel(channel: Channel) {
        channelDao.updateChannel(channel)
    }
}
