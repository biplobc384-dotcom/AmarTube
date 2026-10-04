package com.arifur.amartube.core

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

@Composable
fun rememberMediaController(): MediaController? {
    val controller = remember { mutableStateOf<MediaController?>(null) }
    
    DisposableEffect(Unit) {
        val future = PlayerManager.controllerFuture
        if (future?.isDone == true) {
            controller.value = future.get()
        } else {
            future?.addListener(
                { controller.value = future.get() },
                Executors.newSingleThreadExecutor()
            )
        }
        onDispose { }
    }
    return controller.value
}
