package com.arifur.amartube.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val version: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkUrl: String,
    val isUpdateAvailable: Boolean
)

class UpdateManager private constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun checkForUpdate(currentVersionName: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/biplobc384-dotcom/AmarTube/releases/latest"
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AmarTube-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val jsonStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(jsonStr)

                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val releaseTitle = json.optString("name", "New Update")
                val releaseNotes = json.optString("body", "Bug fixes and performance improvements.")

                var apkDownloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val downloadUrl = asset.optString("browser_download_url", "")
                        val name = asset.optString("name", "").lowercase()
                        if (name.endsWith(".apk") || downloadUrl.endsWith(".apk")) {
                            apkDownloadUrl = downloadUrl
                            break
                        }
                    }
                }

                if (apkDownloadUrl.isBlank() && json.has("html_url")) {
                    apkDownloadUrl = json.optString("html_url")
                }

                val isNewer = isVersionNewer(tagName, currentVersionName)

                return@withContext AppUpdateInfo(
                    version = if (tagName.isNotBlank()) tagName else "1.0",
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    apkUrl = apkDownloadUrl,
                    isUpdateAvailable = isNewer
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false

                val body = response.body ?: return@withContext false
                val totalBytes = body.contentLength()
                val inputStream: InputStream = body.byteStream()

                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val apkFile = File(dir, "AmarTube_Update.apk")
                if (apkFile.exists()) apkFile.delete()

                val outputStream = FileOutputStream(apkFile)
                val buffer = ByteArray(16 * 1024)
                var bytesCopied: Long = 0
                var bytesRead: Int
                var lastProgressTime = 0L

                while (inputStream.read(buffer).also { bytesRead = it } >= 0) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastProgressTime > 300) {
                        lastProgressTime = now
                        val percent = if (totalBytes > 0) (bytesCopied * 100 / totalBytes).toInt() else -1
                        withContext(Dispatchers.Main) {
                            onProgress(percent)
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    onProgress(100)
                    installApk(context, apkFile)
                }
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isVersionNewer(latestVersion: String, currentVersion: String): Boolean {
        if (latestVersion.isBlank() || currentVersion.isBlank()) return false
        try {
            val latestParts = latestVersion.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = currentVersion.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (_: Exception) {}
        return false
    }

    companion object {
        @Volatile
        private var instance: UpdateManager? = null

        fun getInstance(): UpdateManager {
            return instance ?: synchronized(this) {
                instance ?: UpdateManager().also { instance = it }
            }
        }
    }
}
