package com.example.privacypolicysummarizer

import android.content.BroadcastReceiver
import androidx.compose.ui.Alignment
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.privacypolicysummarizer.network.PrivacyPolicyFetcher
import com.example.privacypolicysummarizer.ui.theme.PrivacyPolicySummarizerTheme
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.launch
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import android.app.AppOpsManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager


class MainActivity : ComponentActivity() {

    private lateinit var appChangeReceiver: AppChangeReceiver
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageNameToNavigate = intent?.getStringExtra("navigate_to_package")

        // Request Usage Access permission if not granted
        if (!hasUsageStatsPermission()) {
            requestUsageAccessPermission()
        }

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        if (!ForegroundAppService.isRunning) {
            startService(Intent(this, ForegroundAppService::class.java))
        }

        val viewModel = InstalledAppsViewModel(application)

        appChangeReceiver = AppChangeReceiver { packageName ->
            lifecycleScope.launch {
                sendInstallNotification(packageName)
                println("📦 App installed: $packageName — notification sent")
                PrivacyPolicyFetcher.fetchPrivacyPolicyContent(packageName)
            }
        }

        val intentFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }

        registerReceiver(appChangeReceiver, intentFilter)

        setContent {
            PrivacyPolicySummarizerTheme {
                val navController = rememberNavController()
    
                LaunchedEffect(packageNameToNavigate) {
                    if (!packageNameToNavigate.isNullOrEmpty()) {
                        navController.navigate("summary/$packageNameToNavigate")
                    }
                }
    
                AppNavigation(viewModel, navController)
            }
        }
    }

    private fun sendInstallNotification(packageName: String) {
        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to_package", packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "app_install_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "App Install Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("App Installed")
            .setContentText("Get Privacy Summary for $appName?")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

//        if (packageName != packageName) return
        notificationManager.notify(packageName.hashCode(), notification)
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(appChangeReceiver)
    }

    private fun savePrivacyPolicyToFile(packageName: String, policyContent: String) {
        try {
            val fileName = "${packageName.replace('.', '_')}_privacy_policy.txt"
            val file = File(filesDir, fileName)
            if (file.exists()) {
                println("Privacy policy already saved for $packageName at ${file.absolutePath}")
                return
            }
            file.writeText(policyContent)
            println("Privacy policy saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to save privacy policy: ${e.message}")
        }
    }

    // 🔽 Add this below
    private fun savePrivacyPolicyUrl(packageName: String, url: String) {
        try {
            val fileName = "${packageName.replace('.', '_')}_privacy_policy_url.txt"
            val file = File(filesDir, fileName)
            if (file.exists()) {
                println("Privacy policy already saved for $packageName at ${file.absolutePath}")
                return
            }
            file.writeText(url)
            println("Privacy policy URL saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to save privacy policy URL: ${e.message}")
        }
    }

    class AppChangeReceiver(private val onAppChanged: (String) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            val packageName = intent?.data?.schemeSpecificPart
            if (action == Intent.ACTION_PACKAGE_ADDED && packageName != null) {
                onAppChanged(packageName)
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    private fun requestUsageAccessPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}

@Composable
fun AppNavigation(viewModel: InstalledAppsViewModel, navController: NavHostController) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(viewModel = viewModel, onAppClick = { app ->
                navController.navigate("summary/${app.packageName}")
            })
        }
        composable("summary/{packageName}") { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            AppSummaryScreen(packageName = packageName)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: InstalledAppsViewModel, onAppClick: (ApplicationInfo) -> Unit) {
    val context = LocalContext.current
    val allApps by viewModel.appsList.collectAsState()
    var query by remember { mutableStateOf("") }

    val filteredApps = allApps.filter {
        it.loadLabel(context.packageManager).toString().contains(query, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Installed Apps") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            LazyColumn {
                items(filteredApps) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppClick(app) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = app.loadIcon(context.packageManager)
                        val bitmap = icon.toBitmapSafely()

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = app.loadLabel(context.packageManager).toString(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

fun Drawable.toBitmapSafely(): Bitmap {
    return if (this is BitmapDrawable) {
        this.bitmap
    } else {
        createBitmap(this.intrinsicWidth.coerceAtLeast(1), this.intrinsicHeight.coerceAtLeast(1)).also { bitmap ->
            val canvas = Canvas(bitmap)
            this.setBounds(0, 0, canvas.width, canvas.height)
            this.draw(canvas)
        }
    }
}