package com.arifur.amartube.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    downloaderViewModel: DownloaderViewModel = viewModel()
) {
    var url by remember { mutableStateOf("https://google.com") }
    var urlInput by remember { mutableStateOf("google.com") }
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 4.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Far Left: Back Button
                        IconButton(
                            onClick = { if (webView?.canGoBack() == true) webView?.goBack() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onBackground)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Center: Wide Pill Address Bar
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(Color(0x14FFFFFF), RoundedCornerShape(9999.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Lock Icon
                            Icon(Icons.Default.Lock, contentDescription = "Secure", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // URL Text Field
                            BasicTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        var finalUrl = urlInput.trim()
                                        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                                            finalUrl = if (finalUrl.contains(".") && !finalUrl.contains(" ")) {
                                                "https://$finalUrl"
                                            } else {
                                                "https://www.google.com/search?q=${java.net.URLEncoder.encode(finalUrl, "UTF-8")}"
                                            }
                                        }
                                        url = finalUrl
                                        webView?.loadUrl(url)
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    if (urlInput.isEmpty()) {
                                        Text("Search or type URL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            )

                            // Refresh Icon inside the pill
                            IconButton(
                                onClick = { webView?.reload() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Far Right: Three-dot Menu
                        IconButton(
                            onClick = { /* Menu Action */ },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val currentUrl = webView?.url
                    if (currentUrl != null) {
                        webView?.evaluateJavascript(
                            "(function() { return Array.from(document.querySelectorAll('video')).map(v => v.src || (v.querySelector('source') ? v.querySelector('source').src : '')).filter(Boolean).join(','); })();"
                        ) { result ->
                            val urls = result?.trim('"')?.split(",")?.filter { it.isNotBlank() && it != "null" } ?: emptyList()
                            downloaderViewModel.extractDownloadLinks(currentUrl, urls)
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Download") },
                text = { Text("Download", fontWeight = FontWeight.Bold) },
                modifier = Modifier.padding(bottom = 90.dp) // Lift above the bottom nav
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadsImagesAutomatically = true
                        // Use default user agent but remove WebView specific tags to prevent bot detection
                        val defaultAgent = android.webkit.WebSettings.getDefaultUserAgent(ctx)
                        settings.userAgentString = defaultAgent.replace("; wv", "").replace("Version/4.0 ", "")
                        
                        val webViewInstance = this
                        android.webkit.CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(webViewInstance, true)
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return false // Load all links inside this WebView
                            }
                            
                            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                                super.onPageFinished(view, loadedUrl)
                                isLoading = false
                                loadedUrl?.let { 
                                    // Format clean URL for address bar
                                    urlInput = it.removePrefix("https://").removePrefix("http://").removeSuffix("/")
                                }
                            }
                        }
                        
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                isLoading = newProgress < 100
                            }
                        }
                        
                        loadUrl(url)
                        webView = this
                    }
                },
                update = { view ->
                    webView = view
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom Sheet for Downloads
        if (downloaderViewModel.showDownloadSheet.value) {
            ModalBottomSheet(
                onDismissRequest = { downloaderViewModel.showDownloadSheet.value = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 80.dp)
                ) {
                    Text(
                        text = "Download Options",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (downloaderViewModel.isExtracting.value) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Extracting media links via Multi-Layer Engine...")
                        }
                    } else if (downloaderViewModel.errorMessage.value != null) {
                        Text(
                            text = downloaderViewModel.errorMessage.value ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (downloaderViewModel.availableDownloads.isEmpty()) {
                        Text("No downloadable media found on this page.")
                    } else {
                        LazyColumn {
                            items(downloaderViewModel.availableDownloads) { option ->
                                ListItem(
                                    headlineContent = { 
                                        Text(option.quality, fontWeight = FontWeight.SemiBold) 
                                    },
                                    supportingContent = { 
                                        Text(option.title, maxLines = 1) 
                                    },
                                    leadingContent = { 
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) 
                                    },
                                    modifier = Modifier.clickable {
                                        downloaderViewModel.startDownload(context, option)
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
