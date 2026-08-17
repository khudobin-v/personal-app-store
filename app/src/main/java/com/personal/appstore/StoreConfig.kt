package com.personal.appstore

/**
 * Конфигурация магазина.
 *
 * Адрес витрины задаётся ровно в одном месте — свойством `MANIFEST_URL`
 * в `gradle.properties`, откуда попадает в `BuildConfig` (см. app/build.gradle.kts).
 */
object StoreConfig {

    /**
     * raw-ссылка на apps.json по умолчанию (из gradle.properties).
     * Пользователь может переопределить её в настройках: магазин умеет искать
     * витрину по GitHub-логину, см. SettingsStore и RepoDiscovery.
     */
    val defaultManifestUrl: String = BuildConfig.MANIFEST_URL

    /** packageName самого магазина — для баннера самообновления. */
    val storePackageName: String = BuildConfig.APPLICATION_ID

    /** Периодичность фоновой проверки обновлений. */
    const val UPDATE_CHECK_INTERVAL_HOURS: Long = 6L

    /** Манифест старше этого возраста считается устаревшим (офлайн-режим). */
    const val STALE_AFTER_MILLIS: Long = 24L * 60 * 60 * 1000

    /** Число попыток загрузки манифеста при сетевых ошибках. */
    const val NETWORK_RETRIES: Int = 3
}
