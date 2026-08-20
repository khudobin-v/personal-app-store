package com.personal.appstore.data.model

import kotlinx.serialization.Serializable

/**
 * Транспортные модели apps.json (schemaVersion 1).
 *
 * Контракт формата описан в README репозитория манифеста; менять его нельзя —
 * тот же файл пишет scripts/update_manifest.py на стороне CI.
 */
@Serializable
data class ManifestDto(
    val schemaVersion: Int = SUPPORTED_SCHEMA_VERSION,
    val updatedAt: String? = null,
    val apps: List<AppDto> = emptyList(),
) {
    companion object {
        const val SUPPORTED_SCHEMA_VERSION: Int = 1
    }
}

@Serializable
data class AppDto(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val apkSizeBytes: Long,
    val changelog: String = "",
    val releasedAt: String? = null,
    /** Кто опубликовал последнюю версию: логин издателя либо 'ci'. */
    val publishedBy: String? = null,
    val versions: List<VersionDto> = emptyList(),
)

@Serializable
data class VersionDto(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val apkSizeBytes: Long,
    val changelog: String = "",
    val releasedAt: String? = null,
)
