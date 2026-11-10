package com.akitorahayashi.catboard.catapi

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class Client(private val engine: HttpClientEngine) : ImageSource, AutoCloseable {
    private val http = HttpClient(engine) {
        expectSuccess = false
        followRedirects = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun fetchImageURLs(totalCount: Int, batchSize: Int): List<String> {
        require(totalCount >= 0) { "totalCount must not be negative" }
        require(batchSize > 0) { "batchSize must be positive" }
        if (totalCount == 0) return emptyList()

        val result = mutableListOf<String>()
        val pageCount = (totalCount - 1) / batchSize + 1
        for (page in 0 until pageCount) {
            val response = http.get("https://api.thecatapi.com/v1/images/search") {
                parameter("limit", batchSize)
                parameter("page", page)
                parameter("order", "Rand")
            }
            if (response.status != HttpStatusCode.OK) {
                throw HttpFailure(response.status.value)
            }
            for (image in response.body<List<ImageResponse>>()) {
                val url = Url(image.url)
                require(url.host.isNotBlank() && url.protocol in setOf(URLProtocol.HTTP, URLProtocol.HTTPS)) {
                    "The Cat API returned an invalid image URL"
                }
                result += image.url
                if (result.size == totalCount) return result
            }
        }
        return result
    }

    override fun close() {
        http.close()
        engine.close()
    }

    @Serializable
    private data class ImageResponse(val url: String)
}
