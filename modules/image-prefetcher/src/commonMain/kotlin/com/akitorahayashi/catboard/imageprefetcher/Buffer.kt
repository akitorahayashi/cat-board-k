package com.akitorahayashi.catboard.imageprefetcher

interface Buffer {
    suspend fun getPrefetchedCount(): Int
    suspend fun getPrefetchedImages(imageCount: Int): List<String>
    suspend fun startPrefetchingIfNeeded()
    suspend fun clearAllPrefetchedImages()
}
