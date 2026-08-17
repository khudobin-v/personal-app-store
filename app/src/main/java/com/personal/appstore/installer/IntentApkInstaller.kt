package com.personal.appstore.installer

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Установка через ACTION_VIEW + FileProvider — совместимый путь на случай,
 * если сессионный установщик недоступен на конкретной прошивке.
 */
class IntentApkInstaller(private val context: Context) : ApkInstaller {

    override suspend fun install(file: File, appId: String): InstallOutcome = withContext(Dispatchers.IO) {
        if (!InstallPermissions.canInstallPackages(context)) {
            return@withContext InstallOutcome.NeedsUnknownSourcesPermission
        }

        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
            context.startActivity(intent)
            InstallOutcome.Started
        } catch (e: Exception) {
            InstallOutcome.Failed(e.message ?: "не удалось открыть установщик")
        }
    }

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}

/**
 * Сначала пробуем сессионный установщик, при отказе — intent.
 * Пользователю в любом случае показывается системный диалог установки.
 */
class FallbackApkInstaller(
    private val primary: ApkInstaller,
    private val fallback: ApkInstaller,
) : ApkInstaller {

    override suspend fun install(file: File, appId: String): InstallOutcome =
        when (val outcome = primary.install(file, appId)) {
            is InstallOutcome.Failed -> fallback.install(file, appId)
            else -> outcome
        }
}
