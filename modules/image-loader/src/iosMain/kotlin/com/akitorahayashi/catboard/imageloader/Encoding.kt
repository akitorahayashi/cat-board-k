@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.akitorahayashi.catboard.imageloader

import coil3.Image
import coil3.toBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import platform.Foundation.NSData
import platform.Foundation.create

fun ImageData.asData(): NSData = if (bytes.isEmpty()) {
    NSData()
} else {
    bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
}

internal actual fun encode(image: Image): ByteArray {
    val raster = org.jetbrains.skia.Image.makeFromBitmap(image.toBitmap())
    try {
        val data = checkNotNull(raster.encodeToData(EncodedImageFormat.JPEG, 80)) { "Image encoding failed" }
        try {
            return data.bytes
        } finally {
            data.close()
        }
    } finally {
        raster.close()
    }
}
