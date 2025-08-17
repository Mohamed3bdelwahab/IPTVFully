package com.example.iptvtv.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.iptvtv.ui.screens.ChannelListScreen
import com.example.iptvtv.ui.screens.PlayerScreen
import com.example.iptvtv.ui.screens.SettingsScreen
import com.example.iptvtv.ui.screens.FavoritesScreen

@Composable
fun IPTVNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.ChannelList.route
    ) {
        composable(route = Screen.ChannelList.route) {
            ChannelListScreen(
                onChannelClick = { channel ->
                    navController.navigate(Screen.Player.createRoute(channel.id))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onFavoritesClick = {
                    navController.navigate(Screen.Favorites.route)
                },
                onToggleFavorite = { channel ->
                    // Handled by ViewModel
                }
            )
        }
        
        composable(
            route = Screen.Player.route,
            arguments = Screen.Player.arguments
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
            PlayerScreen(
                channelId = channelId,
                onBackPressed = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBackPressed = { navController.popBackStack() },
                onPlaylistAdded = { config ->
                    // TODO: Handle playlist addition
                    navController.popBackStack()
                }
            )
        }
        
        composable(route = Screen.Favorites.route) {
            FavoritesScreen(
                onChannelClick = { channel ->
                    navController.navigate(Screen.Player.createRoute(channel.id))
                },
                onBackPressed = { navController.popBackStack() }
            )
        }
    }
}
