package com.arifur.amartube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(url: String, viewModel: PlayerViewModel = viewModel(), onBack: () -> Unit) {
    LaunchedEffect(url) {
        viewModel.loadVideo(url)
    }

    var isPlaying by remember { mutableStateOf(true) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    
    // Background UI (Dark theme for music)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White)
            }
            Text("Playing from AmarTube", color = Color.White, fontSize = 14.sp)
            IconButton(onClick = { /* Menu */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
            }
        }

        if (viewModel.isLoading.value) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (viewModel.error.value != null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${viewModel.error.value}", color = Color.Red)
            }
        } else {
            val playUrl = viewModel.streamUrl.value
            if (playUrl != null) {
                val mediaController = com.arifur.amartube.core.rememberMediaController()
                
                LaunchedEffect(playUrl, mediaController) {
                    if (mediaController != null) {
                        val mediaItem = MediaItem.fromUri(playUrl)
                        mediaController.setMediaItem(mediaItem)
                        mediaController.prepare()
                        mediaController.playWhenReady = true
                    }
                }
                
                if (mediaController != null) {
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                player = mediaController
                                useController = false
                            }
                        },
                        modifier = Modifier.size(0.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // Large Square Album Art
                AsyncImage(
                    model = viewModel.thumbnailUrl.value,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Title and Artist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.videoTitle.value,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.channelName.value,
                            color = Color.LightGray,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { /* Like */ }) {
                        Icon(Icons.Default.ThumbUp, contentDescription = "Like", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dummy Progress Bar
                var progress by remember { mutableStateOf(0f) }
                LaunchedEffect(isPlaying) {
                    while(isPlaying) {
                        delay(1000)
                        if (progress < 1f) progress += 0.01f
                    }
                }
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Shuffle */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Shuffle", tint = Color.Gray)
                    }
                    IconButton(onClick = { /* Prev */ }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    // Play/Pause Button
                    val mediaController = com.arifur.amartube.core.rememberMediaController()
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                isPlaying = !isPlaying
                                if (isPlaying) mediaController?.play() else mediaController?.pause()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.width(6.dp).height(20.dp).background(Color.Black))
                                Box(modifier = Modifier.width(6.dp).height(20.dp).background(Color.Black))
                            }
                        } else {
                            Icon(
                                Icons.Default.PlayArrow, 
                                contentDescription = "Play/Pause", 
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    IconButton(onClick = { /* Next */ }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = { /* Repeat */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Repeat", tint = Color.Gray)
                    }
                }
            }
        }
    }
}
