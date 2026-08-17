package com.personal.appstore.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.core.content.IntentCompat

/**
 * Принимает статусы PackageInstaller.Session.
 *
 * STATUS_PENDING_USER_ACTION — система просит показать подтверждение установки;
 * этот intent нужно запустить как есть.
 */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)
        val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
            ?: intent.getStringExtra(EXTRA_APP_ID)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirmation == null) {
                    InstallEvents.publish(
                        InstallEvent.Failed(packageName, "система не прислала диалог подтверждения"),
                    )
                    return
                }
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(confirmation) }
                    .onFailure { error ->
                        Log.w(TAG, "не удалось показать подтверждение установки", error)
                        InstallEvents.publish(
                            InstallEvent.Failed(packageName, "не удалось показать окно установки"),
                        )
                    }
            }

            PackageInstaller.STATUS_SUCCESS ->
                InstallEvents.publish(InstallEvent.Success(packageName))

            PackageInstaller.STATUS_FAILURE_ABORTED ->
                InstallEvents.publish(InstallEvent.Cancelled(packageName))

            else -> InstallEvents.publish(
                InstallEvent.Failed(packageName, message ?: "установка не удалась (код $status)"),
            )
        }
    }

    companion object {
        const val EXTRA_APP_ID = "com.personal.appstore.EXTRA_APP_ID"
        private const val TAG = "InstallResultReceiver"
    }
}
