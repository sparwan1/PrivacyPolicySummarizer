package com.example.privacypolicysummarizer

import android.content.Intent
import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch



class InstalledAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val _appsList = MutableStateFlow<List<ApplicationInfo>>(emptyList())
    val appsList = _appsList.asStateFlow()

    private val packageManager: PackageManager = application.packageManager

    fun loadInstalledApps() {
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolvedActivities = packageManager.queryIntentActivities(intent, 0)

            val userApps = resolvedActivities.map { it.activityInfo.applicationInfo }
                .filter { app ->
                    val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val label = app.loadLabel(packageManager).toString()
                    !isSystemApp && label.isNotBlank()
                }
                .distinctBy { it.packageName }
                .sortedBy { it.loadLabel(packageManager).toString() }

            _appsList.value = userApps
        }
    }

}


