package com.akitorahayashi.catboard.imagestore

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun createDatabase(path: String): UrlDatabase {
    require(path.startsWith('/')) { "The URL database needs an absolute path" }
    return Room.databaseBuilder<UrlDatabase>(name = path)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
