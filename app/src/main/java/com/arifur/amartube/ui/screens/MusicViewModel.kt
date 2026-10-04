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

class MusicViewModel : ViewModel() {
    val searchQuery = mutableStateOf("")
    val searchResults = mutableStateListOf<StreamInfoItem>()
    val isLoading = mutableStateOf(false)
    val isLoadingMore = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    
    val isNetworkError = mutableStateOf(false)
    
    private var currentQuery: String = ""
    private var nextPage: Page? = null
    
    val searchSuggestions = mutableStateListOf<String>()
    private var suggestionJob: kotlinx.coroutines.Job? = null

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
        if (query.isBlank()) {
            searchSuggestions.clear()
            return
        }
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(300) // Debounce
            try {
                val extractor = ServiceList.YouTube.suggestionExtractor
                val suggestions = extractor.suggestionList(query)
                searchSuggestions.clear()
                searchSuggestions.addAll(suggestions)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun clearSuggestions() {
        searchSuggestions.clear()
    }

    fun searchMusic(query: String, isUserQuery: Boolean = true) {
        if (query.isBlank()) return
        currentQuery = query
        if (isUserQuery) {
            searchQuery.value = query
        } else {
            searchQuery.value = ""
        }
        isLoading.value = true
        errorMessage.value = null
        isNetworkError.value = false
        searchResults.clear()
        nextPage = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch using YouTube Service (More reliable)
                val queryHandler = ServiceList.YouTube.searchQHFactory.fromQuery(query)
                val searchInfo = SearchInfo.getInfo(ServiceList.YouTube, queryHandler)
                
                nextPage = searchInfo.nextPage
                val items = searchInfo.relatedItems.filterIsInstance<StreamInfoItem>()
                
                searchResults.addAll(items)
            } catch (e: Exception) {
                e.printStackTrace()
                if (e is java.net.UnknownHostException || e is java.net.ConnectException || e is java.net.SocketTimeoutException || e.message?.contains("Unable to resolve host") == true) {
                    isNetworkError.value = true
                    errorMessage.value = "No network"
                } else {
                    errorMessage.value = e.message ?: "Failed to fetch music."
                }
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