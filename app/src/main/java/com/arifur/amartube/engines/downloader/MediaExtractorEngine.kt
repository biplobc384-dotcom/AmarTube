package com.arifur.amartube.engines.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractedMediaOption(
    val title: String,
    val quality: String,
    val downloadUrl: String,
    val isAudioOnly: Boolean = false,
    val fileSize: Long? = null,
    val formatBadge: String = "MP4",
    val extension: String = "mp4",
    val mimeType: String? = null
)

class MediaExtractorEngine private constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun extractMedia(url: String, jsMediaUrls: List<String> = emptyList()): List<ExtractedMediaOption> = withContext(Dispatchers.IO) {
        val options = mutableListOf<ExtractedMediaOption>()
        val cleanUrl = url.trim()
        val lowerUrl = cleanUrl.lowercase()

        // 1. Direct File Check (HEAD Request or Extension matching)
        try {
            val directOption = checkDirectFileDownload(cleanUrl)
            if (directOption != null) {
                options.add(directOption)
                return@withContext options
            }
        } catch (_: Exception) {}

        // 2. YouTube & NewPipe Extractor
        if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") || lowerUrl.contains("soundcloud.com")) {
            try {
                val service = NewPipe.getServiceByUrl(cleanUrl)
                if (service != null) {
                    val streamInfo = StreamInfo.getInfo(service, cleanUrl)
                    val title = streamInfo.name ?: "Media Video"

                    streamInfo.videoStreams?.forEach { stream ->
                        val res = stream.resolution ?: "Video"
                        val formatName = stream.format?.name?.uppercase() ?: "MP4"
                        options.add(
                            ExtractedMediaOption(
                                title = title,
                                quality = "$res ($formatName)",
                                downloadUrl = stream.content,
                                isAudioOnly = false,
                                formatBadge = formatName,
                                extension = "mp4"
                            )
                        )
                    }

                    streamInfo.audioStreams?.forEach { stream ->
                        val bitrate = if (stream.averageBitrate > 0) "${stream.averageBitrate}kbps" else "Audio"
                        val formatName = stream.format?.name?.uppercase() ?: "M4A"
                        options.add(
                            ExtractedMediaOption(
                                title = title,
                                quality = "Audio $bitrate ($formatName)",
                                downloadUrl = stream.content,
                                isAudioOnly = true,
                                formatBadge = "MP3",
                                extension = "m4a"
                            )
                        )
                    }

                    if (options.isNotEmpty()) return@withContext options
                }
            } catch (_: Exception) {}

            // Fallback for YouTube: Piped & Invidious APIs
            try {
                val pipedOptions = extractYouTubeViaPipedOrInvidious(cleanUrl)
                if (pipedOptions.isNotEmpty()) return@withContext pipedOptions
            } catch (_: Exception) {}
        }

        // 3. TikTok Extractor (TikWM API & Cobalt API)
        if (lowerUrl.contains("tiktok.com")) {
            try {
                val tikTokOptions = extractTikTokViaTikWm(cleanUrl)
                if (tikTokOptions.isNotEmpty()) return@withContext tikTokOptions
            } catch (_: Exception) {}
        }

        // 4. Cobalt Universal Extractor (Facebook, Instagram, TikTok, YouTube, Twitter/X)
        try {
            val cobaltOptions = extractViaCobalt(cleanUrl)
            if (cobaltOptions.isNotEmpty()) return@withContext cobaltOptions
        } catch (_: Exception) {}

        // 5. HTML Page Scraper / Direct Media Links from JS Injected Browser
        if (jsMediaUrls.isNotEmpty()) {
            jsMediaUrls.forEachIndexed { index, mediaUrl ->
                val ext = getExtensionFromUrl(mediaUrl, "mp4")
                options.add(
                    ExtractedMediaOption(
                        title = "Captured Media Stream ${index + 1}",
                        quality = "Best Available (${ext.uppercase()})",
                        downloadUrl = mediaUrl,
                        isAudioOnly = ext == "mp3" || ext == "m4a" || ext == "aac",
                        formatBadge = ext.uppercase(),
                        extension = ext
                    )
                )
            }
            if (options.isNotEmpty()) return@withContext options
        }

        // 6. Generic HTML Regex Scraper
        try {
            val htmlOptions = scrapeHtmlForMedia(cleanUrl)
            if (htmlOptions.isNotEmpty()) return@withContext htmlOptions
        } catch (_: Exception) {}

        return@withContext options
    }

    suspend fun checkDirectFileDownload(url: String): ExtractedMediaOption? = withContext(Dispatchers.IO) {
        val directExtensions = listOf(".apk", ".zip", ".pdf", ".mp4", ".mp3", ".m4a", ".mkv", ".avi", ".rar", ".7z", ".doc", ".docx", ".xls", ".xlsx")
        val lowerUrl = url.lowercase()
        val extensionFromPath = directExtensions.find { lowerUrl.contains(it) }?.removePrefix(".")

        val request = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            val contentType = response.header("Content-Type")?.lowercase() ?: ""
            val contentDisposition = response.header("Content-Disposition") ?: ""
            val contentLength = response.header("Content-Length")?.toLongOrNull()

            val isNotHtml = contentType.isNotBlank() && !contentType.contains("text/html")
            val isAttachment = contentDisposition.lowercase().contains("attachment")

            if (isNotHtml || isAttachment || extensionFromPath != null) {
                var fileName = "Direct_Download"
                if (contentDisposition.contains("filename=")) {
                    val match = Pattern.compile("filename=\"?([^\";]+)\"?").matcher(contentDisposition)
                    if (match.find()) {
                        fileName = match.group(1) ?: fileName
                    }
                } else {
                    val segments = url.split("/").lastOrNull()?.split("?")?.firstOrNull()
                    if (!segments.isNullOrBlank() && segments.contains(".")) {
                        fileName = segments
                    }
                }

                val ext = extensionFromPath ?: when {
                    contentType.contains("video") -> "mp4"
                    contentType.contains("audio") -> "mp3"
                    contentType.contains("pdf") -> "pdf"
                    contentType.contains("zip") -> "zip"
                    contentType.contains("apk") || contentType.contains("android.package-archive") -> "apk"
                    else -> "file"
                }

                return@withContext ExtractedMediaOption(
                    title = fileName,
                    quality = "Direct File (${ext.uppercase()})",
                    downloadUrl = url,
                    isAudioOnly = contentType.contains("audio"),
                    fileSize = contentLength,
                    formatBadge = ext.uppercase(),
                    extension = ext,
                    mimeType = contentType
                )
            }
        }
        return@withContext null
    }

    private suspend fun extractTikTokViaTikWm(url: String): List<ExtractedMediaOption> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ExtractedMediaOption>()
        val formBody = FormBody.Builder()
            .add("url", url)
            .add("count", "12")
            .add("cursor", "0")
            .add("web", "1")
            .build()

        val request = Request.Builder()
            .url("https://tikwm.com/api/")
            .post(formBody)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G981B) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                val json = JSONObject(jsonStr)
                if (json.optInt("code") == 0 && json.has("data")) {
                    val data = json.getJSONObject("data")
                    val title = data.optString("title", "TikTok Video")
                    val play = data.optString("play")
                    val wmplay = data.optString("wmplay")
                    val music = data.optString("music")
                    val size = data.optLong("size", 0L).takeIf { it > 0 }

                    if (play.isNotBlank()) {
                        val videoUrl = if (play.startsWith("http")) play else "https://tikwm.com$play"
                        list.add(
                            ExtractedMediaOption(
                                title = title,
                                quality = "HD Video (No Watermark)",
                                downloadUrl = videoUrl,
                                isAudioOnly = false,
                                fileSize = size,
                                formatBadge = "FHD",
                                extension = "mp4"
                            )
                        )
                    }

                    if (wmplay.isNotBlank()) {
                        val videoUrl = if (wmplay.startsWith("http")) wmplay else "https://tikwm.com$wmplay"
                        list.add(
                            ExtractedMediaOption(
                                title = title,
                                quality = "Watermark Video",
                                downloadUrl = videoUrl,
                                isAudioOnly = false,
                                formatBadge = "MP4",
                                extension = "mp4"
                            )
                        )
                    }

                    if (music.isNotBlank()) {
                        val audioUrl = if (music.startsWith("http")) music else "https://tikwm.com$music"
                        list.add(
                            ExtractedMediaOption(
                                title = title,
                                quality = "Original Audio (MP3)",
                                downloadUrl = audioUrl,
                                isAudioOnly = true,
                                formatBadge = "MP3",
                                extension = "mp3"
                            )
                        )
                    }
                }
            }
        }
        return@withContext list
    }

    private suspend fun extractYouTubeViaPipedOrInvidious(url: String): List<ExtractedMediaOption> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ExtractedMediaOption>()
        val videoIdMatcher = Pattern.compile("(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/)([^\"&?\\/\\s]{11})").matcher(url)
        val videoId = if (videoIdMatcher.find()) videoIdMatcher.group(1) else null ?: return@withContext list

        val pipedInstances = listOf("https://api.piped.video", "https://pipedapi.kavin.rocks", "https://pipedapi.drgns.space")
        for (instance in pipedInstances) {
            try {
                val req = Request.Builder().url("$instance/streams/$videoId").build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "")
                        val title = json.optString("title", "YouTube Video")

                        val videoStreams = json.optJSONArray("videoStreams")
                        if (videoStreams != null) {
                            for (i in 0 until videoStreams.length()) {
                                val item = videoStreams.getJSONObject(i)
                                val streamUrl = item.optString("url")
                                val quality = item.optString("quality", "720p")
                                val size = item.optLong("filesize", 0L).takeIf { it > 0 }
                                if (streamUrl.isNotBlank()) {
                                    list.add(
                                        ExtractedMediaOption(
                                            title = title,
                                            quality = quality,
                                            downloadUrl = streamUrl,
                                            isAudioOnly = false,
                                            fileSize = size,
                                            formatBadge = "MP4",
                                            extension = "mp4"
                                        )
                                    )
                                }
                            }
                        }

                        val audioStreams = json.optJSONArray("audioStreams")
                        if (audioStreams != null) {
                            for (i in 0 until audioStreams.length()) {
                                val item = audioStreams.getJSONObject(i)
                                val streamUrl = item.optString("url")
                                val bitrate = item.optString("bitrate", "128k")
                                val size = item.optLong("filesize", 0L).takeIf { it > 0 }
                                if (streamUrl.isNotBlank()) {
                                    list.add(
                                        ExtractedMediaOption(
                                            title = title,
                                            quality = "Audio ($bitrate)",
                                            downloadUrl = streamUrl,
                                            isAudioOnly = true,
                                            fileSize = size,
                                            formatBadge = "M4A",
                                            extension = "m4a"
                                        )
                                    )
                                }
                            }
                        }

                        if (list.isNotEmpty()) return@withContext list
                    }
                }
            } catch (_: Exception) {}
        }
        return@withContext list
    }

    private suspend fun extractViaCobalt(url: String): List<ExtractedMediaOption> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ExtractedMediaOption>()
        val cobaltEndpoints = listOf("https://api.cobalt.tools", "https://co.wuk.sh/api/json", "https://cobalt.api.redna2.xyz")

        for (endpoint in cobaltEndpoints) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("url", url)
                    put("videoQuality", "720")
                    put("vQuality", "720")
                    put("isAudioOnly", false)
                }

                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "")
                        val downloadUrl = json.optString("url", "")
                        if (downloadUrl.isNotBlank()) {
                            list.add(
                                ExtractedMediaOption(
                                    title = "Media Video (HD)",
                                    quality = "720p HD",
                                    downloadUrl = downloadUrl,
                                    isAudioOnly = false,
                                    formatBadge = "HD",
                                    extension = "mp4"
                                )
                            )
                            return@withContext list
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return@withContext list
    }

    private suspend fun scrapeHtmlForMedia(url: String): List<ExtractedMediaOption> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ExtractedMediaOption>()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""

                var title = "Web Media"
                val titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(html)
                if (titleMatcher.find()) {
                    title = titleMatcher.group(1)?.replace("&amp;", "&")?.trim() ?: title
                }

                val formatMap = mutableMapOf<String, String>()

                // 1. Script/JSON Quality Matcher (xVideos, xHamster, Pornhub, Spankbang, RedTube, etc.)
                val qualityPattern = Pattern.compile("\"(1080p|720p|480p|360p|240p)\"\\s*:\\s*\"(https?:\\\\?/\\\\?/[^\"]+\\.mp4[^\"]*)\"", Pattern.CASE_INSENSITIVE)
                val qualityMatcher = qualityPattern.matcher(html)
                while (qualityMatcher.find()) {
                    val q = qualityMatcher.group(1)?.lowercase()
                    val vUrl = qualityMatcher.group(2)?.replace("\\/", "/")?.replace("\\u0026", "&")?.replace("&amp;", "&")
                    if (!q.isNullOrBlank() && !vUrl.isNullOrBlank() && vUrl.startsWith("http")) {
                        formatMap[q] = vUrl
                    }
                }

                // 2. Generic mp4 URLs in script or JSON
                if (formatMap.isEmpty()) {
                    val genericMp4Pattern = Pattern.compile("\"(https?:\\\\?/\\\\?/[^\"]+\\.mp4[^\"]*)\"", Pattern.CASE_INSENSITIVE)
                    val genericMatcher = genericMp4Pattern.matcher(html)
                    while (genericMatcher.find()) {
                        val rawUrl = genericMatcher.group(1)?.replace("\\/", "/")?.replace("\\u0026", "&")?.replace("&amp;", "&")
                        if (!rawUrl.isNullOrBlank() && rawUrl.startsWith("http") && !rawUrl.contains("placeholder") && !rawUrl.contains("preview")) {
                            val label = when {
                                rawUrl.contains("1080") -> "1080p"
                                rawUrl.contains("720") -> "720p"
                                rawUrl.contains("480") -> "480p"
                                else -> "720p"
                            }
                            if (!formatMap.containsKey(label)) {
                                formatMap[label] = rawUrl
                            }
                        }
                    }
                }

                // 3. HTML video tag & meta og:video
                if (formatMap.isEmpty()) {
                    val mediaPatterns = listOf(
                        Pattern.compile("<video[^>]+src=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("<source[^>]+src=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("<meta\\s+property=\"og:video\"\\s+content=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("<meta\\s+property=\"og:video:url\"\\s+content=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
                    )

                    for (pattern in mediaPatterns) {
                        val matcher = pattern.matcher(html)
                        while (matcher.find()) {
                            var mediaUrl = matcher.group(1)?.replace("\\/", "/")?.replace("&amp;", "&")
                            if (!mediaUrl.isNullOrBlank() && !mediaUrl.startsWith("blob:")) {
                                var finalMediaUrl = mediaUrl
                                if (finalMediaUrl.startsWith("/")) {
                                    val baseUri = java.net.URI(url)
                                    finalMediaUrl = "${baseUri.scheme}://${baseUri.host}$finalMediaUrl"
                                }
                                formatMap["HD"] = finalMediaUrl
                                break
                            }
                        }
                        if (formatMap.isNotEmpty()) break
                    }
                }

                // Add extracted formats to options
                formatMap.forEach { (qLabel, streamUrl) ->
                    val badge = when {
                        qLabel.contains("1080") -> "FHD"
                        qLabel.contains("720") -> "HD"
                        else -> "MP4"
                    }
                    list.add(
                        ExtractedMediaOption(
                            title = title,
                            quality = "$qLabel Video",
                            downloadUrl = streamUrl,
                            isAudioOnly = false,
                            formatBadge = badge,
                            extension = "mp4"
                        )
                    )
                }
            }
        }
        return@withContext list
    }

    private fun getExtensionFromUrl(url: String, fallback: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".mp4") -> "mp4"
            lower.contains(".m3u8") -> "m3u8"
            lower.contains(".m4a") -> "m4a"
            lower.contains(".mp3") -> "mp3"
            lower.contains(".apk") -> "apk"
            lower.contains(".pdf") -> "pdf"
            lower.contains(".zip") -> "zip"
            else -> fallback
        }
    }

    private fun String?.isNotBlank(): Boolean = !this.isNullOrBlank()

    companion object {
        @Volatile
        private var instance: MediaExtractorEngine? = null

        fun getInstance(): MediaExtractorEngine {
            return instance ?: synchronized(this) {
                instance ?: MediaExtractorEngine().also { instance = it }
            }
        }
    }
}
