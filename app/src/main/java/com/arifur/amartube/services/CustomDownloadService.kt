package com.arifur.amartube.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class CustomDownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloads = ConcurrentHashMap<String, Job>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val url = intent?.getStringExtra("URL")
        val title = intent?.getStringExtra("TITLE") ?: "Media"
        val fileName = intent?.getStringExtra("FILE_NAME") ?: "AmarTube_Download.mp4"
        val downloadId = url ?: return START_NOT_STICKY

        when (action) {
            "START" -> startDownload(downloadId, title, fileName)
            "CANCEL" -> cancelDownload(downloadId)
        }

        return START_STICKY
    }

    private fun startDownload(url: String, title: String, fileName: String) {
        val notificationId = url.hashCode()
        startForeground(notificationId, createNotification(title, 0, "Starting download...", url))

        val job = serviceScope.launch {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .build()

                val response: Response = client.newCall(request).execute()

                if (!response.isSuccessful) throw Exception("Server returned code ${response.code}")

                val body = response.body ?: throw Exception("Empty response body")
                val totalBytes = body.contentLength()
                val inputStream: InputStream = body.byteStream()

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                val outputFile = File(downloadsDir, fileName)
                val outputStream = FileOutputStream(outputFile)

                val buffer = ByteArray(16 * 1024)
                var bytesCopied: Long = 0
                var bytesRead: Int
                var lastUpdateTime = 0L

                while (inputStream.read(buffer).also { bytesRead = it } >= 0) {
                    if (!isActive) break
                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime > 1000) {
                        lastUpdateTime = currentTime
                        val progress = if (totalBytes > 0) (bytesCopied * 100 / totalBytes).toInt() else 0
                        val downloadedMB = bytesCopied / (1024 * 1024)
                        val totalMB = totalBytes / (1024 * 1024)
                        val sizeText = if (totalMB > 0) "${downloadedMB}MB / ${totalMB}MB ($progress%)" else "${downloadedMB}MB"

                        updateNotification(notificationId, createNotification(title, progress, sizeText, url))
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (isActive) {
                    updateNotification(notificationId, createFinishedNotification(title, fileName))
                } else {
                    outputFile.delete() // Cleanup if cancelled
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateNotification(notificationId, createErrorNotification(title))
            } finally {
                activeDownloads.remove(url)
                if (activeDownloads.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                }
            }
        }
        activeDownloads[url] = job
    }

    private fun cancelDownload(url: String) {
        activeDownloads[url]?.cancel()
        activeDownloads.remove(url)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(url.hashCode())
        if (activeDownloads.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun createNotification(title: String, progress: Int, text: String, url: String): Notification {
        val cancelIntent = Intent(this, CustomDownloadService::class.java).apply {
            action = "CANCEL"
            putExtra("URL", url)
        }
        val cancelPendingIntent = PendingIntent.getService(this, url.hashCode(), cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "AMARTUBE_DOWNLOAD")
            .setContentTitle("Downloading: $title")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun createFinishedNotification(title: String, fileName: String): Notification {
        return NotificationCompat.Builder(this, "AMARTUBE_DOWNLOAD")
            .setContentTitle("Download Complete")
            .setContentText("$title ($fileName)")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
    }

    private fun createErrorNotification(title: String): Notification {
        return NotificationCompat.Builder(this, "AMARTUBE_DOWNLOAD")
            .setContentTitle("Download Failed")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setAutoCancel(true)
            .build()
    }

    private fun updateNotification(notificationId: Int, notification: Notification) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "AMARTUBE_DOWNLOAD",
                "AmarTube Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
