package pl.edu.friends

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import pl.edu.friends.notifications.CapsuleCheckWorker
import pl.edu.friends.notifications.NotificationHelper
import java.util.concurrent.TimeUnit

class FriendsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)

        val workRequest = PeriodicWorkRequestBuilder<CapsuleCheckWorker>(12, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BestiesCapsuleCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
