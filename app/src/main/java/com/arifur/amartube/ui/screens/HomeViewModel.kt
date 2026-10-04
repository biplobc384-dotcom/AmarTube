package com.arifur.amartube.ui.screens

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class HomeViewModel : ViewModel() {
    val trendingVideos = mutableStateListOf<StreamInfoItem>()
    val isLoading = mutableStateOf(false)
    val isLoadingMore = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    
    val isNetworkError = mutableStateOf(false)
    
    private var nextPage: Page? = null
    
    init {
        fetchTrending()
    }

    fun fetchTrending(forceRefresh: Boolean = false) {
        if (trendingVideos.isNotEmpty() && !forceRefresh) return
        
        isLoading.value = true
        errorMessage.value = null
        isNetworkError.value = false
        if (forceRefresh) trendingVideos.clear()
        nextPage = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try fetching default Kiosk (Trending) for YouTube
                val defaultKiosk = ServiceList.YouTube.kioskList.defaultKioskId
                val kioskInfo = KioskInfo.getInfo(ServiceList.YouTube, defaultKiosk)
                
                nextPage = kioskInfo.nextPage
                val items = kioskInfo.relatedItems.filterIsInstance<StreamInfoItem>()
                
                if (items.isNotEmpty()) {
                    trendingVideos.addAll(items)
                } else {
                    fallbackSearch()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (e is java.net.UnknownHostException || e is java.net.ConnectException || e is java.net.SocketTimeoutException) {
                    isNetworkError.value = true
                    errorMessage.value = "No network"
                } else {
                    fallbackSearch()
                }
            } finally {
                isLoading.value = false
            }
        }
    }
    
    private suspend fun fallbackSearch() {
        try {
            val queryHandler = ServiceList.YouTube.searchQHFactory.fromQuery("Trending 2026")
            val searchInfo = SearchInfo.getInfo(ServiceList.YouTube, queryHandler)
            nextPage = searchInfo.nextPage
            trendingVideos.addAll(searchInfo.relatedItems.filterIsInstance<StreamInfoItem>())
        } catch (e: Exception) {
            if (e is java.net.UnknownHostException || e is java.net.ConnectException || e is java.net.SocketTimeoutException || e.message?.contains("Unable to resolve host") == true) {
                isNetworkError.value = true
                errorMessage.value = "No network"
            } else {
                errorMessage.value = e.message ?: "Failed to load Home Feed"
            }
        }
    }
    
    fun loadMore() {
        if (isLoadingMore.value || isLoading.value || nextPage == null) return
        
        isLoadingMore.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // KioskInfo does not have a direct getMoreItems like SearchInfo, we use KioskInfo.getMoreItems
                val kioskInfoPage = KioskInfo.getMoreItems(ServiceList.YouTube, ServiceList.YouTube.kioskList.defaultKioskId, nextPage)
                nextPage = kioskInfoPage.nextPage
                val items = kioskInfoPage.items.filterIsInstance<StreamInfoItem>()
                trendingVideos.addAll(items)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore.value = false
            }
        }
    }
}
