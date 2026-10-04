package com.arifur.amartube.core

import android.app.Application
import org.schabi.newpipe.extractor.NewPipe
import com.arifur.amartube.engines.downloader.AmarTubeDownloader

class AmarTubeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize NewPipe Extractor with our custom downloader
        NewPipe.init(AmarTubeDownloader.getInstance())
        
        // Initialize Background Media Player
        PlayerManager.initialize(this)
    }
}
