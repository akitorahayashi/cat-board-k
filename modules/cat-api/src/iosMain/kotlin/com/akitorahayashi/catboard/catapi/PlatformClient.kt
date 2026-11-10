package com.akitorahayashi.catboard.catapi

import io.ktor.client.engine.darwin.Darwin

fun createClient(): Client = Client(Darwin.create())
