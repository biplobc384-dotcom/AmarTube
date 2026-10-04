package com.arifur.amartube.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arifur.amartube.R
import com.arifur.amartube.core.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showQualityDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(-1) }

    // Dialog for when NO updates are found (User is on latest version)
    if (viewModel.showNoUpdateDialog.value) {
        AlertDialog(
            onDismissRequest = { viewModel.showNoUpdateDialog.value = false },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "🎉 আপনি আপডেট আছেন!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "আপনি অ্যাপের সর্বশেষ ভার্সনে আছেন (v${viewModel.currentVersion})। নতুন কোনো আপডেট পাওয়া যায়নি।",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.showNoUpdateDialog.value = false }) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    // Dialog for when an UPDATE is available
    val updateInfo = viewModel.appUpdateInfo.value
    if (updateInfo != null && updateInfo.isUpdateAvailable) {
        AlertDialog(
            onDismissRequest = {
                if (!isDownloading) viewModel.appUpdateInfo.value = null
            },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "🎉 নতুন আপডেট এসেছে! (v${updateInfo.version})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = updateInfo.releaseTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = updateInfo.releaseNotes,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (downloadProgress >= 0) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "APK ডাউনলোড হচ্ছে... $downloadProgress%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "APK সংযোগ ও ডাউনলোড হচ্ছে...",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDownloading && updateInfo.apkUrl.isNotBlank(),
                    onClick = {
                        isDownloading = true
                        coroutineScope.launch {
                            val success = UpdateManager.getInstance().downloadAndInstallApk(
                                context = context,
                                apkUrl = updateInfo.apkUrl,
                                onProgress = { percent ->
                                    downloadProgress = percent
                                }
                            )
                            isDownloading = false
                            if (success) {
                                viewModel.appUpdateInfo.value = null
                            }
                        }
                    }
                ) {
                    Text(if (isDownloading) "ডাউনলোড হচ্ছে..." else "এখন আপডেট করুন")
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { viewModel.appUpdateInfo.value = null }) {
                        Text("পরে")
                    }
                }
            }
        )
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Download Quality") },
            text = {
                Column {
                    val options = listOf("Ask each time", "1080p", "720p", "480p", "360p", "Audio only")
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.downloadQuality.value = option
                                    showQualityDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.downloadQuality.value == option,
                                onClick = {
                                    viewModel.downloadQuality.value = option
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("Storage Location") },
            text = {
                Column {
                    val options = listOf("Internal Storage", "SD Card")
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.storageLocation.value = option
                                    showStorageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.storageLocation.value == option,
                                onClick = {
                                    viewModel.storageLocation.value = option
                                    showStorageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(bottom = 120.dp) // Space for bottom nav
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Account Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.user_avatar),
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A2A)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("User Mayaboti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("mayaboti@example.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSectionTitle("General")
                SettingsCard {
                    SettingsToggleItem(icon = Icons.Default.Settings, title = "Dark Theme", subtitle = "Reduce glare and improve night viewing", isChecked = true)
                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(start = 56.dp))
                    SettingsToggleItem(icon = Icons.Default.PlayArrow, title = "Background Play", subtitle = "Keep playing audio when app is minimized", isChecked = true)
                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(start = 56.dp))
                    SettingsItem(icon = Icons.Default.CheckCircle, title = "Language", subtitle = "English")
                }

                SettingsSectionTitle("Downloads")
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.KeyboardArrowDown,
                        title = "Download Quality",
                        subtitle = viewModel.downloadQuality.value,
                        onClick = { showQualityDialog = true }
                    )
                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(start = 56.dp))
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Storage Location",
                        subtitle = viewModel.storageLocation.value,
                        onClick = { showStorageDialog = true }
                    )
                }

                SettingsSectionTitle("Open Source Engines")
                SettingsCard {
                    SettingsLinkItem(
                        icon = Icons.Default.Code,
                        title = "NewPipeExtractor",
                        subtitle = "Extracts data from YouTube",
                        url = "https://github.com/TeamNewPipe/NewPipeExtractor",
                        context = context
                    )
                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(start = 56.dp))
                    SettingsLinkItem(
                        icon = Icons.Default.PlayArrow,
                        title = "Jetpack Media3 (ExoPlayer)",
                        subtitle = "Video and audio playback",
                        url = "https://github.com/androidx/media",
                        context = context
                    )
                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(start = 56.dp))
                    SettingsLinkItem(
                        icon = Icons.Default.Image,
                        title = "Coil",
                        subtitle = "Image loading",
                        url = "https://github.com/coil-kt/coil",
                        context = context
                    )
                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(start = 56.dp))
                    SettingsLinkItem(
                        icon = Icons.Default.NetworkCell,
                        title = "OkHttp",
                        subtitle = "Networking",
                        url = "https://github.com/square/okhttp",
                        context = context
                    )
                }

                SettingsSectionTitle("Developer")
                SettingsCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Image(
                            painter = painterResource(id = R.drawable.developer_image),
                            contentDescription = "Developer Profile",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Arifur",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "App Developer & Maintainer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    HorizontalDivider(color = Color(0x14FFFFFF))
                    SettingsLinkItem(
                        icon = Icons.Default.Group,
                        title = "Telegram Group",
                        subtitle = "Join our community",
                        url = "https://t.me/ArifurHackworld",
                        context = context
                    )
                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(start = 56.dp))
                    SettingsLinkItem(
                        icon = Icons.Default.Campaign,
                        title = "Telegram Channel",
                        subtitle = "Get latest updates",
                        url = "https://t.me/ArifurHack",
                        context = context
                    )
                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(start = 56.dp))
                    SettingsLinkItem(
                        icon = Icons.Default.Person,
                        title = "Telegram Direct",
                        subtitle = "@arifur905",
                        url = "https://t.me/arifur905",
                        context = context
                    )
                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(start = 56.dp))
                    SettingsLinkItem(
                        icon = Icons.Default.ThumbUp,
                        title = "Facebook",
                        subtitle = "Connect on Facebook",
                        url = "https://www.facebook.com/fary.pol",
                        context = context
                    )
                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(start = 56.dp))
                    SettingsLinkItem(
                        icon = Icons.Default.Code,
                        title = "GitHub",
                        subtitle = "View my projects",
                        url = "https://github.com/biplobc384-dotcom/AmarTube",
                        context = context
                    )
                }

                SettingsSectionTitle("About & Updates")
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = "v${viewModel.currentVersion}"
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(start = 56.dp))

                    // Check for Updates Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.checkForUpdates(context) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewModel.isCheckingUpdate.value) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Check for Updates", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            val statusText = if (viewModel.isCheckingUpdate.value) "Checking GitHub for updates..." else (viewModel.updateMessage.value ?: "Tap to check for latest GitHub release")
                            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    RoundedCornerShape(16.dp).let { shape ->
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14FFFFFF))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp),
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsLinkItem(icon: ImageVector, title: String, subtitle: String, url: String, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsToggleItem(icon: ImageVector, title: String, subtitle: String, isChecked: Boolean) {
    var checkedState by remember { mutableStateOf(isChecked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { checkedState = !checkedState }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checkedState,
            onCheckedChange = { checkedState = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
