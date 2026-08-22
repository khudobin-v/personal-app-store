package com.personal.appstore.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.personal.appstore.ui.MainActivity
import com.personal.appstore.worker.UpdateNotifier

/**
 * Магазин обновил сам себя — процесс при этом всегда убивают, и обратно он не
 * поднимается. Возвращаем человека в приложение:
 *
 * - до Android 10 фоновый запуск активити ещё разрешён, поэтому просто
 *   открываем магазин заново;
 * - начиная с Android 10 такой запуск система молча блокирует, остаётся
 *   уведомление «Магазин обновлён» — так же делает и Google Play.
 */
class PackageReplacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val launch = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val started = runCatching { context.startActivity(launch) }.isSuccess
            if (started) return
            Log.w(TAG, "не удалось открыть магазин после обновления, показываем уведомление")
        }

        UpdateNotifier(context).notifySelfUpdated(versionName)
    }

    private companion object {
        const val TAG = "PackageReplaced"
    }
}
