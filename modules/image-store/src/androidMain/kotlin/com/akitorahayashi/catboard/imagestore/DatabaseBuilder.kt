package com.akitorahayashi.catboard.imagestore

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun createDatabase(context: Context, path: String): UrlDatabase {
    require(path.startsWith('/')) { "The URL database needs an absolute path" }
    return Room.databaseBuilder<UrlDatabase>(context.applicationContext, path)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
