package com.akitorahayashi.catboard.imagescreener

import com.akitorahayashi.catboard.imageloader.ImageData

interface Screener {
    @Throws(Exception::class)
    suspend fun screenImages(images: List<ImageData>, settings: ScreeningSettings): List<String>
}
