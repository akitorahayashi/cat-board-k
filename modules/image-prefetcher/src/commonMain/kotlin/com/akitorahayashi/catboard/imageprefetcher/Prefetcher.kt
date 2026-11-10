package com.akitorahayashi.catboard.imageprefetcher

import com.akitorahayashi.catboard.imageloader.Loader
import com.akitorahayashi.catboard.imagescreener.Screener
import com.akitorahayashi.catboard.imagescreener.ScreeningSettings
import com.akitorahayashi.catboard.imagestore.UrlRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Prefetcher(
    private val repository: UrlRepository,
    private val loader: Loader,
    private val screener: Screener,
    private val database: PrefetchDatabase,
    private val settings: StateFlow<ScreeningSettings>,
    private val scope: CoroutineScope,
) : Buffer {
    private val control = Mutex()
    private var job: Job? = null
    private val active = MutableStateFlow(false)
    private val failure = MutableStateFlow<Throwable?>(null)
    val isPrefetching = active.asStateFlow()
    val prefetchFailure = failure.asStateFlow()

    override suspend fun getPrefetchedCount(): Int {
        val current = settings.value
        return database.queue().count(current.isScreeningEnabled, current.scaryMode)
    }

    override suspend fun getPrefetchedImages(imageCount: Int): List<String> {
        val current = settings.value
        return database.queue().take(imageCount, current.isScreeningEnabled, current.scaryMode)
    }

    override suspend fun startPrefetchingIfNeeded() {
        control.withLock {
            if (job?.isActive == true || getPrefetchedCount() >= 180) return
            val current = settings.value
            active.value = true
            failure.value = null
            job = scope.launch {
                try {
                    var attempts = 0
                    while (settings.value == current && getPrefetchedCount() < 180 && attempts < 30) {
                        val count = minOf(10, 180 - getPrefetchedCount())
                        val urls = repository.getNextImageURLs(count)
                        check(urls.isNotEmpty()) { "事前読み込み用の画像URLを取得できませんでした。更新して再試行してください。" }
                        require(urls.size <= count) { "The repository exceeded the requested batch size" }
                        val images = loader.loadImageData(urls)
                        val accepted = screener.screenImages(images, current)
                        require(accepted.size <= images.size && accepted.all { url -> images.any { it.imageURL == url } }) {
                            "The screener returned an image outside the submitted batch"
                        }
                        database.queue().insert(accepted.map {
                            PrefetchedCatImageURL(it, current.isScreeningEnabled, current.scaryMode)
                        })
                        attempts++
                    }
                    check(settings.value != current || getPrefetchedCount() >= 180) {
                        "条件に合う画像を十分に事前読み込みできませんでした。必要に応じて設定を変更してください。"
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    failure.value = error
                } finally {
                    active.value = false
                }
            }
        }
    }

    override suspend fun clearAllPrefetchedImages() {
        control.withLock {
            job?.cancelAndJoin()
            job = null
            database.queue().clear()
            loader.clearMemoryCache()
            failure.value = null
        }
    }
}
