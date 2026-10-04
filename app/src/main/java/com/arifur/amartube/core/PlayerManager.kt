package com.arifur.amartube.core

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.arifur.amartube.services.MediaPlaybackService

object PlayerManager {
    var controllerFuture: ListenableFuture<MediaController>? = null

    fun initialize(context: Context) {
        if (controllerFuture == null) {
            val sessionToken = SessionToken(context, ComponentName(context, MediaPlaybackService::class.java))
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        }
    }

    fun getPlayer(): MediaController? {
        return if (controllerFuture?.isDone == true) controllerFuture?.get() else null
    }

    fun release() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
    }
}
