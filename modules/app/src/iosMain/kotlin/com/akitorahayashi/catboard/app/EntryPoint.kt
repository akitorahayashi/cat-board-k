@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.akitorahayashi.catboard.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akitorahayashi.catboard.catapi.createClient
import com.akitorahayashi.catboard.imageloader.createLoader
import com.akitorahayashi.catboard.imageprefetcher.Prefetcher
import com.akitorahayashi.catboard.imageprefetcher.createDatabase as createPrefetchDatabase
import com.akitorahayashi.catboard.imagescreener.Screener
import com.akitorahayashi.catboard.imagescreener.ScreeningSettings
import com.akitorahayashi.catboard.imagestore.Repository
import com.akitorahayashi.catboard.imagestore.createDatabase as createUrlDatabase
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation

fun galleryViewController(screener: Screener, defaultScreeningEnabled: Boolean) = run {
    val arguments = NSProcessInfo.processInfo.arguments
    val testing = "--uitesting" in arguments
    val manager = NSFileManager.defaultManager
    val cache = if (testing) {
        NSTemporaryDirectory() + NSUUID().UUIDString
    } else {
        val directory = manager.URLForDirectory(NSCachesDirectory, NSUserDomainMask, null, true, null)
        requireNotNull(directory?.path) + "/cat-board-k"
    }
    check(manager.createDirectoryAtPath(cache, true, null, null))
    val defaults = NSUserDefaults.standardUserDefaults
    val settings = MutableStateFlow(
        if (testing) ScreeningSettings(false, false) else ScreeningSettings(
            if (defaults.objectForKey("isScreeningEnabled") == null) defaultScreeningEnabled else defaults.boolForKey("isScreeningEnabled"),
            defaults.boolForKey("scaryMode"),
        ),
    )
    val job = SupervisorJob()
    val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    val database = createUrlDatabase("$cache/urls.db")
    val prefetched = createPrefetchDatabase("$cache/prefetch.db")
    val images = createLoader("$cache/images")
    val api = if (testing) null else createClient()
    val repository = Repository(database, if (testing) MockImageSource() else requireNotNull(api), scope)
    val fixture = if (testing) sampleImage() else null
    val loader = if (testing) MockLoader(requireNotNull(fixture), "--simulate-error" in arguments) else images
    val selectedScreener = if (testing) MockScreener() else screener
    val prefetcher = Prefetcher(repository, loader, selectedScreener, prefetched, settings, scope)
    val failures = combine(repository.refillFailure, prefetcher.prefetchFailure) { refill, prefetch -> refill ?: prefetch }
    val imageModel: (String) -> Any = if (testing) { { requireNotNull(fixture) } } else { { it } }

    job.invokeOnCompletion {
        database.close()
        prefetched.close()
        images.close()
        api?.close()
        if (testing) check(manager.removeItemAtPath(cache, null))
    }
    ComposeUIViewController {
        val model = viewModel {
            GalleryViewModel(repository, loader, selectedScreener, prefetcher, settings, { value ->
                if (!testing) {
                    defaults.setBool(value.isScreeningEnabled, "isScreeningEnabled")
                    defaults.setBool(value.scaryMode, "scaryMode")
                }
                settings.value = value
            }, failures, scope)
        }
        DisposableEffect(Unit) { onDispose { job.cancel() } }
        Gallery(model, images.images, imageModel)
    }
}

private fun sampleImage(): ByteArray {
    val image = requireNotNull(UIImage.imageNamed("SampleUITestImage")) { "The UI testing image is missing" }
    val data = requireNotNull(UIImagePNGRepresentation(image)) { "The UI testing image could not be encoded" }
    return requireNotNull(data.bytes).reinterpret<ByteVar>().readBytes(data.length.toInt())
}
