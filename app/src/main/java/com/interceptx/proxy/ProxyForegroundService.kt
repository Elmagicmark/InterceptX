package com.interceptx.proxy

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.interceptx.InterceptXApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

class ProxyForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var engine: ProxyEngine

    companion object {
        const val CHANNEL_ID = "interceptx_proxy_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.interceptx.action.START_PROXY"
        const val ACTION_STOP = "com.interceptx.action.STOP_PROXY"
        const val EXTRA_PORT = "extra_port"
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as InterceptXApp
        engine = app.proxyEngine
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                engine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val port = intent?.getIntExtra(EXTRA_PORT, 8080) ?: 8080
                startForeground(NOTIFICATION_ID, buildNotification(port))
                engine.start(port)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine.stop()
        super.onDestroy()
    }

    private fun buildNotification(port: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("InterceptX Proxy Active")
            .setContentText("Listening on 127.0.0.1:$port")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Proxy Service", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
