package com.example.privacypolicysummarizer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.app.PendingIntent

class ForegroundAppService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastApp: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post(checkAppRunnable)
        return START_STICKY
    }

    private val checkAppRunnable = object : Runnable {
        override fun run() {
            val currentApp = getForegroundApp()
            if (currentApp != null && currentApp != lastApp) {
                sendNotification(currentApp)
                lastApp = currentApp
            }
            handler.postDelayed(this, 2000) // Check every 2 sec
        }
    }

    private fun getForegroundApp(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 10000
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
        val recent = stats.maxByOrNull { it.lastTimeUsed }
        return recent?.packageName
    }

    private fun sendNotification(packageName: String) {
        val appName = packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(packageName, 0)
        )

        val channelId = "app_open_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "App Open Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // 🔽 Create Intent to open MainActivity with the packageName
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

        val notification = NotificationCompat.Builder(this, channelId)
        .setContentTitle("App Privacy Summary")
        .setContentText("Get Privacy Summary for $appName?")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(pendingIntent) // 👈 attach intent here
        .setAutoCancel(true)
        .build()

        if(packageName != "com.example.privacypolicysummarizer") {
            notificationManager.notify(packageName.hashCode(), notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
