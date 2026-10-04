package com.arifur.amartube.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Tube : Screen("tube", "Tube", Icons.Filled.OndemandVideo)
    object Music : Screen("music", "Music", Icons.Filled.PlayArrow)
    object Browser : Screen("browser", "Browser", Icons.Filled.Search)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}
