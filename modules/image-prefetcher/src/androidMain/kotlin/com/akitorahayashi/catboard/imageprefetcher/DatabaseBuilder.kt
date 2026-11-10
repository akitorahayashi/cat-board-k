package com.akitorahayashi.catboard.imageprefetcher

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun createDatabase(context: Context, path: String): PrefetchDatabase {
    require(path.startsWith('/')) { "The prefetch database needs an absolute path" }
    return Room.databaseBuilder<PrefetchDatabase>(context.applicationContext, path)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
