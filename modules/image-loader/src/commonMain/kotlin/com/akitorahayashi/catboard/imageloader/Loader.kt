@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package com.akitorahayashi.catboard.imageloader

@kotlin.native.HiddenFromObjC
interface Loader {
    suspend fun loadImageData(urls: List<String>): List<ImageData>
    fun clearMemoryCache()
}
