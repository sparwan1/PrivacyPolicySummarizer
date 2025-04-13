package com.example.privacypolicysummarizer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap

@Composable
fun HomeScreen(
    viewModel: InstalledAppsViewModel,
    onAppClick: (app: android.content.pm.ApplicationInfo) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val apps by viewModel.appsList.collectAsState()
    val filteredApps = apps.filter {
        it.loadLabel(context.packageManager).toString().contains(searchQuery, ignoreCase = true)
    }

    // Load apps when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Apps") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            LazyColumn {
                items(filteredApps) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppClick(app) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        val icon = app.loadIcon(context.packageManager)
                        val bitmap = when (icon) {
                            is android.graphics.drawable.BitmapDrawable -> icon.bitmap
                            else -> icon.toBitmapSafely()
                        }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = app.loadLabel(context.packageManager).toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}