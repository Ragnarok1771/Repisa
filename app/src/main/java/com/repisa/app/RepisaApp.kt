package com.repisa.app

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.repisa.app.data.Http
import com.repisa.app.i18n.L10n

class RepisaApp : Application(), SingletonImageLoader.Factory {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(L10n.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        L10n.init(this)
    }

    /**
     * Carga de carátulas: caché en memoria generosa (las cajas se ven una y otra vez al desplazarse)
     * y caché en disco grande, para que tras la primera vez no se vuelvan a descargar.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { Http.client })) }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.3).build() }
        .diskCache {
            DiskCache.Builder().directory(context.cacheDir.resolve("covers")).maxSizeBytes(768L * 1024 * 1024).build()
        }
        .crossfade(120)
        .build()
}
