package com.akitorahayashi.catboard.imageloader

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

fun createLoader(context: Context, cacheDirectory: String): CoilLoader = createLoader(
    context.applicationContext,
    cacheDirectory,
    HttpClient(OkHttp) {
        install(HttpTimeout) { requestTimeoutMillis = 10_000 }
    },
)
