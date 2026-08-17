package com.personal.appstore.di

import android.content.Context
import com.personal.appstore.StoreConfig
import com.personal.appstore.data.ManifestRepository
import com.personal.appstore.data.local.DataStoreManifestCache
import com.personal.appstore.data.remote.ApkDownloader
import com.personal.appstore.data.remote.ManifestApi
import com.personal.appstore.domain.InstalledAppsProvider
import com.personal.appstore.installer.ApkInstaller
import com.personal.appstore.installer.FallbackApkInstaller
import com.personal.appstore.installer.IntentApkInstaller
import com.personal.appstore.installer.SessionApkInstaller
import com.personal.appstore.worker.UpdateNotifier
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Ручной DI: зависимостей немного, Hilt с кодогенерацией здесь только замедлил бы сборку.
 * Живёт в [com.personal.appstore.StoreApplication], доступен воркеру и ViewModel.
 */
class ServiceLocator(private val appContext: Context) {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.MINUTES) // большой APK на медленной сети
            .retryOnConnectionFailure(true)
            .build()
    }

    val manifestRepository: ManifestRepository by lazy {
        ManifestRepository(
            api = ManifestApi(okHttpClient, StoreConfig.manifestUrl),
            cache = DataStoreManifestCache(appContext),
        )
    }

    val apkDownloader: ApkDownloader by lazy {
        ApkDownloader(okHttpClient, File(appContext.cacheDir, "apk"))
    }

    val installedApps: InstalledAppsProvider by lazy { InstalledAppsProvider(appContext) }

    val apkInstaller: ApkInstaller by lazy {
        FallbackApkInstaller(
            primary = SessionApkInstaller(appContext),
            fallback = IntentApkInstaller(appContext),
        )
    }

    val updateNotifier: UpdateNotifier by lazy { UpdateNotifier(appContext) }
}
