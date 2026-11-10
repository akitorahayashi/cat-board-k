package com.akitorahayashi.catboard.app

import com.akitorahayashi.catboard.imageloader.ImageData
import com.akitorahayashi.catboard.imagescreener.Screener
import com.akitorahayashi.catboard.imagescreener.ScreeningSettings

internal class MockScreener : Screener {
    override suspend fun screenImages(images: List<ImageData>, settings: ScreeningSettings): List<String> =
        images.filterIndexed { index, _ -> !settings.isScreeningEnabled || (index % 2 == 0) != settings.scaryMode }.map { it.imageURL }
}
