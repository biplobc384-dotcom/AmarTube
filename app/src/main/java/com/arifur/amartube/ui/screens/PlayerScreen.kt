package com.arifur.amartube.ui.screens

import androidx.compose.foundation.background
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

import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    url: String, 
    viewModel: PlayerViewModel = viewModel(), 
    downloaderViewModel: DownloaderViewModel = viewModel(),
    onBack: () -> Unit,
    onRelatedVideoClick: (String) -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(url) {
        viewModel.loadVideo(url)
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        
        // --- 1. VIDEO PLAYER SECTION (Fixed at Top) ---
        if (viewModel.isLoading.value) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (viewModel.error.value != null) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${viewModel.error.value}", color = Color.Red, modifier = Modifier.padding(16.dp))
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

                val currentController = mediaController
                if (currentController != null) {
                    AndroidView(
                        factory = { context ->
                            val view = PlayerView(context).apply {
                                player = currentController
                                setShowNextButton(false)
                                setShowPreviousButton(false)
                                setShowRewindButton(true)
                                setShowFastForwardButton(true)
                                setShowSubtitleButton(true)
                                controllerShowTimeoutMs = 3000
                                controllerHideOnTouch = true
                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            }
                            view
                        },
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black)
                    )
                }
            }
        }

        // --- 2. SCROLLABLE DETAILS & ALGORITHM RECOMMENDATIONS ---
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            
            // Video Details Section
            item {
                Column(modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp)) {
                    // Title
                    Text(
                        text = viewModel.videoTitle.value, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // View Count & Date
                    Text(
                        text = "${formatViewCount(viewModel.viewCount.value)} views • ${viewModel.uploadDate.value}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Channel Row
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        // Avatar
                        if (viewModel.uploaderAvatarUrl.value != null) {
                            AsyncImage(
                                model = viewModel.uploaderAvatarUrl.value,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Gray)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = viewModel.channelName.value.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.channelName.value, 
                                style = MaterialTheme.typography.bodyMedium, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${viewModel.subCount.value} subscribers", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action Buttons Row (Download Only)
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Download Button
                        item {
                            Surface(
                                shape = RoundedCornerShape(24.dp), 
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.clickable {
                                    downloaderViewModel.extractDownloadLinks(url)
                                }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Download", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
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
