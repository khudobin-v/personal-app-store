package com.personal.appstore.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Установка через PackageInstaller.Session.
 *
 * Плюсы против ACTION_VIEW: приходит результат установки, не нужен FileProvider,
 * а на 31+ обновление приложения, которое поставил сам магазин, может пройти
 * вообще без диалога (UPDATE_PACKAGES_WITHOUT_USER_ACTION).
 */
class SessionApkInstaller(private val context: Context) : ApkInstaller {

    override suspend fun install(file: File, appId: String): InstallOutcome = withContext(Dispatchers.IO) {
        if (!InstallPermissions.canInstallPackages(context)) {
            return@withContext InstallOutcome.NeedsUnknownSourcesPermission
        }

        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(appId)
            setSize(file.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Система сама покажет диалог, если посчитает нужным.
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE)
            }
        }

        var sessionId = -1
        try {
            sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite(WRITE_NAME, 0, file.length()).use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                    session.fsync(output)
                }
                session.commit(statusIntentSender(sessionId, appId))
            }
            InstallOutcome.Started
        } catch (e: Exception) {
            if (sessionId >= 0) runCatching { installer.abandonSession(sessionId) }
            Log.w(TAG, "PackageInstaller.Session не сработал", e)
            InstallOutcome.Failed(e.message ?: "установка не удалась")
        }
    }

    private fun statusIntentSender(sessionId: Int, appId: String) = PendingIntent.getBroadcast(
        context,
        sessionId,
        Intent(context, InstallResultReceiver::class.java)
            .putExtra(InstallResultReceiver.EXTRA_APP_ID, appId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    ).intentSender

    private companion object {
        const val WRITE_NAME = "apk"
        const val TAG = "SessionApkInstaller"
    }
}
