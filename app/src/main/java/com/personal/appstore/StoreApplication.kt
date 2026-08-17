package com.personal.appstore

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.personal.appstore.di.ServiceLocator
import com.personal.appstore.worker.UpdateCheckWorker

class StoreApplication : Application(), SingletonImageLoader.Factory {

    lateinit var locator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        locator = ServiceLocator(this)
        locator.updateNotifier.ensureChannel()
        UpdateCheckWorker.schedule(this)
    }

    /** Иконки грузим тем же OkHttp-клиентом, что и всё остальное. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { locator.okHttpClient }))
            }
            .crossfade(true)
            .build()
}
