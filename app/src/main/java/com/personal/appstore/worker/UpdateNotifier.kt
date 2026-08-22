package com.personal.appstore.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.personal.appstore.R
import com.personal.appstore.domain.model.StoreApp
import com.personal.appstore.ui.MainActivity

/** Уведомления о новых версиях приложений. */
class UpdateNotifier(private val context: Context) {

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_updates),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_updates_description)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun notifyUpdates(apps: List<StoreApp>) {
        if (apps.isEmpty() || !canPostNotifications()) return
        ensureChannel()

        val title = when (apps.size) {
            1 -> "Обновление: ${apps.first().name}"
            else -> "Обновлений: ${apps.size}"
        }
        val text = apps.joinToString(", ") { "${it.name} ${it.latest.versionName}" }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /**
     * «Магазин обновился, откройте» после самообновления.
     *
     * Своё обновление всегда убивает процесс, а поднять активити из
     * широковещательного приёмника нельзя: с Android 10 фоновый запуск активити
     * запрещён. Уведомление — единственный надёжный способ вернуть человека в
     * магазин; так же ведёт себя и Google Play.
     */
    fun notifySelfUpdated(versionName: String?) {
        if (!canPostNotifications()) return
        ensureChannel()

        val contentIntent = PendingIntent.getActivity(
            context,
            SELF_UPDATE_REQUEST,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Магазин обновлён")
            .setContentText(
                versionName?.let { "Версия $it готова — нажмите, чтобы открыть" }
                    ?: "Нажмите, чтобы открыть",
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        NotificationManagerCompat.from(context).notify(SELF_UPDATE_NOTIFICATION_ID, notification)
    }

    /** Гасим уведомление, когда магазин и так открыт. */
    fun cancelSelfUpdated() {
        NotificationManagerCompat.from(context).cancel(SELF_UPDATE_NOTIFICATION_ID)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "app-updates"
        private const val NOTIFICATION_ID = 1001
        private const val SELF_UPDATE_NOTIFICATION_ID = 1002
        private const val SELF_UPDATE_REQUEST = 2
    }
}
