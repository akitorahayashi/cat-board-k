package com.akitorahayashi.catboard.imageloader

import android.graphics.Bitmap
import coil3.Image
import coil3.toBitmap
import java.io.ByteArrayOutputStream

internal actual fun encode(image: Image): ByteArray = ByteArrayOutputStream().use { output ->
    check(image.toBitmap().compress(Bitmap.CompressFormat.JPEG, 80, output)) { "Image encoding failed" }
    output.toByteArray()
}
