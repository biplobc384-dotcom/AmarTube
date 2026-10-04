package com.arifur.amartube.ui.screens

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arifur.amartube.core.AppUpdateInfo
import com.arifur.amartube.core.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    var currentVersion = "1.0"
    val isCheckingUpdate = mutableStateOf(false)
    val updateMessage = mutableStateOf<String?>(null)
    val appUpdateInfo = mutableStateOf<AppUpdateInfo?>(null)
    val showNoUpdateDialog = mutableStateOf(false)

    val downloadQuality = mutableStateOf("Ask each time")
    val storageLocation = mutableStateOf("Internal Storage")

    fun checkForUpdates(context: Context) {
        if (isCheckingUpdate.value) return

        isCheckingUpdate.value = true
        updateMessage.value = null
        appUpdateInfo.value = null

        val currentVersionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
        currentVersion = currentVersionName

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = UpdateManager.getInstance().checkForUpdate(
                    context = context,
                    currentVersionName = currentVersionName,
                    forceCheck = true
                )

                if (info != null && info.isUpdateAvailable) {
                    appUpdateInfo.value = info
                    updateMessage.value = "নতুন আপডেট পাওয়া গেছে: v${info.version}"
                } else {
                    showNoUpdateDialog.value = true
                    updateMessage.value = "আপনি সর্বশেষ ভার্সনে আছেন (v$currentVersionName)"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateMessage.value = "আপডেট চেক করতে সমস্যা হয়েছে"
            } finally {
                isCheckingUpdate.value = false
            }
        }
    }
}
