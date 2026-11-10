package com.akitorahayashi.catboard.app

import android.content.Context
import com.akitorahayashi.catboard.catapi.createClient
import com.akitorahayashi.catboard.imageloader.createLoader
import com.akitorahayashi.catboard.imageprefetcher.createDatabase as createPrefetchDatabase
import com.akitorahayashi.catboard.imagescreener.Screener
import com.akitorahayashi.catboard.imagestore.createDatabase as createUrlDatabase
import java.io.File

fun createGalleryRuntime(context: Context, screener: Screener): GalleryRuntime {
    val application = context.applicationContext
    val cache = File(application.cacheDir, "cat-board-k")
    check(cache.isDirectory || cache.mkdirs()) { "The gallery cache directory could not be created" }
    val database = createUrlDatabase(application, File(cache, "urls.db").absolutePath)
    val prefetched = createPrefetchDatabase(application, File(cache, "prefetch.db").absolutePath)
    val images = createLoader(application, File(cache, "images").absolutePath)
    val api = createClient()
    return createRuntime(database, prefetched, images, api, screener, Preferences(application)) {
        images.close()
        api.close()
    }
}
