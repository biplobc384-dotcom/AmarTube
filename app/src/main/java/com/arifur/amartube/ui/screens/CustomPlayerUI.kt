package com.arifur.amartube.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPlayerUI(
    player: Player,
    isLandscape: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var controlsVisible by remember { mutableStateOf(true) }
    
    var skipText by remember { mutableStateOf("") }
    var skipVisible by remember { mutableStateOf(false) }
    var skipDirection by remember { mutableIntStateOf(1) } // 1 for right, -1 for left
    var playerError by remember { mutableStateOf<androidx.media3.common.PlaybackException?>(null) }

    // Auto-hide controls
    LaunchedEffect(controlsVisible, isPlaying, playerError) {
        if (playerError != null) {
            controlsVisible = true
        } else if (controlsVisible && isPlaying) {
            kotlinx.coroutines.delay(3000)
            controlsVisible = false
        }
    }

    LaunchedEffect(skipVisible) {
        if (skipVisible) {
            kotlinx.coroutines.delay(800)
            skipVisible = false
        }
    }

    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            playerError = player.playerError
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x > width / 2) {
                            player.seekTo(player.currentPosition + 10000)
                            skipText = "+10s"
                            skipDirection = 1
                            skipVisible = true
                        } else {
                            player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                            skipText = "-10s"
                            skipDirection = -1
                            skipVisible = true
                        }
                    }
                )
            }
    ) {
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = Float.POSITIVE_INFINITY // This will be adjusted below
                        )
                    )
            ) {
                // Center Play/Pause or Refresh
                if (playerError != null) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { 
                                player.prepare()
                                player.play()
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Text("Network Error", color = Color.White, fontSize = 12.sp)
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (isPlaying) player.pause() else player.play()
                            controlsVisible = true
                        },
                        modifier = Modifier.align(Alignment.Center).size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                // Bottom Bar
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition) + " / " + formatTime(duration),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        IconButton(onClick = onToggleFullscreen) {
                            Icon(
                                imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                    
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                        onValueChange = {
                            val newPosition = (it * duration).toLong()
                            player.seekTo(newPosition)
                            currentPosition = newPosition
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Red,
                            activeTrackColor = Color.Red,
                            inactiveTrackColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 0.dp) // Seekbar at the very bottom
                    )
                }
            }
        }

        // Skip Animation Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = skipVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(if (skipDirection == 1) Alignment.CenterEnd else Alignment.CenterStart).padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0x88000000), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(text = skipText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
