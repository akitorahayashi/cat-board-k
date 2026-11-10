package com.akitorahayashi.catboard.catapi

interface ImageSource {
    suspend fun fetchImageURLs(totalCount: Int, batchSize: Int = 10): List<String>
}
