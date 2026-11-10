package com.akitorahayashi.catboard.catapi

import io.ktor.client.engine.okhttp.OkHttp

fun createClient(): Client = Client(OkHttp.create())
