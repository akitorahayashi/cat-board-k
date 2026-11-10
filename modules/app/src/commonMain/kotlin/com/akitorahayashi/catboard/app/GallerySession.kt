@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package com.akitorahayashi.catboard.app

import com.akitorahayashi.catboard.imageloader.ImageData
import com.akitorahayashi.catboard.imageloader.Loader
import com.akitorahayashi.catboard.imagescreener.ScreeningSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GallerySession internal constructor(
    @property:kotlin.native.HiddenFromObjC val model: GalleryViewModel,
    private val loader: Loader,
    private val scope: CoroutineScope,
) {
    val state: GalleryState get() = model.state.value
    val settings: ScreeningSettings get() = model.settings.value

    fun observeState(onState: (GalleryState) -> Unit): Observation = Observation(
        scope.launch { model.state.collect(onState) },
    )

    fun observeSettings(onSettings: (ScreeningSettings) -> Unit): Observation = Observation(
        scope.launch { model.settings.collect(onSettings) },
    )

    fun loadInitialImages() { model.loadInitialImages() }
    fun refresh() { model.refresh() }
    fun fetchAdditionalImages() { model.fetchAdditionalImages() }
    fun changeSettings(isScreeningEnabled: Boolean, scaryMode: Boolean) {
        model.changeSettings(ScreeningSettings(isScreeningEnabled, scaryMode))
    }

    fun loadImage(imageURL: String, onImage: (ImageData) -> Unit, onError: (String) -> Unit): Observation = Observation(
        scope.launch {
            try {
                onImage(loader.loadImageData(listOf(imageURL)).single())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onError(error.message ?: error.toString())
            }
        },
    )

    fun close() { scope.cancel() }
}
