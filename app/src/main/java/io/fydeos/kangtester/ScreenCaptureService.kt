package io.fydeos.kangtester

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.random.Random


class ScreenCaptureService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    override fun onCreate() {
        super.onCreate()
        startNotification()
    }

    private var notificationId = 0

    private fun startNotification() {
        notificationId = Random.nextInt()
        val id = "ScreenCaptureChannel1"
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, id)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_launcher_foreground
                    )
                )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.capturing_screen))
        val notification: Notification = notificationBuilder.build()
        val channel = NotificationChannel(
            id,
            "Screen Capture",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        startForeground(
            notificationId,
            notification
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}