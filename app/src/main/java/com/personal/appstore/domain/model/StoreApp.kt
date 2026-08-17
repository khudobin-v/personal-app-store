package com.personal.appstore.domain.model

import java.time.Instant

/** Одна опубликованная версия приложения. */
data class AppRelease(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val apkSizeBytes: Long,
    val changelog: String,
    val releasedAt: Instant?,
)

/** Приложение в витрине: последняя версия + история (до 10 записей). */
data class StoreApp(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val latest: AppRelease,
    val history: List<AppRelease>,
)

/** Разобранная витрина. */
data class Manifest(
    val updatedAt: Instant?,
    val apps: List<StoreApp>,
)

/** Установленный на устройстве пакет. */
data class InstalledApp(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
)
