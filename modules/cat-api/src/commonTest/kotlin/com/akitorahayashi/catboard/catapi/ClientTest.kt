package com.akitorahayashi.catboard.catapi

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClientTest {
    @Test
    fun pagesAndStopsAtRequestedCount() = runTest {
        val pages = mutableListOf<String>()
        val engine = MockEngine { request ->
            assertEquals("https", request.url.protocol.name)
            assertEquals("api.thecatapi.com", request.url.host)
            assertEquals("/v1/images/search", request.url.encodedPath)
            assertEquals("2", request.url.parameters["limit"])
            assertEquals("Rand", request.url.parameters["order"])
            val page = requireNotNull(request.url.parameters["page"])
            pages += page
            val content = when (page) {
                "0" -> """[{"id":"a","url":"https://cats.example/a.jpg"},{"url":"https://cats.example/b.jpg"}]"""
                "1" -> """[{"url":"https://cats.example/c.jpg"},{"url":"https://cats.example/d.jpg"}]"""
                else -> error("Unexpected page: $page")
            }
            respond(content, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        Client(engine).use { client ->
            assertEquals(
                listOf("https://cats.example/a.jpg", "https://cats.example/b.jpg", "https://cats.example/c.jpg"),
                client.fetchImageURLs(3, 2),
            )
        }
        assertEquals(listOf("0", "1"), pages)
    }

    @Test
    fun rejectsEveryNon200ResponseIncludingRedirects() = runTest {
        for (status in listOf(201, 204, 301, 400, 429, 500)) {
            val engine = MockEngine {
                respond("[]", HttpStatusCode.fromValue(status))
            }
            Client(engine).use { client ->
                val error = assertFailsWith<HttpFailure> { client.fetchImageURLs(1) }
                assertEquals(status, error.statusCode)
            }
        }
    }

    @Test
    fun returnsEmptyWithoutRequestAndValidatesArguments() = runTest {
        val engine = MockEngine { error("No request expected") }
        Client(engine).use { client ->
            assertTrue(client.fetchImageURLs(0).isEmpty())
            assertFailsWith<IllegalArgumentException> { client.fetchImageURLs(-1) }
            assertFailsWith<IllegalArgumentException> { client.fetchImageURLs(1, 0) }
        }
    }

    @Test
    fun exposesInvalidResponses() = runTest {
        val engine = MockEngine {
            respond("""[{"id":"missing-url"}]""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        Client(engine).use { client ->
            assertFails { client.fetchImageURLs(1) }
        }
    }

    @Test
    fun emptyPagesDoNotCauseUnboundedRequests() = runTest {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        Client(engine).use { client ->
            assertTrue(client.fetchImageURLs(3, 2).isEmpty())
        }
        assertEquals(2, requests)
    }
}
