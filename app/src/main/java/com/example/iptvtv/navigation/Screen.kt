package com.example.iptvtv.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object ChannelList : Screen("channel_list")
    object Player : Screen("player/{channelId}") {
        val arguments = listOf(
            navArgument("channelId") { type = NavType.StringType }
        )
        
        fun createRoute(channelId: String) = "player/$channelId"
    }
    object Settings : Screen("settings")
    object Favorites : Screen("favorites")
}
