package io.fydeos.kangtester

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput

class NotificationClickedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val act = intent.getStringExtra(NotificationCheckFragment.EXTRA_N_ACTION)
        if (act == NotificationCheckFragment.KEY_ACTION_COMMENT) {
            val txt = RemoteInput.getResultsFromIntent(intent)
                ?.getCharSequence(NotificationCheckFragment.KEY_TEXT_REPLY)
            val repliedNotification =
                Notification.Builder(context, NotificationCheckFragment.CHANNEL_ID)
                    .setSmallIcon(R.drawable.tablet_icon)
                    .setContentText(
                        context.getString(R.string.notification_comment_received).format(txt)
                    )
                    .build()

            context.sendBroadcast(Intent(RECV_ACTION).putExtra(NotificationCheckFragment.KEY_TEXT_REPLY, txt))

// Issue the new notification.
            NotificationManagerCompat.from(context).notify(
                intent.getIntExtra(Notification.EXTRA_NOTIFICATION_ID, 0),
                repliedNotification
            )
        } else if (act == NotificationCheckFragment.KEY_ACTION_DISMISS) {
            NotificationManagerCompat.from(context)
                .cancel(intent.getIntExtra(Notification.EXTRA_NOTIFICATION_ID, 0))
        }
    }

    companion object {
        val RECV_ACTION = "io.fydeos.kangtester.NOTIFICATION_COMMENT"
    }

}