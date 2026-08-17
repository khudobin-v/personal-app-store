package com.personal.appstore.domain

import com.personal.appstore.domain.model.InstalledApp

/** Состояние приложения из витрины относительно того, что стоит на устройстве. */
sealed interface AppStatus {

    /** Пакета нет на устройстве → «Установить». */
    data object NotInstalled : AppStatus

    /** Установлена версия старее витрины → «Обновить». */
    data class UpdateAvailable(val installed: InstalledApp) : AppStatus

    /** Установлена актуальная версия → «Открыть». */
    data class UpToDate(val installed: InstalledApp) : AppStatus

    /**
     * Установлено новее, чем в витрине (например, отладочная сборка).
     * Тоже «Открыть»: понижать версию Android не даст.
     */
    data class InstalledNewer(val installed: InstalledApp) : AppStatus
}

/** Приложение можно запустить с устройства. */
val AppStatus.isInstalled: Boolean
    get() = this !is AppStatus.NotInstalled

/** Есть что скачивать. */
val AppStatus.needsDownload: Boolean
    get() = this is AppStatus.NotInstalled || this is AppStatus.UpdateAvailable

/**
 * Единственное место, где решается, что показывать на кнопке.
 * Сравнение только по versionCode: versionName — человеческая строка,
 * порядок по ней не гарантирован.
 */
object AppStatusResolver {

    fun resolve(latestVersionCode: Long, installed: InstalledApp?): AppStatus = when {
        installed == null -> AppStatus.NotInstalled
        installed.versionCode < latestVersionCode -> AppStatus.UpdateAvailable(installed)
        installed.versionCode == latestVersionCode -> AppStatus.UpToDate(installed)
        else -> AppStatus.InstalledNewer(installed)
    }
}
