package com.akitorahayashi.catboard.app

import com.akitorahayashi.catboard.imageloader.ImageData
import com.akitorahayashi.catboard.imageloader.Loader
import com.akitorahayashi.catboard.imageprefetcher.Buffer
import com.akitorahayashi.catboard.imagescreener.ScreeningSettings
import com.akitorahayashi.catboard.imagestore.UrlRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GallerySessionTest {
    @Test
    fun closingOneScenePreservesOtherScenesAndClosingTheRuntimeStopsThem() = runTest {
        val runtime = runtime(MockLoader(byteArrayOf(42), false))
        val first = runtime.createSession()
        val second = runtime.createSession()
        first.close()
        second.model.refresh().join()
        assertEquals(30, second.state.images.size)
        runtime.close()
        var delivered = false
        second.observeState { delivered = true }
        runCurrent()
        assertEquals(false, delivered)
        assertFailsWith<IllegalStateException> { runtime.createSession() }
    }

    @Test
    fun imageRequestsReportFailuresAndCanRecoverWithImageData() = runTest {
        val session = session(MockLoader(byteArrayOf(42), true))
        var image: ImageData? = null
        var error: String? = null
        session.loadImage("https://cats.example/image", { image = it }, { error = it })
        runCurrent()
        assertNull(image)
        assertNotNull(error)
        error = null
        session.loadImage("https://cats.example/image", { image = it }, { error = it })
        runCurrent()
        assertEquals("https://cats.example/image", assertNotNull(image).imageURL)
        assertContentEquals(byteArrayOf(42), assertNotNull(image).bytes)
        assertNull(error)
        session.close()
    }

    @Test
    fun subscribersReceiveStateAndCanStopObserving() = runTest {
        val session = session(MockLoader(byteArrayOf(42), false))
        val states = mutableListOf<GalleryState>()
        val observation = session.observeState { states += it }
        runCurrent()
        assertTrue(states.last().images.isEmpty())
        session.model.refresh().join()
        runCurrent()
        assertEquals(30, states.last().images.size)
        observation.cancel()
        val count = states.size
        session.model.refresh().join()
        runCurrent()
        assertEquals(count, states.size)
        session.close()
    }

    @Test
    fun settingsChangesArePublishedThroughThePlatformBoundary() = runTest {
        val session = session(MockLoader(byteArrayOf(42), false))
        val settings = mutableListOf<ScreeningSettings>()
        session.observeSettings { settings += it }
        runCurrent()
        session.changeSettings(true, true)
        runCurrent()
        assertEquals(ScreeningSettings(true, true), settings.last())
        session.close()
    }

    @Test
    fun closingTheSessionCancelsImageRequestsWithoutDeliveringAnError() = runTest {
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val loader = object : Loader {
            override suspend fun loadImageData(urls: List<String>): List<ImageData> {
                entered.complete(Unit)
                try { awaitCancellation() } finally { cancelled.complete(Unit) }
            }
            override fun clearMemoryCache() = Unit
        }
        val session = session(loader)
        var delivered = false
        session.loadImage("https://cats.example/image", { delivered = true }, { delivered = true })
        entered.await()
        session.close()
        cancelled.await()
        assertEquals(false, delivered)
    }

    private fun TestScope.session(loader: Loader) = runtime(loader).createSession()

    private fun TestScope.runtime(loader: Loader): GalleryRuntime {
        val scope = CoroutineScope(backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job]))
        val settings = MutableStateFlow(ScreeningSettings(false, false))
        val source = MockImageSource()
        val repository = object : UrlRepository {
            override suspend fun getNextImageURLs(count: Int) = source.fetchImageURLs(count)
        }
        val buffer = object : Buffer {
            override suspend fun getPrefetchedCount() = 0
            override suspend fun getPrefetchedImages(imageCount: Int) = emptyList<String>()
            override suspend fun startPrefetchingIfNeeded() = Unit
            override suspend fun clearAllPrefetchedImages() = Unit
        }
        return GalleryRuntime(scope, loader) { sessionScope ->
            GalleryViewModel(repository, loader, MockScreener(), buffer, settings, { settings.value = it }, emptyFlow(), sessionScope)
        }
    }
}
