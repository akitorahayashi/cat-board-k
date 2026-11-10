@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.akitorahayashi.catboard.app

import com.akitorahayashi.catboard.catapi.createClient
import com.akitorahayashi.catboard.imageloader.createLoader
import com.akitorahayashi.catboard.imageprefetcher.createDatabase as createPrefetchDatabase
import com.akitorahayashi.catboard.imagescreener.Screener
import com.akitorahayashi.catboard.imagescreener.ScreeningSettings
import com.akitorahayashi.catboard.imagestore.createDatabase as createUrlDatabase
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

fun createGalleryRuntime(screener: Screener, defaultScreeningEnabled: Boolean): GalleryRuntime {
    val manager = NSFileManager.defaultManager
    val directory = manager.URLForDirectory(NSCachesDirectory, NSUserDomainMask, null, true, null)
    val cache = requireNotNull(directory?.path) + "/cat-board-k"
    check(manager.createDirectoryAtPath(cache, true, null, null))
    val database = createUrlDatabase("$cache/urls.db")
    val prefetched = createPrefetchDatabase("$cache/prefetch.db")
    val images = createLoader("$cache/images")
    val api = createClient()
    return createRuntime(database, prefetched, images, api, screener, Preferences(defaultScreeningEnabled, NSUserDefaults.standardUserDefaults)) {
        images.close()
        api.close()
    }
}

fun createTestGalleryRuntime(image: NSData, simulateError: Boolean): GalleryRuntime {
    val cache = NSTemporaryDirectory() + NSUUID().UUIDString
    val manager = NSFileManager.defaultManager
    check(manager.createDirectoryAtPath(cache, true, null, null))
    val bytes = requireNotNull(image.bytes).reinterpret<ByteVar>().readBytes(image.length.toInt())
    val settings = object : SettingsStore {
        override val settings = MutableStateFlow(ScreeningSettings(false, false))
        override suspend fun save(value: ScreeningSettings) { settings.value = value }
    }
    return createRuntime(
        createUrlDatabase("$cache/urls.db"), createPrefetchDatabase("$cache/prefetch.db"),
        MockLoader(bytes, simulateError), MockImageSource(), MockScreener(), settings,
    ) { check(manager.removeItemAtPath(cache, null)) }
}
