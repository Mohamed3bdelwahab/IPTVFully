package com.example.iptvtv.util

import com.example.iptvtv.data.model.Channel
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3UParser @Inject constructor() {
    fun parseM3U(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        
        var currentName = ""
        var currentUrl = ""
        var currentLogo = ""
        var currentGroup = ""
        
        for (line in lines) {
            when {
                line.startsWith("#EXTINF:") -> {
                    // Parse channel info
                    val nameMatch = Regex(",(.+)$").find(line)
                    currentName = nameMatch?.groupValues?.get(1) ?: ""
                    
                    val logoMatch = Regex("tvg-logo=\"([^\"]+)\"").find(line)
                    currentLogo = logoMatch?.groupValues?.get(1) ?: ""
                    
                    val groupMatch = Regex("group-title=\"([^\"]+)\"").find(line)
                    currentGroup = groupMatch?.groupValues?.get(1) ?: ""
                }
                line.startsWith("http") -> {
                    currentUrl = line.trim()
                    if (currentName.isNotEmpty() && currentUrl.isNotEmpty()) {
                        channels.add(
                            Channel(
                                id = UUID.randomUUID().toString(),
                                name = currentName,
                                streamUrl = currentUrl,
                                logoUrl = currentLogo.takeIf { it.isNotEmpty() },
                                category = currentGroup.takeIf { it.isNotEmpty() }
                            )
                        )
                        // Reset for next channel
                        currentName = ""
                        currentUrl = ""
                        currentLogo = ""
                        currentGroup = ""
                    }
                }
            }
        }
        
        return channels
    }
}
