package io.fydeos.kangtester

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.app.NotificationCompat


class OverlayService : Service() {
    private var wm: WindowManager? = null
    private var huaji = true
    private var button: ImageButton? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        val CHANNEL_ID = "overlay_channel"
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Overlay notification",
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay is displayed")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        startForeground(9999, notification)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        huaji = true
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.START or Gravity.TOP
        params.x = 0
        params.y = 0


        button = ImageButton(this);
        button!!.setImageResource(R.drawable.huaji)
        button!!.alpha = 1.0f;
        button!!.background = null
        button!!.setOnClickListener {
            button!!.setImageResource(if (huaji) R.drawable.pen else R.drawable.huaji)
            huaji = !huaji
        }
        button!!.setOnLongClickListener {
            stopSelf()
            return@setOnLongClickListener true
        }

        button!!.setOnTouchListener(object : OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    MotionEvent.ACTION_UP -> {
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        if (button != null)
                            wm!!.updateViewLayout(button, params)
                    }
                }
                return false
            }
        })

        wm!!.addView(button, params)
    }


    override fun onDestroy() {
        super.onDestroy()
        if (button != null) {
            wm!!.removeView(button)
            button = null
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}