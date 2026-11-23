package pl.edu.friends.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pl.edu.friends.data.AppDatabase
import pl.edu.friends.util.BestiesLogicHelper

class CapsuleCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dao = AppDatabase.getDatabase(applicationContext).bestiesDao()
            val capsules = dao.getAllCapsulesSync()
            val now = System.currentTimeMillis()
            val unlocked = capsules.firstOrNull { BestiesLogicHelper.isCapsuleUnlocked(it, now) }
            if (unlocked != null) {
                NotificationHelper.showUnlockedCapsuleNotification(applicationContext, unlocked.title)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
