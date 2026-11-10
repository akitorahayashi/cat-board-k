@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.akitorahayashi.catboard.imageloader

import coil3.PlatformContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.UIKit.UIImage
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull

class LoaderTest {
    @Test
    fun returnsImagesInInputOrderWithDataReadableByUIKit() = runTest {
        val png = Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aP9sAAAAASUVORK5CYII=")
        val http = HttpClient(MockEngine) {
            engine { addHandler { respond(png, headers = headersOf(HttpHeaders.ContentType, "image/png")) } }
        }
        withLoader(http) { loader ->
            val urls = listOf("https://cats.example/first.png", "https://cats.example/second.png")
            val result = loader.loadImageData(urls)
            assertEquals(urls, result.map { it.imageURL })
            for (image in result) {
                val data = image.asData()
                assertEquals(image.bytes.size.toULong(), data.length)
                assertNotNull(UIImage(data = data))
            }
        }
    }

    @Test
    fun failedDownloadsRemainVisibleToTheCaller() = runTest {
        val http = HttpClient(MockEngine) {
            engine { addHandler { respond("Unavailable", HttpStatusCode.ServiceUnavailable) } }
        }
        withLoader(http) { loader ->
            assertFails { loader.loadImageData(listOf("https://cats.example/unavailable.png")) }
        }
    }

    private suspend fun withLoader(http: HttpClient, block: suspend (CoilLoader) -> Unit) {
        val manager = NSFileManager.defaultManager
        val directory = NSTemporaryDirectory() + NSUUID().UUIDString
        check(manager.createDirectoryAtPath(directory, false, null, null))
        var loader: CoilLoader? = null
        try {
            loader = createLoader(PlatformContext.INSTANCE, directory, http)
            withContext(Dispatchers.Default) { withTimeout(10_000) { block(loader) } }
        } finally {
            loader?.close()
            http.close()
            check(manager.removeItemAtPath(directory, null))
        }
    }
}
