package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.VpnRepository
import java.util.concurrent.TimeUnit

/**
 * مهمة مجدولة لتحديث قائمة الخوادم بشكل دوري في الخلفية باستخدام WorkManager
 * Periodic background worker for fetching fresh VPN Gate servers.
 */
class RefreshServersWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = VpnRepository(database.vpnServerDao())
            val refreshResult = repository.refreshServers()

            if (refreshResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "periodic_vpn_servers_refresh"

        fun schedule(context: Context, enabled: Boolean) {
            try {
                val workManager = WorkManager.getInstance(context)
                if (enabled) {
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

                    // تحديث كل 6 ساعات مع قيود توفر الشبكة
                    val request = PeriodicWorkRequestBuilder<RefreshServersWorker>(6, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build()

                    workManager.enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        request
                    )
                } else {
                    workManager.cancelUniqueWork(WORK_NAME)
                }
            } catch (e: Exception) {
                // تجاهل الاستثناء في بيئة اختبارات الوحدة / Robolectric
            }
        }
    }
}
