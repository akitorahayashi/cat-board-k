@file:OptIn(coil3.annotation.ExperimentalCoilApi::class)

package com.akitorahayashi.catboard.imageloader

import coil3.PlatformContext
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class ExpiryTest {
    @Test
    fun diskImagesAreRevalidatedAfterSeventyTwoHours() = runTest {
        var now = 1000L
        val expiry = Expiry { now }
        val cached = NetworkResponse(responseMillis = now)
        val request = NetworkRequest("https://cats.example/image.jpg")
        val options = Options(PlatformContext.INSTANCE)
        now += 71.hours.inWholeMilliseconds
        assertEquals(cached, expiry.read(cached, request, options).response)
        now += 1.hours.inWholeMilliseconds
        assertEquals(request, expiry.read(cached, request, options).request)
    }
}
