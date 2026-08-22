package com.personal.appstore.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
 * Фоновая проверка витрины: раз в час при наличии сети сравнивает манифест с
 * установленными пакетами и показывает уведомление о новых версиях.
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

        // Об одной и той же версии уведомляем один раз: проверка ходит по кругу,
        // а обновиться человек мог решить не сразу.
        val keys = updatable.map { "${it.id}:${it.latest.versionCode}" }.toSet()
        val alreadyNotified = locator.settingsStore.notifiedUpdates()
        val fresh = updatable.filter { "${it.id}:${it.latest.versionCode}" !in alreadyNotified }

        if (fresh.isNotEmpty()) {
            locator.updateNotifier.notifyUpdates(fresh)
        }
        // Пишем ровно текущий набор: установленные обновления уходят из списка сами.
        if (keys != alreadyNotified) {
            locator.settingsStore.setNotifiedUpdates(keys)
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "UpdateCheckWorker"
        private const val WORK_NAME = "update-check"
        private const val INITIAL_WORK_NAME = "update-check-initial"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodic = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                StoreConfig.UPDATE_CHECK_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            val workManager = WorkManager.getInstance(context)
            // UPDATE, а не KEEP: с KEEP у тех, кто уже поставил магазин, навсегда
            // осталась бы старая периодичность из прошлой версии.
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodic,
            )

            // Периодическую работу система ставит на конец первого интервала:
            // после установки первая проверка была бы только через шесть часов.
            // Поэтому отдельно гоняем разовую — с задержкой, чтобы уведомление
            // не прилетело человеку прямо поверх открытого магазина.
            val initial = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                .setConstraints(constraints)
                .setInitialDelay(StoreConfig.FIRST_UPDATE_CHECK_DELAY_MINUTES, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniqueWork(
                INITIAL_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                initial,
            )
        }
    }
}
