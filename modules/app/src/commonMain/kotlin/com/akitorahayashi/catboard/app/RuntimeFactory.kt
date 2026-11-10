package com.akitorahayashi.catboard.app

import com.akitorahayashi.catboard.catapi.ImageSource
import com.akitorahayashi.catboard.imageloader.Loader
import com.akitorahayashi.catboard.imageprefetcher.PrefetchDatabase
import com.akitorahayashi.catboard.imageprefetcher.Prefetcher
import com.akitorahayashi.catboard.imagescreener.Screener
import com.akitorahayashi.catboard.imagestore.Repository
import com.akitorahayashi.catboard.imagestore.UrlDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine

internal fun createRuntime(
    database: UrlDatabase,
    prefetched: PrefetchDatabase,
    loader: Loader,
    source: ImageSource,
    screener: Screener,
    settings: SettingsStore,
    onClose: () -> Unit,
): GalleryRuntime {
    val job = SupervisorJob()
    val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    val repository = Repository(database, source, scope)
    val prefetcher = Prefetcher(repository, loader, screener, prefetched, settings.settings, scope)
    val failures = combine(repository.refillFailure, prefetcher.prefetchFailure) { refill, prefetch -> refill ?: prefetch }
    job.invokeOnCompletion {
        database.close()
        prefetched.close()
        onClose()
    }
    return GalleryRuntime(scope, loader) { sessionScope ->
        GalleryViewModel(repository, loader, screener, prefetcher, settings.settings, settings::save, failures, sessionScope)
    }
}
