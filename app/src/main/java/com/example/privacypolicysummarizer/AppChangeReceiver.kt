package com.example.privacypolicysummarizer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppChangeReceiver(private val onAppChanged: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REMOVED) {
            onAppChanged()
        }
    }
}
