package com.personal.appstore.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.personal.appstore.StoreApplication
import com.personal.appstore.StoreConfig
import com.personal.appstore.domain.AppStatus
import com.personal.appstore.domain.AppStatusResolver
import com.personal.appstore.domain.model.StoreApp
import java.util.concurrent.TimeUnit

/**
 * Фоновая проверка витрины: раз в ~6 часов при наличии сети сравнивает манифест
 * с установленными пакетами и показывает уведомление о новых версиях.
 */
class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val locator = (applicationContext as StoreApplication).locator

        val manifest = try {
            locator.manifestRepository.refresh()
        } catch (e: Exception) {
            Log.w(TAG, "проверка обновлений не удалась", e)
            null
        } ?: return Result.retry()

        val updatable: List<StoreApp> = manifest.apps.filter { app ->
            val installed = locator.installedApps.find(app.id)
            AppStatusResolver.resolve(app.latest.versionCode, installed) is AppStatus.UpdateAvailable
        }

        if (updatable.isNotEmpty()) {
            locator.updateNotifier.notifyUpdates(updatable)
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "UpdateCheckWorker"
        private const val WORK_NAME = "update-check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                StoreConfig.UPDATE_CHECK_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
