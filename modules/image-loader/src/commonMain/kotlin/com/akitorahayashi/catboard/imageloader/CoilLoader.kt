@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package com.akitorahayashi.catboard.imageloader

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.Path.Companion.toPath

@kotlin.native.HiddenFromObjC
class CoilLoader internal constructor(
    val images: ImageLoader,
    private val context: PlatformContext,
    private val http: HttpClient,
) : Loader, AutoCloseable {
    private val downloads = Semaphore(4)

    override suspend fun loadImageData(urls: List<String>): List<ImageData> = coroutineScope {
        urls.map { url ->
            async {
                downloads.withPermit {
                    val request = ImageRequest.Builder(context).data(url).build()
                    when (val result = images.execute(request)) {
                        is ErrorResult -> throw result.throwable
                        is SuccessResult -> ImageData(url, encode(result.image))
                    }
                }
            }
        }.awaitAll()
    }

    override fun clearMemoryCache() {
        images.memoryCache?.clear()
    }

    override fun close() {
        images.shutdown()
        images.diskCache?.shutdown()
        http.close()
    }
}

@OptIn(coil3.annotation.ExperimentalCoilApi::class)
@kotlin.native.HiddenFromObjC
fun createLoader(context: PlatformContext, cacheDirectory: String, http: HttpClient): CoilLoader {
    require(cacheDirectory.startsWith('/')) { "The image cache needs an absolute path" }
    val images = ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder().maxSizeBytes(200L * 1024 * 1024).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDirectory.toPath())
                .maxSizeBytes(500L * 1024 * 1024)
                .build()
        }
        .components {
            add(KtorNetworkFetcherFactory(httpClient = { http }, cacheStrategy = { Expiry() }))
        }
        .build()
    return CoilLoader(images, context, http)
}
