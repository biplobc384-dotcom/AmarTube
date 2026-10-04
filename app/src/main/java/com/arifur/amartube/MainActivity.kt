package com.arifur.amartube

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.arifur.amartube.core.AppUpdateInfo
import com.arifur.amartube.core.UpdateManager
import com.arifur.amartube.ui.navigation.AppNavigation
import com.arifur.amartube.ui.theme.AmarTubeTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide System Navigation Bar (Transient Immersive Mode)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())

        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            val permissions = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            if (permissions.isNotEmpty()) {
                val permissionState = rememberMultiplePermissionsState(permissions)
                LaunchedEffect(Unit) {
                    permissionState.launchMultiplePermissionRequest()
                }
            }

            var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
            var isDownloading by remember { mutableStateOf(false) }
            var downloadProgress by remember { mutableStateOf(-1) }

            LaunchedEffect(Unit) {
                val currentVersionName = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
                } catch (e: Exception) {
                    "1.0"
                }
                updateInfo = UpdateManager.getInstance().checkForUpdate(context, currentVersionName)
            }

            val info = updateInfo
            if (info != null && info.isUpdateAvailable) {
                AlertDialog(
                    onDismissRequest = {
                        if (!isDownloading) updateInfo = null
                    },
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text(
                            text = "🎉 নতুন আপডেট এসেছে! (v${info.version})",
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
                                text = info.releaseTitle,
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
                                    text = info.releaseNotes,
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
                            enabled = !isDownloading && info.apkUrl.isNotBlank(),
                            onClick = {
                                isDownloading = true
                                coroutineScope.launch {
                                    val success = UpdateManager.getInstance().downloadAndInstallApk(
                                        context = context,
                                        apkUrl = info.apkUrl,
                                        onProgress = { percent ->
                                            downloadProgress = percent
                                        }
                                    )
                                    isDownloading = false
                                    if (success) {
                                        updateInfo = null
                                    }
                                }
                            }
                        ) {
                            Text(if (isDownloading) "ডাউনলোড হচ্ছে..." else "এখন আপডেট করুন")
                        }
                    },
                    dismissButton = {
                        if (!isDownloading) {
                            TextButton(onClick = { updateInfo = null }) {
                                Text("পরে")
                            }
                        }
                    }
                )
            }

            AmarTubeTheme {
                AppNavigation()
            }
        }
    }
}
