@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.akitorahayashi.catboard.imageprefetcher

import com.akitorahayashi.catboard.imageloader.ImageData
import com.akitorahayashi.catboard.imageloader.Loader
import com.akitorahayashi.catboard.imagescreener.Screener
import com.akitorahayashi.catboard.imagescreener.ScreeningSettings
import com.akitorahayashi.catboard.imagestore.UrlRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrefetcherTest {
    @Test
    fun fillsTheBufferConsumesInOrderAndRestartsAfterConsumption() = runTest {
        withPrefetcher {
            assertEquals(0, prefetcher.getPrefetchedCount())
            prefetcher.startPrefetchingIfNeeded()
            prefetcher.isPrefetching.first { !it }
            assertEquals(180, prefetcher.getPrefetchedCount())
            assertEquals(listOf("image-0", "image-1", "image-2", "image-3", "image-4"), prefetcher.getPrefetchedImages(5))
            assertEquals(175, prefetcher.getPrefetchedCount())
            prefetcher.startPrefetchingIfNeeded()
            prefetcher.isPrefetching.first { !it }
            assertEquals(180, prefetcher.getPrefetchedCount())
            assertNull(prefetcher.prefetchFailure.value)
        }
    }

    @Test
    fun concurrentStartsRespectTheBufferLimit() = runTest {
        withPrefetcher {
            listOf(async { prefetcher.startPrefetchingIfNeeded() }, async { prefetcher.startPrefetchingIfNeeded() }).awaitAll()
            prefetcher.isPrefetching.first { !it }
            assertEquals(180, prefetcher.getPrefetchedCount())
            assertEquals(180, prefetcher.getPrefetchedImages(200).size)
        }
    }

    @Test
    fun storesOnlyImagesAcceptedByTheScreenerAndBoundsAttempts() = runTest {
        val filtering = object : Screener {
            override suspend fun screenImages(images: List<ImageData>, settings: ScreeningSettings) = images.take(5).map { it.imageURL }
        }
        withPrefetcher(filtering) {
            prefetcher.startPrefetchingIfNeeded()
            prefetcher.isPrefetching.first { !it }
            assertEquals(150, prefetcher.getPrefetchedCount())
            val images = prefetcher.getPrefetchedImages(180)
            assertTrue("image-0" in images)
            assertFalse("image-5" in images)
            assertNotNull(prefetcher.prefetchFailure.value)
        }
    }

    @Test
    fun changedSettingsCannotReuseImagesFromThePreviousPolicy() = runTest {
        withPrefetcher {
            prefetcher.startPrefetchingIfNeeded()
            prefetcher.isPrefetching.first { !it }
            settings.value = ScreeningSettings(true, false)
            assertEquals(0, prefetcher.getPrefetchedCount())
            assertTrue(prefetcher.getPrefetchedImages(1).isEmpty())
            prefetcher.clearAllPrefetchedImages()
            assertTrue(loader.cleared)
            settings.value = ScreeningSettings(false, false)
            assertEquals(0, prefetcher.getPrefetchedCount())
        }
    }

    @Test
    fun clearCancelsInFlightScreeningBeforeRemovingTheQueue() = runTest {
        val entered = CompletableDeferred<Unit>()
        val suspended = object : Screener {
            override suspend fun screenImages(images: List<ImageData>, settings: ScreeningSettings): List<String> {
                entered.complete(Unit)
                awaitCancellation()
            }
        }
        withPrefetcher(suspended) {
            prefetcher.startPrefetchingIfNeeded()
            entered.await()
            prefetcher.clearAllPrefetchedImages()
            assertFalse(prefetcher.isPrefetching.value)
            assertEquals(0, prefetcher.getPrefetchedCount())
            assertTrue(loader.cleared)
        }
    }

    @Test
    fun loadingFailuresAreReportedWithoutCachingUnscreenedImages() = runTest {
        withPrefetcher {
            loader.failure = IllegalStateException("Image unavailable")
            prefetcher.startPrefetchingIfNeeded()
            prefetcher.isPrefetching.first { !it }
            assertEquals(0, prefetcher.getPrefetchedCount())
            assertEquals(loader.failure, prefetcher.prefetchFailure.value)
        }
    }

    @Test
    fun prefetchedImagesSurviveDatabaseReopening() = runTest {
        withPrefetcher {
            database.queue().insert(listOf(PrefetchedCatImageURL("stored", false, false)))
            database.close()
            val reopened = createDatabase(path)
            try {
                assertEquals(listOf("stored"), reopened.queue().take(1, false, false))
                assertTrue(reopened.queue().take(1, false, false).isEmpty())
            } finally {
                reopened.close()
            }
        }
    }

    private suspend fun TestScope.withPrefetcher(screener: Screener = AcceptAll, block: suspend Scenario.() -> Unit) {
        val manager = NSFileManager.defaultManager
        val directory = NSTemporaryDirectory() + NSUUID().UUIDString
        check(manager.createDirectoryAtPath(directory, false, null, null))
        var database: PrefetchDatabase? = null
        try {
            val path = "$directory/prefetch.db"
            database = createDatabase(path)
            val settings = MutableStateFlow(ScreeningSettings(false, false))
            val loader = FakeLoader()
            val repository = object : UrlRepository {
                private var next = 0
                override suspend fun getNextImageURLs(count: Int) = List(count) { "image-${next++}" }
            }
            val prefetcher = Prefetcher(repository, loader, screener, database, settings, backgroundScope)
            val scenario = Scenario(prefetcher, settings, loader, database, path)
            withContext(Dispatchers.Default) { withTimeout(10_000) { scenario.block() } }
        } finally {
            backgroundScope.coroutineContext[Job]!!.children.toList().forEach { it.cancelAndJoin() }
            database?.close()
            check(manager.removeItemAtPath(directory, null))
        }
    }

    private class Scenario(
        val prefetcher: Prefetcher,
        val settings: MutableStateFlow<ScreeningSettings>,
        val loader: FakeLoader,
        val database: PrefetchDatabase,
        val path: String,
    )

    private class FakeLoader : Loader {
        var cleared = false
        var failure: Exception? = null
        override suspend fun loadImageData(urls: List<String>): List<ImageData> {
            failure?.let { throw it }
            return urls.map { ImageData(it, byteArrayOf(42)) }
        }
        override fun clearMemoryCache() { cleared = true }
    }

    private object AcceptAll : Screener {
        override suspend fun screenImages(images: List<ImageData>, settings: ScreeningSettings) = images.map { it.imageURL }
    }
}
