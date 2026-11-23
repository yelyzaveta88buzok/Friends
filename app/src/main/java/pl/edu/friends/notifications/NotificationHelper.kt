package pl.edu.friends.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pl.edu.friends.R
import pl.edu.friends.util.AppLocale

object NotificationHelper {

    private const val CHANNEL_ID = "besties_capsules_channel"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showUnlockedCapsuleNotification(context: Context, capsuleTitle: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                AppLocale.t(
                    "🔓 Капсулу Часу відкрито!",
                    "🔓 Time Capsule Unlocked!",
                    "🔓 Kapsuła Czasu odblokowana!"
                )
            )
            .setContentText(
                AppLocale.t(
                    "Вже можна прочитати капсулу: $capsuleTitle",
                    "You can now read: $capsuleTitle",
                    "Możesz już odczytać kapsułę: $capsuleTitle"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(capsuleTitle.hashCode(), notification)
    }
}
