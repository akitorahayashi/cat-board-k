@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package com.akitorahayashi.catboard.imageloader

import coil3.PlatformContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout

@kotlin.native.HiddenFromObjC
fun createLoader(cacheDirectory: String): CoilLoader = createLoader(
    PlatformContext.INSTANCE,
    cacheDirectory,
    HttpClient(Darwin) {
        install(HttpTimeout) { requestTimeoutMillis = 10_000 }
    },
)
