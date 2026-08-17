package com.personal.appstore.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.personal.appstore.domain.model.InstalledApp
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Что из витрины реально стоит на устройстве.
 *
 * Опрашивается точечно по id из манифеста: список установленных приложений
 * целиком нам не нужен.
 */
class InstalledAppsProvider(private val context: Context) {

    private val _installed = MutableStateFlow<Map<String, InstalledApp>>(emptyMap())
    val installed: StateFlow<Map<String, InstalledApp>> = _installed.asStateFlow()

    fun refresh(packageNames: Collection<String>) {
        val pm = context.packageManager
        _installed.value = packageNames
            .distinct()
            .mapNotNull { packageName -> query(pm, packageName) }
            .associateBy { it.packageName }
    }

    fun find(packageName: String): InstalledApp? = query(context.packageManager, packageName)

    /** Intent запуска приложения; null — если приложение без launcher-активности. */
    fun launchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)

    private fun query(pm: PackageManager, packageName: String): InstalledApp? = try {
        val info = pm.getPackageInfo(packageName, 0)
        InstalledApp(
            packageName = packageName,
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            versionName = info.versionName.orEmpty(),
        )
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
