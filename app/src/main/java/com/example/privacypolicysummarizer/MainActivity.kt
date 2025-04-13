package com.example.privacypolicysummarizer

import android.content.BroadcastReceiver
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.privacypolicysummarizer.ui.theme.PrivacyPolicySummarizerTheme
import androidx.core.graphics.createBitmap

class MainActivity : ComponentActivity() {

    private var appChangeReceiver: AppChangeReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = InstalledAppsViewModel(application)

        appChangeReceiver = AppChangeReceiver {
            viewModel.loadInstalledApps()
        }

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        registerReceiver(appChangeReceiver, intentFilter)

        setContent {
            PrivacyPolicySummarizerTheme {
                AppNavigation(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(appChangeReceiver)
    }

    class AppChangeReceiver(private val onAppChanged: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REMOVED) {
                onAppChanged()
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: InstalledAppsViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(viewModel = viewModel, onAppClick = { app ->
                // Navigate and pass the app package name
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
fun HomeScreen(viewModel: InstalledAppsViewModel, onAppClick: (App) -> Unit) {
    val context = LocalContext.current
    val apps by viewModel.appsList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Apps") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(apps) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onAppClick(app) }
                ) {
                    val icon = app.loadIcon(context.packageManager)
                    val bitmap = icon.toBitmapSafely()

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(app.loadLabel(context.packageManager).toString())
                    }
                }
            }
        }
    }
}

@Composable
fun AppSummaryScreen(packageName: String) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("App Summary") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Summary for package: $packageName")
            // Add more UI elements to display the summary
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
