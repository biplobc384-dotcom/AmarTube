package com.arifur.amartube.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arifur.amartube.engines.downloader.MediaExtractorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLDecoder

data class DownloadOption(
    val title: String,
    val quality: String,
    val downloadUrl: String,
    val isAudioOnly: Boolean = false,
    val fileSize: Long? = null,
    val formatBadge: String = "MP4",
    val extension: String = "mp4",
    val mimeType: String? = null
) {
    fun getFormattedSize(): String? {
        val bytes = fileSize ?: return null
        if (bytes <= 0) return null
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }
}

class DownloaderViewModel : ViewModel() {
    val availableDownloads = mutableStateListOf<DownloadOption>()
    val isExtracting = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val showDownloadSheet = mutableStateOf(false)

    private val extractorEngine = MediaExtractorEngine.getInstance()

    fun extractDownloadLinks(url: String, directVideoUrls: List<String> = emptyList()) {
        if (url.isBlank()) return

        isExtracting.value = true
        errorMessage.value = null
        availableDownloads.clear()
        showDownloadSheet.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = extractorEngine.extractMedia(url, directVideoUrls)
                if (results.isNotEmpty()) {
                    results.forEach { option ->
                        availableDownloads.add(
                            DownloadOption(
                                title = option.title,
                                quality = option.quality,
                                downloadUrl = option.downloadUrl,
                                isAudioOnly = option.isAudioOnly,
                                fileSize = option.fileSize,
                                formatBadge = option.formatBadge,
                                extension = option.extension,
                                mimeType = option.mimeType
                            )
                        )
                    }
                } else {
                    errorMessage.value = "কোনো ডাউনলোড করার মতো মিডিয়া পাওয়া যায়নি।"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage.value = "মিডিয়া অপশন এক্সট্র্যাক্ট করতে সমস্যা হয়েছে: ${e.localizedMessage}"
            } finally {
                isExtracting.value = false
            }
        }
    }

    fun handleDirectBrowserDownload(
        context: Context,
        downloadUrl: String,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isExtracting.value = true
            errorMessage.value = null
            availableDownloads.clear()
            showDownloadSheet.value = true

            try {
                val option = extractorEngine.checkDirectFileDownload(downloadUrl)
                if (option != null) {
                    availableDownloads.add(
                        DownloadOption(
                            title = option.title,
                            quality = option.quality,
                            downloadUrl = option.downloadUrl,
                            isAudioOnly = option.isAudioOnly,
                            fileSize = option.fileSize ?: if (contentLength > 0) contentLength else null,
                            formatBadge = option.formatBadge,
                            extension = option.extension,
                            mimeType = option.mimeType ?: mimeType
                        )
                    )
                } else {
                    var filename = "File_Download"
                    val cd = contentDisposition
                    if (!cd.isNullOrEmpty() && cd.contains("filename=")) {
                        val match = "filename=\"?([^\";]+)\"?".toRegex().find(cd)
                        if (match != null) {
                            filename = match.groupValues[1]
                        }
                    } else {
                        val lastSegment = downloadUrl.split("/").lastOrNull()?.split("?")?.firstOrNull()
                        if (!lastSegment.isNullOrBlank()) {
                            try {
                                filename = URLDecoder.decode(lastSegment, "UTF-8")
                            } catch (_: Exception) {
                                filename = lastSegment
                            }
                        }
                    }

                    val ext = when {
                        mimeType?.contains("video") == true -> "mp4"
                        mimeType?.contains("audio") == true -> "mp3"
                        mimeType?.contains("pdf") == true -> "pdf"
                        mimeType?.contains("zip") == true -> "zip"
                        mimeType?.contains("apk") == true -> "apk"
                        filename.contains(".") -> filename.split(".").last()
                        else -> "file"
                    }

                    availableDownloads.add(
                        DownloadOption(
                            title = filename,
                            quality = "Direct File (${ext.uppercase()})",
                            downloadUrl = downloadUrl,
                            isAudioOnly = mimeType?.contains("audio") == true,
                            fileSize = if (contentLength > 0) contentLength else null,
                            formatBadge = ext.uppercase(),
                            extension = ext,
                            mimeType = mimeType
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isExtracting.value = false
            }
        }
    }

    fun startDownload(context: Context, option: DownloadOption) {
        try {
            val extension = option.extension.ifBlank {
                if (option.isAudioOnly) "m4a" else "mp4"
            }
            val safeTitle = option.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_").take(30)
            val fileName = if (option.title.endsWith(".$extension", ignoreCase = true)) {
                option.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            } else {
                "AmarTube_${safeTitle}_${System.currentTimeMillis()}.$extension"
            }

            val intent = android.content.Intent(context, com.arifur.amartube.services.CustomDownloadService::class.java).apply {
                action = "START"
                putExtra("URL", option.downloadUrl)
                putExtra("TITLE", option.title)
                putExtra("FILE_NAME", fileName)
                putExtra("MIME_TYPE", option.mimeType)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            showDownloadSheet.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun String?.isNull_or_empty_custom(): Boolean = this.isNullOrEmpty()
}
