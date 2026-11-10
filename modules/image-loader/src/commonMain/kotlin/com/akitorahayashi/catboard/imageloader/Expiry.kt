@file:OptIn(coil3.annotation.ExperimentalCoilApi::class)

package com.akitorahayashi.catboard.imageloader

import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options
import kotlin.time.Clock

internal class Expiry(private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }) : CacheStrategy {
    override suspend fun read(cacheResponse: NetworkResponse, networkRequest: NetworkRequest, options: Options): CacheStrategy.ReadResult =
        if (now() - cacheResponse.responseMillis >= 3L * 24 * 60 * 60 * 1000) {
            CacheStrategy.ReadResult(networkRequest)
        } else {
            CacheStrategy.DEFAULT.read(cacheResponse, networkRequest, options)
        }

    override suspend fun write(cacheResponse: NetworkResponse?, networkRequest: NetworkRequest, networkResponse: NetworkResponse, options: Options): CacheStrategy.WriteResult =
        CacheStrategy.DEFAULT.write(cacheResponse, networkRequest, networkResponse, options)
}
