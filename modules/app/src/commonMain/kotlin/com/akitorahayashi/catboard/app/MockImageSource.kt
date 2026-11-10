package com.akitorahayashi.catboard.app

import com.akitorahayashi.catboard.catapi.ImageSource

internal class MockImageSource : ImageSource {
    private var next = 0
    override suspend fun fetchImageURLs(totalCount: Int, batchSize: Int) = List(totalCount) { "https://uitesting.invalid/${next++}.jpg" }
}
