package com.akitorahayashi.catboard.imagestore

import com.akitorahayashi.catboard.catapi.ImageSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class Repository(
    private val database: UrlDatabase,
    private val api: ImageSource,
    private val scope: CoroutineScope,
) : UrlRepository {
    private val consumption = Mutex()
    private var refillJob: Job? = null
    private val failure = MutableStateFlow<Throwable?>(null)
    val refillFailure = failure.asStateFlow()

    override suspend fun getNextImageURLs(count: Int): List<String> {
        require(count >= 0) { "count must not be negative" }
        if (count == 0) return emptyList()

        return consumption.withLock {
            val cached = database.urls().take(count)
            val result = try {
                val missing = count - cached.size
                cached.map { it.imageURL } + if (missing > 0) api.fetchImageURLs(missing) else emptyList()
            } catch (error: Throwable) {
                withContext(NonCancellable) { database.urls().insert(cached) }
                throw error
            }
            if (database.urls().count() <= 100) startRefilling()
            result
        }
    }

    private fun startRefilling() {
        if (refillJob?.isActive == true) return
        refillJob = scope.launch {
            try {
                val needed = 300 - database.urls().count()
                if (needed > 0) {
                    val images = api.fetchImageURLs(needed)
                    consumption.withLock {
                        val available = (300 - database.urls().count()).coerceAtLeast(0)
                        database.urls().insert(images.take(available).map { StoredCatImageURL(it) })
                    }
                }
                failure.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                failure.value = error
            }
        }
    }
}
