package com.arifur.amartube.ui.screens

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class PlayerViewModel : ViewModel() {
    val streamUrl = mutableStateOf<String?>(null)
    val isLoading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)
    
    // Metadata
    val videoTitle = mutableStateOf("")
    val channelName = mutableStateOf("")
    val subCount = mutableStateOf("")
    val viewCount = mutableStateOf<Long>(0)
    val uploadDate = mutableStateOf("")
    val likeCount = mutableStateOf<Long>(0)
    val uploaderAvatarUrl = mutableStateOf<String?>(null)
    val thumbnailUrl = mutableStateOf<String?>(null)
    
    // YouTube Algorithm / Recommendations
    val relatedVideos = mutableStateListOf<StreamInfoItem>()

    fun loadVideo(url: String) {
        isLoading.value = true
        error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Automatically detect service (YouTube vs SoundCloud)
                val service = org.schabi.newpipe.extractor.NewPipe.getServiceByUrl(url)
                val streamInfo = StreamInfo.getInfo(service, url)
                
                // Populate Metadata
                videoTitle.value = streamInfo.name
                channelName.value = streamInfo.uploaderName
                subCount.value = (streamInfo.uploaderSubscriberCount.takeIf { it > 0 }?.let { formatViewCount(it) } ?: "") + " subscribers"
                viewCount.value = streamInfo.viewCount
                uploadDate.value = streamInfo.textualUploadDate ?: ""
                likeCount.value = streamInfo.likeCount
                uploaderAvatarUrl.value = streamInfo.uploaderAvatars.firstOrNull()?.url
                thumbnailUrl.value = streamInfo.thumbnails.firstOrNull()?.url
                
                // Populate YouTube Algorithm Recommendations
                relatedVideos.clear()
                val relatedItems = streamInfo.relatedItems
                if (relatedItems != null) {
                    relatedVideos.addAll(relatedItems.filterIsInstance<StreamInfoItem>())
                }
                
                // Fetch the best video stream, or fallback to pure audio stream
                val bestStream = streamInfo.videoStreams?.firstOrNull() ?: streamInfo.audioStreams?.firstOrNull()
                
                if (bestStream != null) {
                    streamUrl.value = bestStream.content
                } else {
                    error.value = "No playable stream found."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                error.value = e.message ?: "Failed to extract video."
            } finally {
                isLoading.value = false
            }
        }
    }
}
