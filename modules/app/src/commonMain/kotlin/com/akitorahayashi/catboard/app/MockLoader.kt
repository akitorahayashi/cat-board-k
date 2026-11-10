package com.akitorahayashi.catboard.app

import com.akitorahayashi.catboard.imageloader.ImageData
import com.akitorahayashi.catboard.imageloader.Loader

internal class MockLoader(private val image: ByteArray, private var failNext: Boolean) : Loader {
    override suspend fun loadImageData(urls: List<String>): List<ImageData> {
        if (failNext) {
            failNext = false
            error("テスト用の画像読み込みエラー")
        }
        return urls.map { ImageData(it, image) }
    }
    override fun clearMemoryCache() = Unit
}
