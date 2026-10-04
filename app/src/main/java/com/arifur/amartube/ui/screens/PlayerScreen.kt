package com.arifur.amartube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@androidx.media3.common.util.UnstableApi
@Composable
fun PlayerScreen(
    url: String, 
    viewModel: PlayerViewModel = viewModel(), 
    downloaderViewModel: DownloaderViewModel = viewModel(),
    onBack: () -> Unit,
    onRelatedVideoClick: (String) -> Unit
) {
    val context = LocalContext.current
    var isBackgroundPlayEnabled by remember { mutableStateOf(false) }
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    LaunchedEffect(url) {
        viewModel.loadVideo(url)
    }

    Column(modifier = Modifier.fillMaxSize().let { if (isLandscape) it else it.statusBarsPadding() }.background(MaterialTheme.colorScheme.background)) {
        
        // --- 1. VIDEO PLAYER SECTION (Fixed at Top) ---
        if (viewModel.isLoading.value) {
            Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp).background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (viewModel.error.value != null) {
            Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp).background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { viewModel.loadVideo(url) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Network Error. Tap to retry.", color = Color.White, fontSize = 14.sp)
                }
            }
        } else {
            val playUrl = viewModel.streamUrl.value
            if (playUrl != null) {
                val mediaController = com.arifur.amartube.core.rememberMediaController()
                

                LaunchedEffect(playUrl, mediaController) {
                    val controller = mediaController
                    if (controller != null) {
                        val mediaItem = MediaItem.fromUri(playUrl)
                        controller.setMediaItem(mediaItem)
                        controller.prepare()
                        controller.playWhenReady = true
                    }
                }
                
                // Pause playback when leaving the screen
                DisposableEffect(mediaController, isBackgroundPlayEnabled) {
                    onDispose {
                        if (!isBackgroundPlayEnabled) {
                            mediaController?.pause()
                        }
                    }
                }
                
                // Pause playback when the app goes into the background
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, mediaController, isBackgroundPlayEnabled) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                            if (!isBackgroundPlayEnabled) {
                                mediaController?.pause()
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val currentController = mediaController
                if (currentController != null) {
                    Box(modifier = if (isLandscape) {
                        Modifier.fillMaxSize().background(Color.Black)
                    } else {
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
                    }) {
                        AndroidView(
                            factory = { ctx ->
                                val view = PlayerView(ctx)
                                view.apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    player = currentController
                                    useController = false
                                }
                                view
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        CustomPlayerUI(
                            player = currentController,
                            isLandscape = isLandscape,
                            onToggleFullscreen = {
                                val activity = context as? Activity
                                if (isLandscape) {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                } else {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- 2. SCROLLABLE DETAILS & ALGORITHM RECOMMENDATIONS ---
        if (!isLandscape) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
            
            // Video Details Section
            item {
                Column(modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)) {
                    // Title
                    Text(
                        text = viewModel.videoTitle.value, 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // View Count & Date
                    Text(
                        text = "${formatViewCount(viewModel.viewCount.value)} views • ${viewModel.uploadDate.value}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Channel Row
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        // Avatar
                        if (viewModel.uploaderAvatarUrl.value != null) {
                            AsyncImage(
                                model = viewModel.uploaderAvatarUrl.value,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Gray)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = viewModel.channelName.value.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.channelName.value, 
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${viewModel.subCount.value} subscribers", 
                                fontSize = 10.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Action Buttons Row (Download & Background Play)
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Background Play Button
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp), 
                                color = if (isBackgroundPlayEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.clickable {
                                    isBackgroundPlayEnabled = !isBackgroundPlayEnabled
                                }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (isBackgroundPlayEnabled) Icons.Default.PlayArrow else Icons.Default.Headphones, contentDescription = "Background Play", modifier = Modifier.size(14.dp), tint = if (isBackgroundPlayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isBackgroundPlayEnabled) "Background: ON" else "Background: OFF", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = if (isBackgroundPlayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                        
                        // Download Button
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp), 
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.clickable {
                                    downloaderViewModel.extractDownloadLinks(url)
                                }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Download", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
            
            // Related Videos
            items(viewModel.relatedVideos) { item ->
                YoutubeVideoItem(item = item, onClick = { onRelatedVideoClick(item.url) })
            }
        }
        } // Close if (!isLandscape)
        
        // Download Bottom Sheet
        if (downloaderViewModel.showDownloadSheet.value) {
            ModalBottomSheet(
                onDismissRequest = { downloaderViewModel.showDownloadSheet.value = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 80.dp)
                ) {
                    Text(
                        text = "Download Options",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (downloaderViewModel.isExtracting.value) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Extracting media links via NewPipe Engine...")
                        }
                    } else if (downloaderViewModel.errorMessage.value != null) {
                        Text(
                            text = downloaderViewModel.errorMessage.value ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (downloaderViewModel.availableDownloads.isEmpty()) {
                        Text("No downloadable media found.")
                    } else {
                        LazyColumn {
                            items(downloaderViewModel.availableDownloads) { option ->
                                ListItem(
                                    headlineContent = { 
                                        Text(option.quality, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground) 
                                    },
                                    supportingContent = { 
                                        Column {
                                            Text(option.title, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("File size calculated on download.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                        }
                                    },
                                    leadingContent = { 
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                                    },
                                    modifier = Modifier.clickable {
                                        downloaderViewModel.startDownload(context, option)
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}
