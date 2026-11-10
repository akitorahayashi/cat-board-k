@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.akitorahayashi.catboard.imagestore

import com.akitorahayashi.catboard.catapi.ImageSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RepositoryTest {
    @Test
    fun consumesStoredURLsInOrderWithoutFetchingCachedItems() = runTest {
        withDatabase { database, _ ->
            val urls = listOf("a", "b", "c") + List(101) { "unused-$it" }
            database.urls().insert(urls.map { StoredCatImageURL(it) })
            val source = object : ImageSource {
                override suspend fun fetchImageURLs(totalCount: Int, batchSize: Int): List<String> {
                    error("Stored URLs must be consumed before an API request")
                }
            }
            val repository = Repository(database, source, backgroundScope)
            assertEquals(listOf("a", "b"), repository.getNextImageURLs(2))
            assertEquals(listOf("c"), repository.getNextImageURLs(1))
        }
    }

    @Test
    fun fillsOnlyTheMissingCountFromTheAPI() = runTest {
        withDatabase { database, _ ->
            database.urls().insert(listOf(StoredCatImageURL("cached")))
            val source = object : ImageSource {
                override suspend fun fetchImageURLs(totalCount: Int, batchSize: Int): List<String> {
                    if (totalCount == 300) awaitCancellation()
                    assertEquals(2, totalCount)
                    return listOf("remote-a", "remote-b")
                }
            }
            val repository = Repository(database, source, backgroundScope)
            assertEquals(listOf("cached", "remote-a", "remote-b"), repository.getNextImageURLs(3))
        }
    }

    @Test
    fun preservesCachedURLsWhenTheAPIRequestFails() = runTest {
        withDatabase { database, _ ->
            database.urls().insert(listOf(StoredCatImageURL("cached")))
            val source = object : ImageSource {
                override suspend fun fetchImageURLs(totalCount: Int, batchSize: Int): List<String> =
                    throw IllegalStateException("API unavailable")
            }
            val repository = Repository(database, source, backgroundScope)
            assertFailsWith<IllegalStateException> { repository.getNextImageURLs(2) }
            assertEquals(listOf("cached"), database.urls().take(1).map { it.imageURL })
        }
    }

    @Test
    fun concurrentConsumersReceiveEachStoredItemOnce() = runTest {
        withDatabase { database, _ ->
            database.urls().insert(listOf("a", "b", "c", "d").map { StoredCatImageURL(it) })
            val source = object : ImageSource {
                override suspend fun fetchImageURLs(totalCount: Int, batchSize: Int) = emptyList<String>()
            }
            val repository = Repository(database, source, backgroundScope)
            val result = listOf(async { repository.getNextImageURLs(2) }, async { repository.getNextImageURLs(2) })
                .awaitAll().flatten()
            assertEquals(4, result.size)
            assertEquals(setOf("a", "b", "c", "d"), result.toSet())
        }
    }

    @Test
    fun storedURLsSurviveReopeningTheDatabase() = runTest {
        withDatabase { database, path ->
            database.urls().insert(listOf(StoredCatImageURL("persisted")))
            database.close()
            val reopened = createDatabase(path)
            try {
                assertEquals(listOf("persisted"), reopened.urls().take(1).map { it.imageURL })
                assertEquals(emptyList(), reopened.urls().take(1))
            } finally {
                reopened.close()
            }
        }
    }

    @Test
    fun lowCacheStartsRefillAndSurfacesBackgroundFailures() = runTest {
        withDatabase { database, _ ->
            val refilled = CompletableDeferred<Int>()
            val failure = IllegalStateException("Refill unavailable")
            val source = object : ImageSource {
                override suspend fun fetchImageURLs(totalCount: Int, batchSize: Int): List<String> {
                    if (totalCount == 1) return listOf("first")
                    refilled.complete(totalCount)
                    throw failure
                }
            }
            val repository = Repository(database, source, backgroundScope)
            assertEquals(listOf("first"), repository.getNextImageURLs(1))
            assertEquals(300, refilled.await())
            assertEquals(failure, repository.refillFailure.filterNotNull().first())
        }
    }

    @Test
    fun lowCacheIsRefilledForTheNextConsumer() = runTest {
        withDatabase { database, _ ->
            val source = object : ImageSource {
                override suspend fun fetchImageURLs(totalCount: Int, batchSize: Int): List<String> =
                    if (totalCount == 1) listOf("first") else listOf("refilled-a", "refilled-b")
            }
            val repository = Repository(database, source, backgroundScope)
            assertEquals(listOf("first"), repository.getNextImageURLs(1))
            while (database.urls().count() < 2) delay(10)
            val next = Repository(database, source, backgroundScope)
            assertEquals(listOf("refilled-a"), next.getNextImageURLs(1))
        }
    }

    @Test
    fun cancellationRestoresTheReservedCacheBeforeCompleting() = runTest {
        withDatabase { database, _ ->
            database.urls().insert(listOf("first", "second").map { StoredCatImageURL(it) })
            val entered = CompletableDeferred<Unit>()
            val source = object : ImageSource {
                override suspend fun fetchImageURLs(totalCount: Int, batchSize: Int): List<String> {
                    entered.complete(Unit)
                    awaitCancellation()
                }
            }
            val repository = Repository(database, source, backgroundScope)
            val request = async { repository.getNextImageURLs(3) }
            entered.await()
            request.cancelAndJoin()
            assertEquals(listOf("first", "second"), database.urls().take(2).map { it.imageURL })
        }
    }

    private suspend fun TestScope.withDatabase(block: suspend (UrlDatabase, String) -> Unit) {
        val manager = NSFileManager.defaultManager
        val directory = NSTemporaryDirectory() + NSUUID().UUIDString
        check(manager.createDirectoryAtPath(directory, false, null, null))
        var database: UrlDatabase? = null
        try {
            val path = "$directory/urls.db"
            database = createDatabase(path)
            withContext(Dispatchers.Default) { withTimeout(10_000) { block(database, path) } }
        } finally {
            backgroundScope.coroutineContext[Job]!!.children.toList().forEach { it.cancelAndJoin() }
            database?.close()
            check(manager.removeItemAtPath(directory, null))
        }
    }
}
