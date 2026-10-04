package com.arifur.amartube.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class SettingsViewModel : ViewModel() {
    val currentVersion = "1.0.0"
    val isCheckingUpdate = mutableStateOf(false)
    val updateMessage = mutableStateOf<String?>(null)
    val latestVersion = mutableStateOf<String?>(null)
    val downloadUrl = mutableStateOf<String?>(null)
    
    val downloadQuality = mutableStateOf("Ask each time")
    val storageLocation = mutableStateOf("Internal Storage")

    private val client = OkHttpClient()

    fun checkForUpdates() {
        if (isCheckingUpdate.value) return
        
        isCheckingUpdate.value = true
        updateMessage.value = null
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Replace with your actual GitHub Repo (e.g., username/AmarTube)
                // Using a placeholder repo string, which should be updated before release
                val url = "https://api.github.com/repos/Arifur/AmarTube/releases/latest"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val json = JSONObject(responseBody)
                        val tagName = json.getString("tag_name").replace("v", "")
                        
                        // Basic semantic version check
                        if (tagName > currentVersion) {
                            latestVersion.value = tagName
                            updateMessage.value = "New update available: v$tagName!"
                            
                            val assets = json.getJSONArray("assets")
                            if (assets.length() > 0) {
                                downloadUrl.value = assets.getJSONObject(0).getString("browser_download_url")
                            } else {
                                updateMessage.value = "New update found, but no APK attached."
                            }
                        } else {
                            updateMessage.value = "You are on the latest version."
                        }
                    }
                } else {
                    if (response.code == 404) {
                        updateMessage.value = "No releases found on GitHub yet."
                    } else {
                        updateMessage.value = "Update check failed (Code: ${response.code})"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateMessage.value = "Error checking for updates."
            } finally {
                isCheckingUpdate.value = false
            }
        }
    }
}
