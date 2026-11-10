package com.akitorahayashi.catboard.imageprefetcher

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun createDatabase(path: String): PrefetchDatabase {
    require(path.startsWith('/')) { "The prefetch database needs an absolute path" }
    return Room.databaseBuilder<PrefetchDatabase>(name = path)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
