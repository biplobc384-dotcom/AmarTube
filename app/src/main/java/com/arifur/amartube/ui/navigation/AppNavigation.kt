package com.arifur.amartube.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arifur.amartube.ui.screens.*
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    // Hide bottom bar on player screen
    val showBottomBar = currentRoute?.startsWith("player") != true && currentRoute?.startsWith("music_player") != true

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { 
            if (showBottomBar) {
                BottomNavigationBar(navController = navController) 
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize() // Edge-to-edge
        ) {
            composable(Screen.Home.route) { 
                HomeScreen(
                    onVideoClick = { videoUrl ->
                        val encodedUrl = URLEncoder.encode(videoUrl, "UTF-8")
                        navController.navigate("player/$encodedUrl")
                    }
                ) 
            }
            composable(Screen.Tube.route) { 
                TubeScreen(
                    onVideoClick = { videoUrl ->
                        val encodedUrl = URLEncoder.encode(videoUrl, "UTF-8")
                        navController.navigate("player/$encodedUrl")
                    }
                ) 
            }
            composable(Screen.Music.route) { 
                MusicScreen(
                    onSongClick = { url ->
                        val encodedUrl = URLEncoder.encode(url, "UTF-8")
                        navController.navigate("music_player/$encodedUrl")
                    }
                ) 
            }
            composable(Screen.Browser.route) { BrowserScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            
            composable(
                route = "player/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                PlayerScreen(
                    url = decodedUrl,
                    onBack = { navController.popBackStack() },
                    onRelatedVideoClick = { videoUrl ->
                        val newEncodedUrl = URLEncoder.encode(videoUrl, "UTF-8")
                        navController.navigate("player/$newEncodedUrl")
                    }
                )
            }
            composable(
                route = "music_player/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                MusicPlayerScreen(
                    url = decodedUrl,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
