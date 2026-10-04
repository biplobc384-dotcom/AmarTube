package com.arifur.amartube.ui.screens

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class TubeViewModel : ViewModel() {
    val searchQuery = mutableStateOf("")
    val searchResults = mutableStateListOf<StreamInfoItem>()
    val isLoading = mutableStateOf(false)
    val isLoadingMore = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    
    private var currentQuery: String = ""
    private var nextPage: Page? = null

    fun searchVideos(query: String, isUserQuery: Boolean = true) {
        if (query.isBlank()) return
        currentQuery = query
        if (isUserQuery) {
            searchQuery.value = query
        } else {
            searchQuery.value = ""
        }
        isLoading.value = true
        errorMessage.value = null
        searchResults.clear()
        nextPage = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val queryHandler = ServiceList.YouTube.searchQHFactory.fromQuery(query)
                val searchInfo = SearchInfo.getInfo(ServiceList.YouTube, queryHandler)
                
                nextPage = searchInfo.nextPage
                val items = searchInfo.relatedItems.filterIsInstance<StreamInfoItem>()
                
                searchResults.addAll(items)
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage.value = e.message ?: "An error occurred fetching data."
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun loadMore() {
        if (isLoadingMore.value || isLoading.value || nextPage == null) return
        
        isLoadingMore.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val queryHandler = ServiceList.YouTube.searchQHFactory.fromQuery(currentQuery)
                val pageResult = SearchInfo.getMoreItems(ServiceList.YouTube, queryHandler, nextPage)
                
                nextPage = pageResult.nextPage
                val items = pageResult.items.filterIsInstance<StreamInfoItem>()
                
                searchResults.addAll(items)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore.value = false
            }
        }
    }
}
