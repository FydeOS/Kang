package io.fydeos.kangtester

import android.Manifest
import android.app.Notification
import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.app.Notification.FLAG_ONGOING_EVENT
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import io.fydeos.kangtester.databinding.FragmentNotificationCheckBinding
import java.util.*
import kotlin.concurrent.fixedRateTimer


/**
 * A simple [Fragment] subclass.
 * Use the [NotificationCheckFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NotificationCheckFragment : Fragment() {

    private var gotPermission = false
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        run {
            gotPermission = isGranted
            notificationPermission()
        }
    }

    private lateinit var _binding: FragmentNotificationCheckBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentNotificationCheckBinding.inflate(inflater, container, false)
        return _binding.root
    }

    private fun notificationPermission() {
        if (gotPermission) {
            _binding.tvNoPermission.visibility = View.GONE
            _binding.lNotification.visibility = View.VISIBLE
        } else {
            _binding.tvNoPermission.visibility = View.VISIBLE
            _binding.lNotification.visibility = View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                gotPermission = true
                notificationPermission()
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            gotPermission = true
            notificationPermission()
        }
        createNotificationChannel()
        _binding.btnBasicNotification.setOnClickListener { postBasicNotification() }
        _binding.btnNotificationWithAction.setOnClickListener { postNotificationWithAction() }
        _binding.btnDelNotification.setOnClickListener {
            if (_progerssNotificationId == notificationId) {
                _progressTimer?.cancel()
                _progressTimer = null
            }
            NotificationManagerCompat.from(context!!).cancel(notificationId)
        }
        _binding.btnNotificationWithProgress.setOnClickListener { postNotificationWithProgressBar() }
        _binding.btnPostNotificationWithImage.setOnClickListener { postNotificationWithImage() }
    }

    private fun createNotificationChannel() {
        val name = "Test"
        val descriptionText = "Test Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = getSystemService(context!!, NotificationManager::class.java)!!
        notificationManager.createNotificationChannel(mChannel)
    }

    private var notificationId = 0;

    private fun postBasicNotification() {
        val openUrlIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.notification_click_link)))
        val contentIntent = PendingIntent.getActivity(
            context!!,
            0,
            openUrlIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context!!, CHANNEL_ID)
            .setSmallIcon(R.drawable.tablet_icon)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_description))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(context!!)) {
            notificationId = (Math.random() * 10000).toInt()
            notify(notificationId, builder.build())
        }
    }

    private fun postNotificationWithImage() {
        val openUrlIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.notification_click_link)))
        val contentIntent = PendingIntent.getActivity(
            context!!,
            0,
            openUrlIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val bitmap = BitmapFactory.decodeResource(context!!.resources,
            R.drawable.fydetab)

        val builder = NotificationCompat.Builder(context!!, CHANNEL_ID)
            .setSmallIcon(R.drawable.tablet_icon)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_description))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setLargeIcon(bitmap)
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(null))
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(context!!)) {
            notificationId = (Math.random() * 10000).toInt()
            notify(notificationId, builder.build())
        }
    }

    // Key for the string that's delivered in the action's intent.
    private fun postNotificationWithAction() {
        notificationId = (Math.random() * 10000).toInt()
        val replyIntent =
            Intent(context!!.applicationContext, NotificationClickedReceiver::class.java).apply {
                action = BROADCAST_NAME
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_N_ACTION, KEY_ACTION_COMMENT)
            }
        val remoteInput: RemoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
            setLabel(getString(R.string.notification_type_prompt))
            build()
        }
        val m = if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val replyPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                activity!!.applicationContext,
                notificationId,
                replyIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or m
            )

        val actionComment = NotificationCompat.Action.Builder(
            R.drawable.baseline_psychology_24, getString(R.string.notification_action_1),
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val dismissIntent =
            Intent(context!!.applicationContext, NotificationClickedReceiver::class.java).apply {
                action = BROADCAST_NAME
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_N_ACTION, KEY_ACTION_DISMISS)
            }

        val dismissPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                activity!!.applicationContext,
                notificationId + 1,
                dismissIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or m
            )
        val actionDismiss = NotificationCompat.Action.Builder(
            R.drawable.baseline_psychology_24, getString(R.string.notification_action_2),
            dismissPendingIntent
        ).build()

        val builder = NotificationCompat.Builder(context!!, CHANNEL_ID).run {
            setSmallIcon(R.drawable.tablet_icon)
            setContentTitle(getString(R.string.notification_title))
            setContentText(getString(R.string.notification_description))
            priority = NotificationCompat.PRIORITY_DEFAULT
            addAction(actionComment)
            addAction(actionDismiss)
        }
        with(NotificationManagerCompat.from(context!!)) {
            notify(notificationId, builder.build())
        }
    }

    private var _progerssNotificationId = 0
    private var _progressTimer: Timer? = null
    private fun postNotificationWithProgressBar() {
        notificationId = (Math.random() * 10000).toInt()
        val contentIntent = PendingIntent.getActivity(
            context!!,
            0,
            Intent(),
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        _progerssNotificationId = notificationId
        val builder = NotificationCompat.Builder(context!!, CHANNEL_ID).apply {
            setContentTitle(getString(R.string.notification_title_2))
            setSmallIcon(R.drawable.baseline_psychology_24)
            setOnlyAlertOnce(true)
        }
        val PROGRESS_MAX = 100
        var PROGRESS_CURRENT = 0
        val handler = view!!.handler
        NotificationManagerCompat.from(context!!).apply {
            // Issue the initial notification with zero progress
            builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, _binding.cbProgressIndeterminate.isChecked)
            notify(_progerssNotificationId, builder.build())

            _progressTimer?.cancel()
            _progressTimer = fixedRateTimer("progress", false, 0, 500) {
                handler.post {
                    if (PROGRESS_CURRENT >= PROGRESS_MAX) {
                        builder
                            .setContentText(getString(R.string.notification_content_3))
                            .setContentTitle(getString(R.string.notification_title_3))
                            .setProgress(0, 0, false)
                            .setContentIntent(contentIntent)
                            .setAutoCancel(true)
                        notify(_progerssNotificationId, builder.build())
                        _progressTimer?.cancel()
                    } else {
                        PROGRESS_CURRENT += 10
                        builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, _binding.cbProgressIndeterminate.isChecked);
                        notify(_progerssNotificationId, builder.build());
                    }
                }
            }
        }
    }

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val CHANNEL_ID = "test_ch"
        const val EXTRA_N_ACTION = "io.fydeos.kangtester.action"
        const val KEY_ACTION_COMMENT = "comment"
        const val KEY_ACTION_DISMISS = "dismiss"
        const val BROADCAST_NAME = "io.fydeos.kangtester.NOTIFICATION_REPLIED"
    }
}