@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package com.akitorahayashi.catboard.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akitorahayashi.catboard.imageloader.Loader
import com.akitorahayashi.catboard.imageprefetcher.Buffer
import com.akitorahayashi.catboard.imagescreener.Screener
import com.akitorahayashi.catboard.imagescreener.ScreeningSettings
import com.akitorahayashi.catboard.imagestore.UrlRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@kotlin.native.HiddenFromObjC
class GalleryViewModel(
    private val repository: UrlRepository,
    private val loader: Loader,
    private val screener: Screener,
    private val prefetcher: Buffer,
    val settings: StateFlow<ScreeningSettings>,
    private val saveSettings: suspend (ScreeningSettings) -> Unit,
    backgroundFailures: Flow<Throwable?>,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val current = MutableStateFlow(GalleryState())
    val state = current.asStateFlow()
    private var loading: Job? = null
    private var nextID = 0L

    init {
        viewModelScope.launch {
            backgroundFailures.collect { error ->
                current.update { it.copy(warning = error?.let { cause -> cause.message ?: cause.toString() }) }
            }
        }
    }

    fun loadInitialImages(): Job? {
        if (state.value.images.isNotEmpty() || loading?.isActive == true) return null
        return refresh()
    }

    fun refresh(): Job = restart { }

    fun changeSettings(value: ScreeningSettings): Job = restart {
        prefetcher.clearAllPrefetchedImages()
        saveSettings(value)
    }

    private fun restart(beforeLoad: suspend () -> Unit): Job {
        val previous = loading
        previous?.cancel()
        return viewModelScope.launch {
            previous?.join()
            current.value = GalleryState()
            try {
                loader.clearMemoryCache()
                beforeLoad()
                loadBatches(30, 6)
                check(state.value.images.isNotEmpty()) { "設定に合う画像を取得できませんでした" }
                current.update { it.copy(isInitializing = false) }
                prefetcher.startPrefetchingIfNeeded()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                current.value = GalleryState(isInitializing = false, errorMessage = error.message ?: error.toString())
            }
        }.also { loading = it }
    }

    fun fetchAdditionalImages(): Job? {
        if (loading?.isActive == true || state.value.isInitializing || state.value.isAdditionalFetching) return null
        if (state.value.images.size >= 300) return refresh()

        return viewModelScope.launch {
            current.update { it.copy(isAdditionalFetching = true, errorMessage = null) }
            try {
                loadBatches(minOf(10, 300 - state.value.images.size), 2)
                current.update { it.copy(isAdditionalFetching = false) }
                prefetcher.startPrefetchingIfNeeded()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                current.update { it.copy(isAdditionalFetching = false, errorMessage = error.message ?: error.toString()) }
            }
        }.also { loading = it }
    }

    fun clearDisplayedImages() {
        loading?.cancel()
        current.value = GalleryState(isInitializing = false)
        loader.clearMemoryCache()
    }

    private suspend fun loadBatches(total: Int, batches: Int) {
        repeat(batches) { batch ->
            val count = total / batches + if (batch < total % batches) 1 else 0
            val images = fetchImages(count)
            currentCoroutineContext().ensureActive()
            current.update { it.copy(images = it.images + images) }
        }
    }

    private suspend fun fetchImages(count: Int): List<GalleryImage> {
        val result = prefetcher.getPrefetchedImages(count).toMutableList()
        require(result.size <= count) { "The prefetcher exceeded the requested image count" }
        repeat(5) {
            if (result.size >= count) return@repeat
            val needed = count - result.size
            val urls = repository.getNextImageURLs(needed)
            check(urls.isNotEmpty()) { "画像URLを取得できませんでした" }
            require(urls.size <= needed) { "The repository exceeded the requested batch size" }
            val images = loader.loadImageData(urls)
            val accepted = screener.screenImages(images, settings.value)
            require(accepted.size <= needed && accepted.size <= images.size && accepted.all { url -> images.any { it.imageURL == url } }) {
                "The screener returned an image outside the submitted batch"
            }
            result += accepted
        }
        return result.map { GalleryImage(nextID++, it) }
    }
}
