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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
            androidx.compose.foundation.layout.Column {
                if (showBottomBar) {
                    val mediaController = com.arifur.amartube.core.rememberMediaController()
                    var hasMedia by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    var title by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
                    var isPlaying by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    
                    androidx.compose.runtime.LaunchedEffect(mediaController) {
                        while(true) {
                            if (mediaController != null) {
                                hasMedia = mediaController.playbackState != androidx.media3.common.Player.STATE_IDLE && 
                                           mediaController.playbackState != androidx.media3.common.Player.STATE_ENDED &&
                                           mediaController.currentMediaItem != null
                                title = mediaController.currentMediaItem?.mediaMetadata?.title?.toString() 
                                    ?: mediaController.currentMediaItem?.localConfiguration?.uri?.lastPathSegment 
                                    ?: "Playing media"
                                isPlaying = mediaController.isPlaying
                            } else {
                                hasMedia = false
                            }
                            kotlinx.coroutines.delay(500)
                        }
                    }
                    
                    if (hasMedia) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(Color(0xFF2A2A2A))
                                .clickable {
                                    val uri = mediaController?.currentMediaItem?.localConfiguration?.uri?.toString()
                                    if (uri != null) {
                                        val encodedUrl = URLEncoder.encode(uri, "UTF-8")
                                        navController.navigate("music_player/$encodedUrl") {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            androidx.compose.material3.Text(
                                text = title,
                                color = Color.White,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                androidx.compose.material3.IconButton(
                                    onClick = { 
                                        if (isPlaying) mediaController?.pause() else mediaController?.play() 
                                    }
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White
                                    )
                                }
                                androidx.compose.material3.IconButton(
                                    onClick = { 
                                        mediaController?.stop()
                                        mediaController?.clearMediaItems()
                                    }
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                    BottomNavigationBar(navController = navController) 
                }
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
