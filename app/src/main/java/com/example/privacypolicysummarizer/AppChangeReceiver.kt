package com.example.privacypolicysummarizer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppChangeReceiver(private val onAppChanged: (String) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_PACKAGE_ADDED) {
            // Extract the package name from the intent data
            val packageName = intent.data?.schemeSpecificPart
            if (!packageName.isNullOrEmpty()) {
                onAppChanged(packageName)
            }
        }
    }
}
