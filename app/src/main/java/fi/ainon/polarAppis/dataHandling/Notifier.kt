package fi.ainon.polarAppis.dataHandling

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.R
import javax.inject.Inject
import javax.inject.Singleton


private const val TARGET_ACTIVITY_NAME = "fi.ainon.polarAppis.ui.MainActivity"
private val NOTIFICATION_CHANNEL_ID: String = "polarAppisNotificationChan"
private val NOTIFICATION_REQUEST_CODE = 0

@Module
@InstallIn(SingletonComponent::class)
interface NotifierModule {

    @Singleton
    @Binds
    fun bindsNotifier(
        notifier: SystemTrayNotifier
    ): Notifier
}


interface Notifier {
    fun postNotification(message: String)
}

class SystemTrayNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : Notifier {


    private val NOTIFICATION_GROUP = "PolarAppisNofifikaatioGroup"

    override fun postNotification(message: String
    ) = with(context) {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }


        PendingIntent.getActivity(
            this,
            NOTIFICATION_REQUEST_CODE,
            Intent().apply {
                action = Intent.ACTION_VIEW
                component = ComponentName(
                    packageName,
                    TARGET_ACTIVITY_NAME,
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val intent = Intent(this, SystemTrayNotifier::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)


        val notification = createNotification {
            setContentTitle("Hei")
                .setSmallIcon(R.drawable.push_icon)
                .setContentText("Huomaa mut $message")
                .setGroup(NOTIFICATION_GROUP)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

        }

        // Send the notifications
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notification,
        )

    }

}

private fun Context.createNotification(
    block: NotificationCompat.Builder.() -> Unit,
): Notification {
    ensureNotificationChannelExists()
    return NotificationCompat.Builder(
        this,
        NOTIFICATION_CHANNEL_ID,
    )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .apply(block)
        .build()
}

/**
 * Ensures that a notification channel is present if applicable
 */
private fun Context.ensureNotificationChannelExists() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        getString(R.string.core_notifications_pol_notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description =
            getString(R.string.core_notifications_pol_notification_channel_description)
    }
    // Register the channel with the system
    NotificationManagerCompat.from(this).createNotificationChannel(channel)
}
