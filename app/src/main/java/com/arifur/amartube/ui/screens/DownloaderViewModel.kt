package com.arifur.amartube.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.concurrent.TimeUnit

data class DownloadOption(
    val title: String,
    val quality: String,
    val downloadUrl: String,
    val isAudioOnly: Boolean = false
)

class DownloaderViewModel : ViewModel() {
    val availableDownloads = mutableStateListOf<DownloadOption>()
    val isExtracting = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val showDownloadSheet = mutableStateOf(false)

    // Using a slightly longer timeout for Cobalt API which can sometimes take a few seconds
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun extractDownloadLinks(url: String, directVideoUrls: List<String> = emptyList()) {
        if (url.isBlank()) return
        
        isExtracting.value = true
        errorMessage.value = null
        availableDownloads.clear()
        showDownloadSheet.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // LAYER 1: Try NewPipe Extractor (YouTube, SoundCloud, MediaCCC, etc.)
                val service = try {
                    NewPipe.getServiceByUrl(url)
                } catch (e: Exception) { null }

                if (service != null) {
                    val streamInfo = StreamInfo.getInfo(service, url)
                    val title = streamInfo.name
                    
                    // Add Video Streams (Muxed)
                    streamInfo.videoStreams?.forEach { stream ->
                        availableDownloads.add(
                            DownloadOption(
                                title = title,
                                quality = "${stream.resolution} (${stream.format.name()}) - Video + Audio",
                                downloadUrl = stream.content
                            )
                        )
                    }
                    
                    // Add Audio Streams
                    streamInfo.audioStreams?.forEach { stream ->
                        availableDownloads.add(
                            DownloadOption(
                                title = title,
                                quality = "Audio ${stream.averageBitrate}kbps (${stream.format.name()})",
                                downloadUrl = stream.content,
                                isAudioOnly = true
                            )
                        )
                    }

                    if (availableDownloads.isNotEmpty()) {
                        isExtracting.value = false
                        return@launch
                    }
                }

                // LAYER 2: Fallback to direct video URLs from WebView JS injection
                if (directVideoUrls.isNotEmpty()) {
                    directVideoUrls.forEachIndexed { index, vUrl ->
                        availableDownloads.add(
                            DownloadOption(
                                title = "Direct Media Link ${index + 1}",
                                quality = "Best Available",
                                downloadUrl = vUrl
                            )
                        )
                    }
                    isExtracting.value = false
                    return@launch
                }
                
                errorMessage.value = "Failed to extract media links from this site."

            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to direct video URLs if NewPipe throws an exception (e.g. Unsupported URL)
                if (directVideoUrls.isNotEmpty()) {
                    directVideoUrls.forEachIndexed { index, vUrl ->
                        availableDownloads.add(
                            DownloadOption(
                                title = "Direct Media Link ${index + 1}",
                                quality = "Best Available",
                                downloadUrl = vUrl
                            )
                        )
                    }
                } else {
                    errorMessage.value = "Error extracting media links."
                }
            } finally {
                isExtracting.value = false
            }
        }
    }

    fun startDownload(context: Context, option: DownloadOption) {
        try {
            val extension = if (option.isAudioOnly) "m4a" else "mp4"
            val safeTitle = option.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_").take(30)
            val fileName = "AmarTube_${safeTitle}_${System.currentTimeMillis()}.$extension"

            val intent = android.content.Intent(context, com.arifur.amartube.services.CustomDownloadService::class.java).apply {
                action = "START"
                putExtra("URL", option.downloadUrl)
                putExtra("TITLE", option.title)
                putExtra("FILE_NAME", fileName)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            showDownloadSheet.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
