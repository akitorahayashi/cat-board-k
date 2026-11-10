package com.akitorahayashi.catboard.app

import com.akitorahayashi.catboard.imageloader.ImageData
import com.akitorahayashi.catboard.imageloader.Loader
import com.akitorahayashi.catboard.imageprefetcher.Buffer
import com.akitorahayashi.catboard.imagescreener.Screener
import com.akitorahayashi.catboard.imagescreener.ScreeningSettings
import com.akitorahayashi.catboard.imagestore.UrlRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GalleryViewModelTest {
    @Test
    fun initialAndAdditionalLoadsPublishTheirCompletedState() = runTest {
        val scenario = scenario()
        val model = scenario.model
        assertTrue(model.state.value.images.isEmpty())
        requireNotNull(model.loadInitialImages()).join()
        assertEquals(30, model.state.value.images.size)
        assertFalse(model.state.value.isInitializing)
        assertTrue(scenario.buffer.started)
        requireNotNull(model.fetchAdditionalImages()).join()
        assertEquals(40, model.state.value.images.size)
        assertFalse(model.state.value.isAdditionalFetching)
    }

    @Test
    fun prefetchedImagesAreConsumedBeforeRequestingNewURLs() = runTest {
        val scenario = scenario()
        scenario.buffer.urls.addAll(List(30) { "prefetched-$it" })
        requireNotNull(scenario.model.loadInitialImages()).join()
        assertEquals(List(30) { "prefetched-$it" }, scenario.model.state.value.images.map { it.imageURL })
        assertEquals(0, scenario.requests)
    }

    @Test
    fun reachingTheDisplayLimitStartsANewGallery() = runTest {
        val model = scenario().model
        requireNotNull(model.loadInitialImages()).join()
        repeat(27) { requireNotNull(model.fetchAdditionalImages()).join() }
        assertEquals(300, model.state.value.images.size)
        requireNotNull(model.fetchAdditionalImages()).join()
        assertEquals(30, model.state.value.images.size)
        assertFalse(model.state.value.isInitializing)
        assertFalse(model.state.value.isAdditionalFetching)
    }

    @Test
    fun repeatedAdditionalLoadRequestsDoNotOverlap() = runTest {
        val model = scenario().model
        requireNotNull(model.loadInitialImages()).join()
        val first = requireNotNull(model.fetchAdditionalImages())
        assertNull(model.fetchAdditionalImages())
        first.join()
        assertEquals(40, model.state.value.images.size)
    }

    @Test
    fun anInitialFailureIsVisibleAndRefreshCanRecover() = runTest {
        val scenario = scenario()
        scenario.loader.failNext = true
        requireNotNull(scenario.model.loadInitialImages()).join()
        assertTrue(scenario.model.state.value.images.isEmpty())
        assertNotNull(scenario.model.state.value.errorMessage)
        scenario.model.refresh().join()
        assertEquals(30, scenario.model.state.value.images.size)
        assertNull(scenario.model.state.value.errorMessage)
    }

    @Test
    fun screeningCanRejectAllImagesWithoutDisplayingUnscreenedData() = runTest {
        val rejecting = object : Screener {
            override suspend fun screenImages(images: List<ImageData>, settings: ScreeningSettings) = emptyList<String>()
        }
        val scenario = scenario(rejecting)
        requireNotNull(scenario.model.loadInitialImages()).join()
        assertTrue(scenario.model.state.value.images.isEmpty())
        assertNotNull(scenario.model.state.value.errorMessage)
        assertFalse(scenario.model.state.value.isInitializing)
        assertEquals(30, scenario.requests)
    }

    @Test
    fun settingsChangesDiscardTheOldPrefetchPolicyAndReload() = runTest {
        val scenario = scenario()
        scenario.buffer.urls.addAll(List(100) { "old-policy-$it" })
        requireNotNull(scenario.model.loadInitialImages()).join()
        scenario.model.changeSettings(ScreeningSettings(true, true)).join()
        assertEquals(ScreeningSettings(true, true), scenario.model.settings.value)
        assertTrue(scenario.buffer.cleared)
        assertEquals(30, scenario.model.state.value.images.size)
        assertTrue(scenario.model.state.value.images.all { it.imageURL.startsWith("direct-") })
    }

    @Test
    fun refreshCancelsAnOlderLoadBeforePublishingTheNewGallery() = runTest {
        val scenario = scenario()
        val entered = CompletableDeferred<Unit>()
        scenario.loader.suspended = entered
        requireNotNull(scenario.model.loadInitialImages())
        entered.await()
        scenario.model.refresh().join()
        assertEquals(30, scenario.model.state.value.images.size)
        assertEquals(30, scenario.model.state.value.images.map { it.id }.distinct().size)
        assertNull(scenario.model.state.value.errorMessage)
    }

    @Test
    fun recoveredBackgroundWorkClearsItsWarningWithoutDiscardingTheGallery() = runTest {
        val failures = MutableStateFlow<Throwable?>(null)
        val scenario = scenario(backgroundFailures = failures)
        requireNotNull(scenario.model.loadInitialImages()).join()
        failures.value = IllegalStateException("Replenishment unavailable")
        assertEquals("Replenishment unavailable", scenario.model.state.first { it.warning != null }.warning)
        failures.value = null
        assertEquals(30, scenario.model.state.first { it.warning == null }.images.size)
    }

    private fun TestScope.scenario(screener: Screener = AcceptAll, backgroundFailures: Flow<Throwable?> = emptyFlow()): Scenario {
        val settings = MutableStateFlow(ScreeningSettings(false, false))
        val loader = FakeLoader()
        val buffer = FakeBuffer()
        var requests = 0
        var nextURL = 0
        val repository = object : UrlRepository {
            override suspend fun getNextImageURLs(count: Int): List<String> {
                requests++
                return List(count) { "direct-${nextURL++}" }
            }
        }
        val model = GalleryViewModel(repository, loader, screener, buffer, settings, { settings.value = it }, backgroundFailures, backgroundScope)
        return Scenario(model, loader, buffer) { requests }
    }

    private class Scenario(
        val model: GalleryViewModel,
        val loader: FakeLoader,
        val buffer: FakeBuffer,
        private val countRequests: () -> Int,
    ) {
        val requests get() = countRequests()
    }

    private class FakeLoader : Loader {
        var failNext = false
        var suspended: CompletableDeferred<Unit>? = null
        override suspend fun loadImageData(urls: List<String>): List<ImageData> {
            suspended?.let {
                suspended = null
                it.complete(Unit)
                awaitCancellation()
            }
            if (failNext) {
                failNext = false
                error("Image unavailable")
            }
            return urls.map { ImageData(it, byteArrayOf(42)) }
        }
        override fun clearMemoryCache() = Unit
    }

    private class FakeBuffer : Buffer {
        val urls = ArrayDeque<String>()
        var cleared = false
        var started = false
        override suspend fun getPrefetchedCount() = urls.size
        override suspend fun getPrefetchedImages(imageCount: Int) = List(minOf(imageCount, urls.size)) { urls.removeFirst() }
        override suspend fun startPrefetchingIfNeeded() { started = true }
        override suspend fun clearAllPrefetchedImages() { urls.clear(); cleared = true }
    }

    private object AcceptAll : Screener {
        override suspend fun screenImages(images: List<ImageData>, settings: ScreeningSettings) = images.map { it.imageURL }
    }
}
