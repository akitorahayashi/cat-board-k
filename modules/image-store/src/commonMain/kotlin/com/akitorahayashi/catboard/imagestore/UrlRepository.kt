package com.akitorahayashi.catboard.imagestore

interface UrlRepository {
    suspend fun getNextImageURLs(count: Int): List<String>
}
